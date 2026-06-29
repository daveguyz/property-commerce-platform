package com.propertycommerce.auctionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertycommerce.auctionservice.model.AuctionLot;
import com.propertycommerce.auctionservice.repository.AuctionLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Mux livestream integration for auction rooms.
 *
 * Mux API docs: https://docs.mux.com/api-reference/video
 * Each auction lot can have one Mux live stream.
 * The stream key is given to the seller (e.g. for OBS/streaming software).
 * The playback ID is used in the Shopify theme to embed the player.
 *
 * Falls back gracefully to YouTube RTMP if Mux is not configured.
 */
@Service @Slf4j @RequiredArgsConstructor
public class LivestreamService {

    private final AuctionLotRepository lotRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mux.token-id:}")      private String muxTokenId;
    @Value("${mux.token-secret:}")  private String muxTokenSecret;

    private static final String MUX_API  = "https://api.mux.com/video/v1";
    private static final String PROVIDER_MUX     = "MUX";
    private static final String PROVIDER_YOUTUBE = "YOUTUBE";
    private static final String PROVIDER_NONE    = "NONE";

    /**
     * Create a Mux live stream for an auction lot.
     * Returns the stream key (for the seller/broadcaster) and playback URL (for viewers).
     */
    @Transactional
    public LivestreamDetails createStream(String lotId) {
        AuctionLot lot = findLot(lotId);

        if (muxTokenId == null || muxTokenId.isBlank()) {
            log.warn("[Livestream] Mux not configured — skipping stream creation for lot {}", lotId);
            return LivestreamDetails.disabled(lotId);
        }

        try {
            HttpHeaders headers = buildMuxHeaders();
            Map<String, Object> body = Map.of(
                    "playback_policy", List.of("public"),
                    "new_asset_settings", Map.of("playback_policy", List.of("public")),
                    "latency_mode", "low",     // Low-latency for live bidding
                    "test", false,
                    "passthrough", lotId       // correlate stream to lot
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    MUX_API + "/live-streams",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            String streamId       = (String) data.get("id");
            String streamKey      = (String) data.get("stream_key");
            List<Map<String, Object>> playbackIds = (List<Map<String, Object>>) data.get("playback_ids");
            String playbackId     = playbackIds != null && !playbackIds.isEmpty()
                    ? (String) playbackIds.get(0).get("id") : null;
            String playbackUrl    = playbackId != null
                    ? "https://stream.mux.com/" + playbackId + ".m3u8" : null;

            // Persist on the lot
            lot.setLivestreamProvider(PROVIDER_MUX);
            lot.setLivestreamKey(streamKey);
            lot.setLivestreamPlaybackId(playbackId);
            lot.setLivestreamUrl(playbackUrl);
            lot.setLivestreamActive(false); // becomes true when seller starts streaming
            lotRepository.save(lot);

            log.info("[Livestream] Mux stream created for lot {}: streamId={}", lotId, streamId);
            return new LivestreamDetails(lotId, PROVIDER_MUX, streamKey, playbackId, playbackUrl, false, streamId);

        } catch (Exception e) {
            log.error("[Livestream] Mux stream creation failed for lot {}: {}", lotId, e.getMessage());
            throw new IllegalStateException("Failed to create Mux livestream: " + e.getMessage(), e);
        }
    }

    /**
     * Handle Mux webhook — update stream active status when seller goes live/offline.
     * Mux sends: video.live_stream.active, video.live_stream.idle, video.live_stream.disabled
     */
    @Transactional
    public void handleMuxWebhook(String eventType, String passthrough) {
        if (passthrough == null || passthrough.isBlank()) return;
        String lotId = passthrough; // we stored lotId as passthrough

        lotRepository.findById(lotId).ifPresent(lot -> {
            switch (eventType) {
                case "video.live_stream.active"  -> {
                    lot.setLivestreamActive(true);
                    log.info("[Livestream] Stream ACTIVE for lot {}", lotId);
                }
                case "video.live_stream.idle",
                     "video.live_stream.disabled" -> {
                    lot.setLivestreamActive(false);
                    log.info("[Livestream] Stream IDLE/DISABLED for lot {}", lotId);
                }
                default -> log.debug("[Livestream] Unhandled Mux event: {}", eventType);
            }
            lotRepository.save(lot);
        });
    }

    /** Delete the Mux stream after the lot is settled (cleanup). */
    @Transactional
    public void deleteStream(String lotId) {
        AuctionLot lot = findLot(lotId);
        if (!PROVIDER_MUX.equals(lot.getLivestreamProvider())
                || lot.getLivestreamKey() == null) return;
        if (muxTokenId == null || muxTokenId.isBlank()) return;

        try {
            // We need the stream ID — look up by stream key via Mux API
            HttpHeaders headers = buildMuxHeaders();
            ResponseEntity<Map> list = restTemplate.exchange(
                    MUX_API + "/live-streams", HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class);
            List<Map<String, Object>> streams = (List<Map<String, Object>>) list.getBody().get("data");
            streams.stream()
                    .filter(s -> lot.getLivestreamKey().equals(s.get("stream_key")))
                    .findFirst()
                    .ifPresent(s -> {
                        String streamId = (String) s.get("id");
                        restTemplate.exchange(MUX_API + "/live-streams/" + streamId,
                                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
                        log.info("[Livestream] Deleted Mux stream {} for lot {}", streamId, lotId);
                    });

            lot.setLivestreamActive(false);
            lot.setLivestreamKey(null);
            lotRepository.save(lot);
        } catch (Exception e) {
            log.warn("[Livestream] Could not delete stream for lot {}: {}", lotId, e.getMessage());
        }
    }

    /** Register a YouTube RTMP URL as the stream source for a lot (manual flow). */
    @Transactional
    public LivestreamDetails setYouTubeStream(String lotId, String youtubeStreamUrl, String streamKey) {
        AuctionLot lot = findLot(lotId);
        lot.setLivestreamProvider(PROVIDER_YOUTUBE);
        lot.setLivestreamKey(streamKey);
        lot.setLivestreamUrl(youtubeStreamUrl);
        lot.setLivestreamActive(false);
        lotRepository.save(lot);
        log.info("[Livestream] YouTube stream set for lot {}", lotId);
        return new LivestreamDetails(lotId, PROVIDER_YOUTUBE, streamKey, null, youtubeStreamUrl, false, null);
    }

    /** Get current stream details for a lot (for the viewer embed). */
    public LivestreamDetails getStreamDetails(String lotId) {
        AuctionLot lot = findLot(lotId);
        if (lot.getLivestreamProvider() == null || PROVIDER_NONE.equals(lot.getLivestreamProvider())) {
            return LivestreamDetails.disabled(lotId);
        }
        return new LivestreamDetails(
                lotId,
                lot.getLivestreamProvider(),
                null, // never expose stream key to viewers
                lot.getLivestreamPlaybackId(),
                lot.getLivestreamUrl(),
                Boolean.TRUE.equals(lot.getLivestreamActive()),
                null
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private HttpHeaders buildMuxHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = muxTokenId + ":" + muxTokenSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private AuctionLot findLot(String lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lotId));
    }

    /** Immutable stream detail record returned to callers. */
    public record LivestreamDetails(
            String lotId,
            String provider,
            String streamKey,       // only returned to the seller, never to viewers
            String muxPlaybackId,
            String streamUrl,       // HLS URL (Mux) or YouTube embed URL
            boolean isLive,
            String muxStreamId
    ) {
        static LivestreamDetails disabled(String lotId) {
            return new LivestreamDetails(lotId, "NONE", null, null, null, false, null);
        }
    }
}
