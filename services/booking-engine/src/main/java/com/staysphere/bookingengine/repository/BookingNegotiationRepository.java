package com.staysphere.bookingengine.repository;
import com.staysphere.bookingengine.model.BookingNegotiation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingNegotiationRepository extends JpaRepository<BookingNegotiation, String> {
    List<BookingNegotiation> findByPropertyIdOrderByCreatedAtDesc(String propertyId);
    List<BookingNegotiation> findByGuestIdOrderByCreatedAtDesc(String guestId);
    List<BookingNegotiation> findByHostIdAndStatusOrderByCreatedAtDesc(
            String hostId, BookingNegotiation.NegotiationStatus status);
}
