package com.propertycommerce.auctionservice.controller;

import com.propertycommerce.auctionservice.model.LotQuestion;
import com.propertycommerce.auctionservice.service.LotQuestionService;
import com.propertycommerce.shared.dto.ApiResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the lot Q&A mechanism.
 *
 * BIDDER endpoints:
 *   POST /api/v1/auctions/{lotId}/questions            — submit question
 *   GET  /api/v1/auctions/{lotId}/questions/mine       — my questions + public answers
 *   GET  /api/v1/auctions/{lotId}/questions/public     — public answers (no auth required)
 *
 * AUCTIONEER / SELLER endpoints:
 *   GET  /api/v1/auctions/{lotId}/questions/queue      — paginated queue, filterable
 *   GET  /api/v1/auctions/{lotId}/questions/count/pending — badge count
 *   POST /api/v1/auctions/{lotId}/questions/{id}/answer    — answer (private or public)
 *   POST /api/v1/auctions/{lotId}/questions/{id}/dismiss   — dismiss without answer
 *   POST /api/v1/auctions/{lotId}/questions/{id}/escalate  — escalate to support
 *   POST /api/v1/auctions/{lotId}/questions/{id}/flag      — flag as abusive
 *
 * Auth: X-User-Id header injected by the API Gateway JWT filter.
 * Role-based permission checks are enforced in LotQuestionService.
 */
@RestController
@RequestMapping("/api/v1/auctions/{lotId}/questions")
@Slf4j
@RequiredArgsConstructor
public class LotQuestionController {

    private final LotQuestionService questionService;

    // ── Bidder: submit ────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<LotQuestion>> submitQuestion(
            @PathVariable String lotId,
            @RequestBody SubmitQuestionRequest req,
            @RequestHeader("X-User-Id")    String bidderId,
            @RequestHeader("X-User-Email") String bidderEmail) {

        if (req.getContent() == null || req.getContent().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Question content is required"));
        }

        LotQuestion saved = questionService.submitQuestion(
                lotId, bidderId, bidderEmail,
                req.getContent(), req.getCategory());

        return ResponseEntity.ok(ApiResponse.success(saved, "Question submitted"));
    }

    // ── Bidder: read own view ─────────────────────────────────────────────

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<LotQuestionService.BidderQuestionView>> getMyQuestions(
            @PathVariable String lotId,
            @RequestHeader("X-User-Id") String bidderId) {

        return ResponseEntity.ok(ApiResponse.success(
                questionService.getBidderView(lotId, bidderId)));
    }

    // ── Public: anyone in the room can see public answers ─────────────────

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<LotQuestion>>> getPublicAnswers(
            @PathVariable String lotId) {

        return ResponseEntity.ok(ApiResponse.success(
                questionService.getPublicAnswers(lotId)));
    }

    // ── Auctioneer / seller: queue ────────────────────────────────────────

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<Page<LotQuestion>>> getQueue(
            @PathVariable String lotId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String callerId) {

        boolean urgentFirst = "urgent".equalsIgnoreCase(sort);
        Page<LotQuestion> result = questionService.getQueue(
                lotId, callerId, status, urgentFirst,
                PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/count/pending")
    public ResponseEntity<ApiResponse<Long>> countPending(
            @PathVariable String lotId) {

        return ResponseEntity.ok(ApiResponse.success(questionService.countPending(lotId)));
    }

    // ── Auctioneer / seller: lifecycle actions ────────────────────────────

    @PostMapping("/{questionId}/answer")
    public ResponseEntity<ApiResponse<LotQuestion>> answerQuestion(
            @PathVariable String lotId,
            @PathVariable String questionId,
            @RequestBody AnswerQuestionRequest req,
            @RequestHeader("X-User-Id") String callerId) {

        if (req.getResponse() == null || req.getResponse().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Response text is required"));
        }

        LotQuestion answered = questionService.answerQuestion(
                questionId, callerId, req.getResponse(), req.isAnswerPublicly());

        return ResponseEntity.ok(ApiResponse.success(answered,
                req.isAnswerPublicly() ? "Answer broadcast to room" : "Answer sent privately"));
    }

    @PostMapping("/{questionId}/dismiss")
    public ResponseEntity<ApiResponse<LotQuestion>> dismissQuestion(
            @PathVariable String lotId,
            @PathVariable String questionId,
            @RequestHeader("X-User-Id") String callerId) {

        return ResponseEntity.ok(ApiResponse.success(
                questionService.dismissQuestion(questionId, callerId),
                "Question dismissed"));
    }

    @PostMapping("/{questionId}/escalate")
    public ResponseEntity<ApiResponse<LotQuestion>> escalateQuestion(
            @PathVariable String lotId,
            @PathVariable String questionId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("X-User-Id") String callerId) {

        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return ResponseEntity.ok(ApiResponse.success(
                questionService.escalateQuestion(questionId, callerId, reason),
                "Question escalated to platform support"));
    }

    @PostMapping("/{questionId}/flag")
    public ResponseEntity<ApiResponse<LotQuestion>> flagAsAbusive(
            @PathVariable String lotId,
            @PathVariable String questionId,
            @RequestHeader("X-User-Id") String callerId) {

        return ResponseEntity.ok(ApiResponse.success(
                questionService.flagAsAbusive(questionId, callerId),
                "Question flagged"));
    }

    // ── Request DTOs ──────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SubmitQuestionRequest {
        private String content;
        private String category;  // optional — defaults to GENERAL
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AnswerQuestionRequest {
        private String  response;
        private boolean answerPublicly = false;
    }
}
