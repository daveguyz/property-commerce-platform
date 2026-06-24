package com.staysphere.aiservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "conversation_history")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConversationHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String guestId;
    private String sessionId;
    @Column(columnDefinition = "TEXT") private String userMessage;
    @Column(columnDefinition = "TEXT") private String aiResponse;
    @Column(columnDefinition = "TEXT") private String extractedIntent;
    @Column(columnDefinition = "TEXT") private String returnedPropertyIds;
    @CreationTimestamp private LocalDateTime createdAt;
}
