package com.staysphere.messagingservice.controller;
import com.staysphere.messagingservice.model.*;
import com.staysphere.messagingservice.service.*;
import com.staysphere.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/messages") @RequiredArgsConstructor
public class MessagingController {
    private final MessagingService messagingService;
    private final SupportTicketService supportTicketService;

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Conversation>>> getConversations(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                messagingService.getUserConversations(userId, PageRequest.of(page, size))));
    }

    @PostMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Conversation>> createConversation(
            @RequestBody Map<String, String> req, @AuthenticationPrincipal String userId) {
        Conversation conv = messagingService.getOrCreateConversation(
                userId, req.get("recipientId"), req.get("bookingId"),
                req.get("propertyId"), Conversation.ConversationType.GUEST_HOST);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(conv));
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Message>>> getMessages(
            @PathVariable String id, @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<Message> messages = messagingService.getMessages(id, userId, PageRequest.of(page, size, Sort.by("sentAt").ascending()));
        messagingService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/conversations/{id}/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @PathVariable String id, @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal String userId) {
        Message sent = messagingService.sendMessage(id, userId,
                (String) req.get("content"),
                req.get("type") != null ? Message.MessageType.valueOf((String) req.get("type"))
                        : Message.MessageType.TEXT,
                (List<String>) req.get("attachmentUrls"));
        return ResponseEntity.ok(ApiResponse.success(sent));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(messagingService.getTotalUnreadCount(userId)));
    }

    @PostMapping("/support/tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SupportTicket>> createTicket(
            @RequestBody Map<String, Object> req, @AuthenticationPrincipal String userId) {
        SupportTicket ticket = supportTicketService.createTicket(
                userId, (String) req.get("subject"), (String) req.get("description"),
                req.get("category") != null ? SupportTicket.TicketCategory.valueOf((String) req.get("category")) : null,
                req.get("priority") != null ? SupportTicket.TicketPriority.valueOf((String) req.get("priority")) : null,
                (String) req.get("bookingId"), (String) req.get("propertyId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(ticket, "Support ticket created"));
    }

    @GetMapping("/support/tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<SupportTicket>>> getUserTickets(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                supportTicketService.getUserTickets(userId, PageRequest.of(page, size))));
    }

    @GetMapping("/support/tickets/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Page<SupportTicket>>> getOpenTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                supportTicketService.getOpenTickets(PageRequest.of(page, size))));
    }

    @PutMapping("/support/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<SupportTicket>> updateTicketStatus(
            @PathVariable String id, @RequestBody Map<String, String> req,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(ApiResponse.success(
                supportTicketService.updateTicketStatus(id, adminId,
                        SupportTicket.TicketStatus.valueOf(req.get("status")),
                        req.get("resolutionNotes"))));
    }

    @GetMapping("/support/tickets/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Map<String,Long>>> getTicketStats() {
        return ResponseEntity.ok(ApiResponse.success(supportTicketService.getTicketStats()));
    }
}
