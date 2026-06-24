package com.staysphere.analyticsservice.repository;
import com.staysphere.analyticsservice.model.PageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface PageViewRepository extends JpaRepository<PageView, String> {
    @Query("SELECT COUNT(DISTINCT p.sessionId) FROM PageView p WHERE p.viewedAt >= :since")
    Long countUniqueSessions(@Param("since") LocalDateTime since);
    @Query("SELECT COUNT(DISTINCT p.userId) FROM PageView p WHERE p.viewedAt >= :since AND p.userId IS NOT NULL")
    Long countUniqueUsers(@Param("since") LocalDateTime since);
    @Query("SELECT p.pageType, COUNT(p) FROM PageView p WHERE p.viewedAt >= :since GROUP BY p.pageType ORDER BY COUNT(p) DESC")
    List<Object[]> getPageViewsByType(@Param("since") LocalDateTime since);
    @Query("SELECT p.country, COUNT(p) FROM PageView p WHERE p.viewedAt >= :since GROUP BY p.country ORDER BY COUNT(p) DESC")
    List<Object[]> getViewsByCountry(@Param("since") LocalDateTime since);
    @Query("SELECT p.deviceType, COUNT(p) FROM PageView p WHERE p.viewedAt >= :since GROUP BY p.deviceType")
    List<Object[]> getViewsByDevice(@Param("since") LocalDateTime since);
    Long countByViewedAtAfter(LocalDateTime since);
}
