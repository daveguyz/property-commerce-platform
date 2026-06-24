package com.staysphere.paymentservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.time.LocalDateTime;

@Entity @Table(name = "host_payout_accounts",
    indexes = @Index(name = "idx_payout_host", columnList = "host_id"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HostPayoutAccount {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false, unique = true) private String hostId;
    private String stripeAccountId, stripeAccountType;
    private Boolean detailsSubmitted, chargesEnabled, payoutsEnabled;
    private String country, currency, email;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
