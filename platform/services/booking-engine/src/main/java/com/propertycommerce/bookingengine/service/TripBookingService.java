package com.propertycommerce.bookingengine.service;
import com.propertycommerce.bookingengine.exception.BookingNotFoundException;
import com.propertycommerce.bookingengine.model.*;
import com.propertycommerce.bookingengine.repository.*;
import com.propertycommerce.shared.dto.BookingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class TripBookingService {
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Transactional
    public TripBooking createTrip(String guestId, String tripName, List<BookingDTO> bookingRequests) {
        List<String> bookingIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (BookingDTO request : bookingRequests) {
            // Validate no date conflicts between legs of the trip
            for (String existingId : bookingIds) {
                Booking existing = bookingRepository.findById(existingId).orElseThrow();
                if (datesOverlap(request.getCheckIn(), request.getCheckOut(),
                        existing.getCheckIn(), existing.getCheckOut())) {
                    throw new IllegalArgumentException("Trip legs have overlapping dates");
                }
            }
            BookingDTO created = bookingService.createBooking(request, guestId);
            bookingIds.add(created.getId());
            if (created.getTotalAmount() != null) totalAmount = totalAmount.add(created.getTotalAmount());
        }

        return TripBooking.builder().guestId(guestId).tripName(tripName)
                .bookingIds(bookingIds).totalAmount(totalAmount)
                .status(TripBooking.TripStatus.DRAFT).build();
    }

    private boolean datesOverlap(java.time.LocalDate s1, java.time.LocalDate e1,
                                   java.time.LocalDate s2, java.time.LocalDate e2) {
        return s1.isBefore(e2) && e1.isAfter(s2);
    }
}
