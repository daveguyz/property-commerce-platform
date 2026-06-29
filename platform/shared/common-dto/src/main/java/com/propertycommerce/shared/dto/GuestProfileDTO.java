package com.propertycommerce.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GuestProfileDTO {
    private String id;
    private String shopifyCustomerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private LocalDate dateOfBirth;
    private String nationality;
    private Boolean idVerified;
    private Boolean phoneVerified;
    private Boolean emailVerified;
    private Double trustScore;
    private Double averageRating;
    private Integer totalBookings;
    private String stripeCustomerId;
    private LocalDateTime createdAt;
    private LocalDateTime memberSince;
}
