package com.propertycommerce.messagingservice.service;
import com.propertycommerce.messagingservice.model.*;
import com.propertycommerce.messagingservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor @Transactional(readOnly = true)
public class MessagingService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate websocket;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public Message sendMessage(String conversationId, String senderId, String content,
            Message.MessageType type, List<String> attachmentUrls) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

        // Validate sender is participant
        if (!conversation.getParticipantOneId().equals(senderId)
                && !conversation.getParticipantTwoId().equals(senderId))
            throw new SecurityException("You are not a participant in this conversation");

        Message message = Message.builder()
                .conversationId(conversationId).senderId(senderId)
                .content(content).type(type).attachmentUrls(attachmentUrls).build();
        Message saved = messageRepository.save(message);

        // Update conversation metadata
        String preview = content.length() > 80 ? content.substring(0, 77) + "…" : content;
        conversation.setLastMessagePreview(preview);
        conversation.setLastMessageAt(LocalDateTime.now());

        // Increment unread for recipient
        String recipientId = conversation.getParticipantOneId().equals(senderId)
                ? conversation.getParticipantTwoId() : conversation.getParticipantOneId();
        if (conversation.getParticipantOneId().equals(recipientId))
            conversation.setUnreadCountOne((conversation.getUnreadCountOne() != null
                    ? conversation.getUnreadCountOne() : 0) + 1);
        else
            conversation.setUnreadCountTwo((conversation.getUnreadCountTwo() != null
                    ? conversation.getUnreadCountTwo() : 0) + 1);
        conversationRepository.save(conversation);

        // WebSocket push to recipient
        websocket.convertAndSendToUser(recipientId, "/queue/messages",
                Map.of("conversationId", conversationId, "message", saved,
                       "senderId", senderId, "timestamp", saved.getSentAt()));

        // Kafka event for notification service
        kafkaTemplate.send("messaging.message.sent",
                Map.of("messageId", saved.getId(), "conversationId", conversationId,
                       "senderId", senderId, "recipientId", recipientId,
                       "preview", preview));

        log.info("Message sent in conversation {} by {}", conversationId, senderId);
        return saved;
    }

    @Transactional
    public Conversation getOrCreateConversation(String user1Id, String user2Id,
            String bookingId, String propertyId, Conversation.ConversationType type) {
        if (bookingId != null) {
            Optional<Conversation> existing = conversationRepository.findByBookingId(bookingId);
            if (existing.isPresent()) return existing.get();
        } else {
            Optional<Conversation> existing = conversationRepository.findDirectConversation(user1Id, user2Id);
            if (existing.isPresent()) return existing.get();
        }
        Conversation conversation = Conversation.builder()
                .participantOneId(user1Id).participantTwoId(user2Id)
                .bookingId(bookingId).propertyId(propertyId).type(type)
                .unreadCountOne(0).unreadCountTwo(0).build();
        return conversationRepository.save(conversation);
    }

    @Transactional
    public void markAsRead(String conversationId, String userId) {
        int updated = messageRepository.markAllAsRead(conversationId, userId);
        Conversation conv = conversationRepository.findById(conversationId).orElseThrow();
        if (conv.getParticipantOneId().equals(userId)) conv.setUnreadCountOne(0);
        else conv.setUnreadCountTwo(0);
        conversationRepository.save(conv);
    }

    public Page<Conversation> getUserConversations(String userId, Pageable pageable) {
        return conversationRepository.findByParticipant(userId, pageable);
    }

    public Page<Message> getMessages(String conversationId, String requesterId, Pageable pageable) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!conv.getParticipantOneId().equals(requesterId) && !conv.getParticipantTwoId().equals(requesterId))
            throw new SecurityException("Access denied");
        return messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable);
    }

    public Integer getTotalUnreadCount(String userId) {
        Integer count = conversationRepository.getTotalUnreadCount(userId);
        return count != null ? count : 0;
    }
}
