package com.staysphere.propertyservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "availability_blocks",
       indexes = @Index(name = "idx_avail_property_dates", columnList = "property_id, start_date, end_date"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AvailabilityBlock {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(name = "property_id", nullable = false) private String propertyId;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Enumerated(EnumType.STRING) private BlockType blockType;
    private String bookingId, reason;
    @CreationTimestamp private LocalDateTime createdAt;
    public enum BlockType { BOOKING, OWNER_BLOCK, MAINTENANCE }
}
