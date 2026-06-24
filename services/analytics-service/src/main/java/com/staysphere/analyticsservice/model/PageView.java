package com.staysphere.analyticsservice.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name = "page_views",
    indexes = { @Index(name = "idx_pv_session", columnList = "session_id"),
                @Index(name = "idx_pv_time", columnList = "viewed_at"),
                @Index(name = "idx_pv_page", columnList = "page_type") })
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PageView {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    private String sessionId, userId, ipAddress;
    private String pageType, pageId, pageTitle;
    private String referrer, userAgent, country, city;
    private String deviceType, browser, os;
    private Integer timeOnPageSeconds;
    @CreationTimestamp private LocalDateTime viewedAt;
}
