package com.staysphere.auctionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staysphere.auctionservice.model.Bid;
import com.staysphere.auctionservice.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class AiFraudService {

    private final BidRepository bidRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api.key:}") private String anthropicApiKey;
    private static final String CLAUDE_URL   = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL = "claude-sonnet-4-6";

    /**
     * Assess a bid for fraud signals. Returns a FraudAssessment with:
     * - score 0.0 = clean, 1.0 = highly suspicious
     * - reasoning: one-sentence explanation
     * Called inline during bid placement; must be fast (async-safe to skip on timeout).
     */
    public FraudAssessment assessBid(Bid bid, String lotId, int totalLotBids) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return new FraudAssessment(0.0, "AI fraud detection not configured", false);
        }

        // Quick rule-based pre-filter (no API call needed)
        double rulesScore = applyHeuristicRules(bid, lotId);
        if (rulesScore > 0.9) {
            return new FraudAssessment(rulesScore, "Heuristic rules triggered: high-velocity bidding pattern", true);
        }

        // Only call Claude for bids that pass heuristics but need deeper analysis
        if (rulesScore < 0.2 && totalLotBids < 50) {
            return new FraudAssessment(rulesScore, "Heuristic assessment: low risk", false);
        }

        try {
            return callClaude(bid, rulesScore, lotId, totalLotBids);
        } catch (Exception e) {
            log.warn("[AiFraud] Claude API unavailable (non-blocking): {}", e.getMessage());
            return new FraudAssessment(rulesScore, "Rule-based assessment (AI unavailable)", rulesScore > 0.5);
        }
    }

    /** Assess a newly KYC-verified user for fraud signals in their verification. */
    public FraudAssessment assessKycVerification(String userId, String stripeSessionId) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return new FraudAssessment(0.0, "AI not configured", false);
        }

        List<Bid> recentBids = bidRepository.findRecentBidsByBidder(userId,
                LocalDateTime.now().minusDays(30));

        String prompt = buildKycPrompt(userId, stripeSessionId, recentBids.size());
        return callClaudeForKyc(prompt);
    }

    // ─── Heuristic rules (fast, no API call) ──────────────────────────────────

    private double applyHeuristicRules(Bid bid, String lotId) {
        double score = 0.0;

        // Rule 1: Bid in last 2 seconds of lot (extreme sniping — acceptable, but flag)
        if (bid.getMsRemainingAtBid() != null && bid.getMsRemainingAtBid() < 2000) score += 0.15;

        // Rule 2: Bidder placed > 10 bids in last 60 seconds (velocity attack)
        long recentBidCount = bidRepository.findRecentBidsByBidder(
                bid.getBidderId(), LocalDateTime.now().minusSeconds(60)).size();
        if (recentBidCount > 10) score += 0.5;
        else if (recentBidCount > 5) score += 0.2;

        // Rule 3: Proxy bid with ceiling exactly at the lot's reserve price
        // (suggests insider knowledge of reserve — only flag if very close)
        // We can't check reserve here without lot context, so skip.

        // Rule 4: "PROXY_SYSTEM" IP means it's an auto-proxy bid — always clean
        if ("PROXY_SYSTEM".equals(bid.getIpAddress())) return 0.0;

        // Rule 5: No device fingerprint is mildly suspicious
        if (bid.getDeviceFingerprint() == null || bid.getDeviceFingerprint().isBlank()) score += 0.1;

        return Math.min(score, 1.0);
    }

    // ─── Claude API calls ─────────────────────────────────────────────────────

    private FraudAssessment callClaude(Bid bid, double rulesScore, String lotId, int totalBids) {
        String systemPrompt = """
                You are an auction fraud detection AI for StaySphere, a Namibian property auction platform.
                Analyse the provided bid metadata and return ONLY a JSON object with:
                - "score": float 0.0-1.0 (0=clean, 1=highly suspicious)
                - "reasoning": one sentence explaining the score
                - "flag_for_review": boolean
                Respond with JSON only. No explanation outside the JSON.
                """;

        String userPrompt = String.format("""
                Bid analysis:
                - Lot ID: %s
                - Bid amount: %s NAD
                - Proxy ceiling: %s
                - Bidder ID: %s
                - IP address: %s
                - Device fingerprint present: %s
                - ms remaining when bid placed: %s
                - Total bids on lot so far: %d
                - Heuristic pre-score: %.3f
                - Bid sequence: %d
                """,
                lotId,
                bid.getAmount(),
                bid.getProxyCeiling() != null ? bid.getProxyCeiling() + " NAD" : "none",
                bid.getBidderId(),
                bid.getIpAddress() != null ? "present" : "missing",
                bid.getDeviceFingerprint() != null,
                bid.getMsRemainingAtBid(),
                totalBids,
                rulesScore,
                bid.getBidSequence()
        );

        String responseText = invokeClaudeApi(systemPrompt, userPrompt, 200);
        return parseAssessment(responseText, rulesScore);
    }

    private FraudAssessment callClaudeForKyc(String prompt) {
        String system = """
                You are a fraud detection AI for an auction KYC process.
                Analyse the user data and return ONLY JSON:
                {"score": 0.0-1.0, "reasoning": "one sentence", "flag_for_review": false}
                """;
        String responseText = invokeClaudeApi(system, prompt, 150);
        return parseAssessment(responseText, 0.0);
    }

    private String buildKycPrompt(String userId, String sessionId, int recentBidCount) {
        return String.format(
                "KYC review: userId=%s, stripeSession=%s, bids in last 30 days=%d. " +
                "Assess risk based on bid activity pattern.", userId, sessionId, recentBidCount);
    }

    @SuppressWarnings("unchecked")
    private String invokeClaudeApi(String systemPrompt, String userPrompt, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", CLAUDE_MODEL);
        body.put("max_tokens", maxTokens);
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        ResponseEntity<Map> response = restTemplate.exchange(
                CLAUDE_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        return content != null && !content.isEmpty() ? (String) content.get(0).get("text") : "{}";
    }

    @SuppressWarnings("unchecked")
    private FraudAssessment parseAssessment(String responseText, double fallbackScore) {
        try {
            // Strip any markdown fences
            String clean = responseText.replaceAll("```json|```", "").trim();
            Map<String, Object> result = objectMapper.readValue(clean, Map.class);
            double score = result.containsKey("score")
                    ? ((Number) result.get("score")).doubleValue() : fallbackScore;
            String reasoning = (String) result.getOrDefault("reasoning", "AI assessment");
            boolean flag = Boolean.TRUE.equals(result.get("flag_for_review")) || score > 0.6;
            return new FraudAssessment(score, reasoning, flag);
        } catch (Exception e) {
            log.warn("[AiFraud] Failed to parse Claude response: {}", e.getMessage());
            return new FraudAssessment(fallbackScore, "AI parse error — rule-based score used", fallbackScore > 0.5);
        }
    }

    /** Immutable result record. */
    public record FraudAssessment(double score, String reasoning, boolean flagForReview) {
        public BigDecimal scoreAsBigDecimal() { return BigDecimal.valueOf(score); }
    }
}
