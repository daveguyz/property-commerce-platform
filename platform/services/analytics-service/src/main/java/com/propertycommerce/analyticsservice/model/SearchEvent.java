package com.propertycommerce.analyticsservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "search_events",
    indexes = @Index(name = "idx_se_time", columnList = "searched_at"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    private String sessionId, userId;
    @Column(columnDefinition = "TEXT") private String query;
    private String city, region;
    private Integer resultsCount;
    private Boolean aiAssisted;
    private Integer resultClickedIndex;
    private String resultClickedId;
    @CreationTimestamp private LocalDateTime searchedAt;
}
