package com.propertycommerce.bookingengine.controller;
import com.propertycommerce.bookingengine.service.*;
import com.propertycommerce.shared.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController @RequestMapping("/api/v1/bookings") @RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final NegotiationService negotiationService;
    private final TripBookingService tripBookingService;
    private final PricingCalculatorService pricingCalculatorService;

    @PostMapping
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<BookingDTO>> createBooking(
            @Valid @RequestBody BookingDTO request, @AuthenticationPrincipal String guestId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(bookingService.createBooking(request, guestId), "Booking created"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingDTO>> getBooking(
            @PathVariable String id, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id, userId)));
    }

    @GetMapping("/guest")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingDTO>>> getGuestBookings(
            @AuthenticationPrincipal String guestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getGuestBookings(guestId, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/host")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingDTO>>> getHostBookings(
            @AuthenticationPrincipal String hostId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getHostBookings(hostId, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingDTO>> cancelBooking(
            @PathVariable String id, @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.cancelBooking(id, userId, body.getOrDefault("reason", "No reason provided"))));
    }

    @GetMapping("/price-estimate")
    public ResponseEntity<ApiResponse<BigDecimal>> getPriceEstimate(
            @RequestParam String propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.calculatePrice(propertyId, checkIn, checkOut)));
    }

    @PostMapping("/negotiate")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<Object>> initiateNegotiation(
            @RequestBody Map<String, Object> request, @AuthenticationPrincipal String guestId) {
        var result = negotiationService.initiateNegotiation(
                (String) request.get("propertyId"), guestId,
                new BigDecimal(request.get("offeredPrice").toString()),
                (String) request.get("message"),
                LocalDate.parse((String) request.get("checkIn")),
                LocalDate.parse((String) request.get("checkOut")),
                (Integer) request.get("guestCount"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result, "Negotiation initiated"));
    }

    @PostMapping("/negotiate/{id}/respond")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ApiResponse<Object>> respondToNegotiation(
            @PathVariable String id, @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal String hostId) {
        boolean accepted = Boolean.parseBoolean(request.get("accepted").toString());
        BigDecimal counter = request.get("counterPrice") != null
                ? new BigDecimal(request.get("counterPrice").toString()) : null;
        return ResponseEntity.ok(ApiResponse.success(
                negotiationService.respondToNegotiation(id, hostId, accepted, counter,
                        (String) request.get("hostResponse"))));
    }
}
