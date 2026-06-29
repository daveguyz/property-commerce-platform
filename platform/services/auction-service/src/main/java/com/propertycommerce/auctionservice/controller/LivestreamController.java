package com.propertycommerce.auctionservice.controller;

import com.propertycommerce.auctionservice.service.LivestreamService;
import com.propertycommerce.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor @Slf4j
public class LivestreamController {

    private final LivestreamService livestreamService;

    /** Create a Mux live stream for a lot (seller only). */
    @PostMapping("/{lotId}/stream")
    public ResponseEntity<ApiResponse<LivestreamService.LivestreamDetails>> createStream(
            @PathVariable String lotId,
            @RequestHeader("X-User-Id") String userId) {
        LivestreamService.LivestreamDetails details = livestreamService.createStream(lotId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(details, "Livestream created — use streamKey with OBS or your broadcasting software"));
    }

    /** Get public stream details for viewers (no stream key). */
    @GetMapping("/{lotId}/stream")
    public ResponseEntity<ApiResponse<LivestreamService.LivestreamDetails>> getStream(
            @PathVariable String lotId) {
        return ResponseEntity.ok(ApiResponse.success(livestreamService.getStreamDetails(lotId)));
    }

    /** Register a YouTube stream URL manually. */
    @PostMapping("/{lotId}/stream/youtube")
    public ResponseEntity<ApiResponse<LivestreamService.LivestreamDetails>> setYouTubeStream(
            @PathVariable String lotId,
            @RequestBody YouTubeStreamRequest req,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                livestreamService.setYouTubeStream(lotId, req.youtubeUrl(), req.streamKey())));
    }

    /** Delete a stream after lot is settled (seller cleanup). */
    @DeleteMapping("/{lotId}/stream")
    public ResponseEntity<ApiResponse<Void>> deleteStream(
            @PathVariable String lotId,
            @RequestHeader("X-User-Id") String userId) {
        livestreamService.deleteStream(lotId);
        return ResponseEntity.ok(ApiResponse.success(null, "Stream deleted"));
    }

    /**
     * Mux webhook — receives stream active/idle events.
     * Public endpoint — Mux calls this without auth.
     */
    @PostMapping("/stream/mux-webhook")
    public ResponseEntity<String> muxWebhook(@RequestBody MuxWebhookPayload payload) {
        try {
            String eventType  = payload.type();
            String passthrough = payload.data() != null ? payload.data().passthrough() : null;
            livestreamService.handleMuxWebhook(eventType, passthrough);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("[Mux Webhook] Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }

    record YouTubeStreamRequest(String youtubeUrl, String streamKey) {}

    record MuxWebhookPayload(String type, MuxWebhookData data) {
        record MuxWebhookData(String passthrough, String id) {}
    }
}
