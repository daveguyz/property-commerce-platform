package com.propertycommerce.bookingengine.service;
import com.propertycommerce.bookingengine.client.PropertyServiceClient;
import com.propertycommerce.bookingengine.exception.BookingNotFoundException;
import com.propertycommerce.bookingengine.exception.PropertyNotAvailableException;
import com.propertycommerce.bookingengine.exception.UnauthorizedException;
import com.propertycommerce.bookingengine.model.Booking;
import com.propertycommerce.bookingengine.model.BookingStatus;
import com.propertycommerce.bookingengine.repository.BookingRepository;
import com.propertycommerce.shared.dto.*;
import com.propertycommerce.shared.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PropertyServiceClient propertyServiceClient;
    private final PricingCalculatorService pricingCalculator;
    private final AccessCodeService accessCodeService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingDTO createBooking(BookingDTO request, String guestId) {
        // 1. Fetch property
        ApiResponse<PropertyDTO> propertyResponse = propertyServiceClient.getProperty(request.getPropertyId());
        if (!propertyResponse.isSuccess() || propertyResponse.getData() == null)
            throw new PropertyNotAvailableException("Property not found or unavailable");

        PropertyDTO property = propertyResponse.getData();

        // 2. Check availability (acquire Redis lock)
        String lockKey = buildLockKey(request.getPropertyId(), request.getCheckIn(), request.getCheckOut());
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, guestId, Duration.ofMinutes(15));
        if (!Boolean.TRUE.equals(locked)) throw new PropertyNotAvailableException("Property is not available for selected dates");

        // 3. Double-check in DB
        if (bookingRepository.existsConflictingBooking(request.getPropertyId(), request.getCheckIn(), request.getCheckOut())) {
            redisTemplate.delete(lockKey);
            throw new PropertyNotAvailableException("Property is not available for selected dates");
        }

        try {
            // 4. Calculate pricing
            PricingCalculatorService.PricingResult pricing = pricingCalculator.calculate(property, request.getCheckIn(), request.getCheckOut());

            // 5. Create booking
            Booking booking = Booking.builder()
                    .propertyId(request.getPropertyId()).guestId(guestId).hostId(property.getHostId())
                    .checkIn(request.getCheckIn()).checkOut(request.getCheckOut())
                    .guestCount(request.getGuestCount()).childrenCount(request.getChildrenCount())
                    .infantCount(request.getInfantCount()).petCount(request.getPetCount())
                    .baseAmount(pricing.baseAmount()).cleaningFee(pricing.cleaningFee())
                    .serviceFee(pricing.serviceFee()).taxes(pricing.taxes()).totalAmount(pricing.total())
                    .hostPayout(pricing.hostPayout()).platformFee(pricing.platformFee())
                    .currency(pricing.currency()).specialRequests(request.getSpecialRequests())
                    .status(BookingStatus.PENDING_PAYMENT).build();

            Booking saved = bookingRepository.save(booking);

            // 6. Publish event
            kafkaTemplate.send(BookingCreatedEvent.TOPIC, BookingCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString()).bookingId(saved.getId())
                    .propertyId(saved.getPropertyId()).guestId(saved.getGuestId())
                    .hostId(saved.getHostId()).checkIn(saved.getCheckIn()).checkOut(saved.getCheckOut())
                    .guestCount(saved.getGuestCount()).totalAmount(saved.getTotalAmount())
                    .platformFee(saved.getPlatformFee()).hostPayout(saved.getHostPayout())
                    .currency(saved.getCurrency()).propertyName(property.getTitle())
                    .propertyAddress(property.getLocation().getCity())
                    .occurredAt(LocalDateTime.now()).build());

            log.info("Booking {} created for guest {} at property {}", saved.getId(), guestId, request.getPropertyId());
            return toDTO(saved);

        } catch (Exception e) {
            redisTemplate.delete(lockKey);
            throw e;
        }
    }

    @Transactional
    public BookingDTO confirmBooking(String bookingId, String paymentIntentId) {
        Booking booking = getBookingEntity(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT)
            throw new IllegalStateException("Booking is not in PENDING_PAYMENT state");

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentIntentId(paymentIntentId);
        booking.setAccessCode(accessCodeService.generateAccessCode());
        booking.setConfirmedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Block dates in property service
        try {
            propertyServiceClient.blockDates(booking.getPropertyId(),
                    Map.of("startDate", booking.getCheckIn(), "endDate", booking.getCheckOut(),
                            "blockType", "BOOKING", "bookingId", bookingId));
        } catch (Exception e) { log.error("Failed to block dates: {}", e.getMessage()); }

        // Release Redis lock
        redisTemplate.delete(buildLockKey(booking.getPropertyId(), booking.getCheckIn(), booking.getCheckOut()));

        kafkaTemplate.send(BookingConfirmedEvent.TOPIC, BookingConfirmedEvent.builder()
                .eventId(UUID.randomUUID().toString()).bookingId(bookingId)
                .propertyId(booking.getPropertyId()).guestId(booking.getGuestId())
                .hostId(booking.getHostId()).accessCode(saved.getAccessCode())
                .paymentIntentId(paymentIntentId).occurredAt(LocalDateTime.now()).build());

        return toDTO(saved);
    }

    @Transactional
    public BookingDTO cancelBooking(String bookingId, String requesterId, String reason) {
        Booking booking = getBookingEntity(bookingId);
        if (!booking.getGuestId().equals(requesterId) && !booking.getHostId().equals(requesterId))
            throw new UnauthorizedException("You are not authorized to cancel this booking");
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.CHECKED_OUT)
            throw new IllegalStateException("Cannot cancel booking in status: " + booking.getStatus());

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledBy(requesterId);
        booking.setCancelledAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Unblock dates
        try { propertyServiceClient.unblockDates(booking.getPropertyId(), bookingId); }
        catch (Exception e) { log.error("Failed to unblock dates: {}", e.getMessage()); }

        kafkaTemplate.send(BookingCancelledEvent.TOPIC, BookingCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString()).bookingId(bookingId)
                .propertyId(booking.getPropertyId()).guestId(booking.getGuestId())
                .hostId(booking.getHostId()).cancellationReason(reason)
                .cancelledBy(requesterId).occurredAt(LocalDateTime.now()).build());

        return toDTO(saved);
    }

    public BookingDTO getBookingById(String bookingId, String requesterId) {
        Booking booking = getBookingEntity(bookingId);
        if (!booking.getGuestId().equals(requesterId) && !booking.getHostId().equals(requesterId))
            throw new UnauthorizedException("Access denied");
        return toDTO(booking);
    }

    public PagedResponse<BookingDTO> getGuestBookings(String guestId, Pageable pageable) {
        Page<Booking> page = bookingRepository.findByGuestIdOrderByCreatedAtDesc(guestId, pageable);
        return toPagedResponse(page);
    }

    public PagedResponse<BookingDTO> getHostBookings(String hostId, Pageable pageable) {
        Page<Booking> page = bookingRepository.findByHostIdOrderByCreatedAtDesc(hostId, pageable);
        return toPagedResponse(page);
    }

    public BigDecimal calculatePrice(String propertyId, LocalDate checkIn, LocalDate checkOut) {
        ApiResponse<PropertyDTO> response = propertyServiceClient.getProperty(propertyId);
        if (!response.isSuccess()) throw new PropertyNotAvailableException("Property not found");
        return pricingCalculator.calculate(response.getData(), checkIn, checkOut).total();
    }

    private Booking getBookingEntity(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));
    }

    private String buildLockKey(String propertyId, LocalDate checkIn, LocalDate checkOut) {
        return "lock:property:" + propertyId + ":dates:" + checkIn + ":" + checkOut;
    }

    private PagedResponse<BookingDTO> toPagedResponse(Page<Booking> page) {
        return PagedResponse.<BookingDTO>builder()
                .content(page.getContent().stream().map(this::toDTO).toList())
                .page(page.getNumber()).size(page.getSize()).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).first(page.isFirst()).last(page.isLast()).build();
    }

    private BookingDTO toDTO(Booking b) {
        return BookingDTO.builder().id(b.getId()).propertyId(b.getPropertyId()).guestId(b.getGuestId())
                .checkIn(b.getCheckIn()).checkOut(b.getCheckOut()).guestCount(b.getGuestCount())
                .baseAmount(b.getBaseAmount()).cleaningFee(b.getCleaningFee())
                .serviceFee(b.getServiceFee()).taxes(b.getTaxes()).totalAmount(b.getTotalAmount())
                .hostPayout(b.getHostPayout()).platformFee(b.getPlatformFee())
                .currency(b.getCurrency()).status(b.getStatus().name())
                .paymentIntentId(b.getPaymentIntentId()).accessCode(b.getAccessCode())
                .specialRequests(b.getSpecialRequests()).createdAt(b.getCreatedAt()).build();
    }
}
