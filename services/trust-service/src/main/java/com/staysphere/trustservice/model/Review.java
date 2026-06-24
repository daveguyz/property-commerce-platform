package com.staysphere.trustservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "reviews",
    indexes = { @Index(name = "idx_review_property", columnList = "property_id"),
                @Index(name = "idx_review_guest", columnList = "guest_id") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String bookingId;
    @Column(nullable = false) private String propertyId;
    @Column(nullable = false) private String guestId;
    @Column(nullable = false) private String hostId;
    private Integer overallRating, cleanlinessRating, accuracyRating;
    private Integer checkInRating, communicationRating, locationRating, valueRating;
    @Column(columnDefinition = "TEXT", nullable = false) private String comment;
    @Column(columnDefinition = "TEXT") private String hostResponse;
    private LocalDateTime hostResponseAt;
    @CreationTimestamp private LocalDateTime createdAt;
}
