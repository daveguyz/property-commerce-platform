package com.staysphere.trustservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.time.*;

@Entity @Table(name = "user_profiles",
    indexes = @Index(name = "idx_profile_user", columnList = "user_id"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false, unique = true) private String userId;
    private String shopifyCustomerId;
    private String firstName, lastName, email, phone;
    private String profileImageUrl, nationality;
    private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING) @Builder.Default private UserRole role = UserRole.GUEST;
    @Builder.Default private Boolean idVerified = false;
    @Builder.Default private Boolean phoneVerified = false;
    @Builder.Default private Boolean emailVerified = false;
    @Builder.Default private Boolean backgroundCheckCompleted = false;
    private String idDocumentType, idDocumentRef;
    @Column(precision = 5, scale = 2) private Double trustScore;
    @Column(precision = 3, scale = 2) private Double averageRating;
    @Builder.Default private Integer totalBookings = 0;
    @Builder.Default private Integer totalReviews = 0;
    @Builder.Default private Integer cancelledBookings = 0;
    private Double responseRate;
    private String stripeCustomerId, stripeAccountId;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public enum UserRole { GUEST, HOST, BOTH, ADMIN }
}
