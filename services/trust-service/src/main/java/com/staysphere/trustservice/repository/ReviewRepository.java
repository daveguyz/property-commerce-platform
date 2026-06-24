package com.staysphere.trustservice.repository;
import com.staysphere.trustservice.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    Page<Review> findByPropertyIdOrderByCreatedAtDesc(String propertyId, Pageable pageable);
    List<Review> findByGuestIdOrderByCreatedAtDesc(String guestId);
    boolean existsByBookingIdAndGuestId(String bookingId, String guestId);

    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.propertyId=:propertyId")
    Double getAverageRatingForProperty(@Param("propertyId") String propertyId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.propertyId=:propertyId")
    Integer getReviewCountForProperty(@Param("propertyId") String propertyId);

    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.guestId=:guestId")
    Double getAverageRatingForGuest(@Param("guestId") String guestId);
}
