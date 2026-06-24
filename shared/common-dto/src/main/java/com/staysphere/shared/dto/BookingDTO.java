package com.staysphere.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingDTO {
    private String id;
    @NotBlank private String propertyId;
    @NotBlank private String guestId;
    @NotNull private LocalDate checkIn;
    @NotNull private LocalDate checkOut;
    @NotNull @Min(1) private Integer guestCount;
    private Integer childrenCount;
    private Integer infantCount;
    private Integer petCount;
    private BigDecimal baseAmount;
    private BigDecimal cleaningFee;
    private BigDecimal serviceFee;
    private BigDecimal taxes;
    private BigDecimal totalAmount;
    private BigDecimal hostPayout;
    private BigDecimal platformFee;
    private String currency;
    private String status;
    private String paymentIntentId;
    private String accessCode;
    private String specialRequests;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private PropertyDTO property;
    private GuestProfileDTO guest;
}
