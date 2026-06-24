package com.staysphere.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingCreatedEvent {
    public static final String TOPIC = "booking.created";
    private String eventId;
    private String bookingId;
    private String propertyId;
    private String guestId;
    private String hostId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guestCount;
    private BigDecimal totalAmount;
    private BigDecimal platformFee;
    private BigDecimal hostPayout;
    private String currency;
    private String guestEmail;
    private String guestPhone;
    private String propertyName;
    private String propertyAddress;
    private LocalDateTime occurredAt;
}
