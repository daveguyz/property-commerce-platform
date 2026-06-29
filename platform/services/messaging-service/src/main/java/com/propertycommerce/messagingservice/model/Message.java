package com.propertycommerce.messagingservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "messages",
    indexes = { @Index(name = "idx_msg_convo", columnList = "conversation_id"),
                @Index(name = "idx_msg_sender", columnList = "sender_id"),
                @Index(name = "idx_msg_time", columnList = "sent_at") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String conversationId;
    @Column(nullable = false) private String senderId;
    @Column(columnDefinition = "TEXT", nullable = false) private String content;
    @Enumerated(EnumType.STRING) @Builder.Default private MessageType type = MessageType.TEXT;
    @ElementCollection
    @CollectionTable(name = "message_attachments", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "attachment_url")
    private List<String> attachmentUrls;
    @Builder.Default private Boolean read = false;
    private LocalDateTime readAt;
    @Builder.Default private Boolean deletedBySender = false;
    @Builder.Default private Boolean deletedByRecipient = false;
    @Column(columnDefinition = "TEXT") private String metadata;
    @CreationTimestamp private LocalDateTime sentAt;

    public enum MessageType { TEXT, IMAGE, DOCUMENT, SYSTEM, BOOKING_LINK, AI_SUGGESTION }
}
