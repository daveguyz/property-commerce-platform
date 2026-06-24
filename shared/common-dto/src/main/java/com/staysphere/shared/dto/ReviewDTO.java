package com.staysphere.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewDTO {
    private String id;
    private String bookingId;
    private String propertyId;
    private String guestId;
    private String hostId;
    @NotNull @Min(1) @Max(5) private Integer overallRating;
    @NotNull @Min(1) @Max(5) private Integer cleanlinessRating;
    @NotNull @Min(1) @Max(5) private Integer accuracyRating;
    @NotNull @Min(1) @Max(5) private Integer checkInRating;
    @NotNull @Min(1) @Max(5) private Integer communicationRating;
    @NotNull @Min(1) @Max(5) private Integer locationRating;
    @NotNull @Min(1) @Max(5) private Integer valueRating;
    @NotBlank @Size(min = 20, max = 2000) private String comment;
    private String hostResponse;
    private LocalDateTime hostResponseAt;
    private LocalDateTime createdAt;
    private GuestProfileDTO guest;
}
