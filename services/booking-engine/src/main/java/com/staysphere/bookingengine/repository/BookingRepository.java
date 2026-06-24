package com.staysphere.bookingengine.repository;
import com.staysphere.bookingengine.model.Booking;
import com.staysphere.bookingengine.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    Page<Booking> findByGuestIdOrderByCreatedAtDesc(String guestId, Pageable pageable);
    Page<Booking> findByHostIdOrderByCreatedAtDesc(String hostId, Pageable pageable);
    List<Booking> findByPropertyIdAndStatusIn(String propertyId, List<BookingStatus> statuses);

    @Query("SELECT COUNT(b)>0 FROM Booking b WHERE b.propertyId=:propertyId AND b.status NOT IN ('CANCELLED','REFUNDED') AND b.checkIn<:checkOut AND b.checkOut>:checkIn")
    boolean existsConflictingBooking(@Param("propertyId") String propertyId,
                                     @Param("checkIn") LocalDate checkIn,
                                     @Param("checkOut") LocalDate checkOut);

    Optional<Booking> findByPaymentIntentId(String paymentIntentId);

    @Query("SELECT b FROM Booking b WHERE b.propertyId=:propertyId AND b.status='CONFIRMED' AND b.checkIn BETWEEN :from AND :to")
    List<Booking> findConfirmedBookingsInRange(@Param("propertyId") String propertyId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    @Query("SELECT AVG(b.totalAmount) FROM Booking b WHERE b.propertyId=:propertyId AND b.status='CONFIRMED' AND b.createdAt >= CURRENT_DATE - 30 DAY")
    Double getAverageRevenueLastMonth(@Param("propertyId") String propertyId);

    long countByGuestIdAndStatus(String guestId, BookingStatus status);
    long countByHostIdAndStatus(String hostId, BookingStatus status);
}
