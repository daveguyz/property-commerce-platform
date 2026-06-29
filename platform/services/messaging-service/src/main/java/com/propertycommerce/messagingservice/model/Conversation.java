package com.propertycommerce.messagingservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.time.*;
import java.util.*;

@Entity @Table(name = "conversations",
    indexes = { @Index(name = "idx_conv_booking", columnList = "booking_id"),
                @Index(name = "idx_conv_participants", columnList = "participant_one_id, participant_two_id") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    private String bookingId, propertyId;
    @Column(nullable = false) private String participantOneId;
    @Column(nullable = false) private String participantTwoId;
    @Enumerated(EnumType.STRING) private ConversationType type;
    @Column(columnDefinition = "TEXT") private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Integer unreadCountOne, unreadCountTwo;
    @Builder.Default private Boolean archivedByOne = false;
    @Builder.Default private Boolean archivedByTwo = false;
    @CreationTimestamp private LocalDateTime createdAt;

    public enum ConversationType { GUEST_HOST, GUEST_ADMIN, HOST_ADMIN, SUPPORT }
}
