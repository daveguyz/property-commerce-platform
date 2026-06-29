package com.propertycommerce.trustservice.service;
import com.propertycommerce.trustservice.model.Review;
import com.propertycommerce.trustservice.repository.ReviewRepository;
import com.propertycommerce.shared.dto.*;
import com.propertycommerce.shared.events.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service @Slf4j @RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final TrustScoreService trustScoreService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public ReviewDTO createReview(ReviewDTO dto, String guestId) {
        if (reviewRepository.existsByBookingIdAndGuestId(dto.getBookingId(), guestId))
            throw new IllegalStateException("You have already reviewed this booking");

        Review review = Review.builder()
                .bookingId(dto.getBookingId()).propertyId(dto.getPropertyId())
                .guestId(guestId).hostId(dto.getHostId())
                .overallRating(dto.getOverallRating()).cleanlinessRating(dto.getCleanlinessRating())
                .accuracyRating(dto.getAccuracyRating()).checkInRating(dto.getCheckInRating())
                .communicationRating(dto.getCommunicationRating()).locationRating(dto.getLocationRating())
                .valueRating(dto.getValueRating()).comment(dto.getComment()).build();

        Review saved = reviewRepository.save(review);

        // Recalculate trust scores
        try { trustScoreService.recalculateTrustScore(guestId); } catch (Exception e) { log.warn("Trust score update failed: {}", e.getMessage()); }

        // Publish event so property-service can update its rating
        kafkaTemplate.send(ReviewCreatedEvent.TOPIC, ReviewCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString()).reviewId(saved.getId())
                .bookingId(saved.getBookingId()).propertyId(saved.getPropertyId())
                .guestId(guestId).hostId(saved.getHostId())
                .overallRating(saved.getOverallRating()).occurredAt(LocalDateTime.now()).build());

        return toDTO(saved);
    }

    public PagedResponse<ReviewDTO> getPropertyReviews(String propertyId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId, pageable);
        return PagedResponse.<ReviewDTO>builder()
                .content(page.getContent().stream().map(this::toDTO).toList())
                .page(page.getNumber()).size(page.getSize()).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).first(page.isFirst()).last(page.isLast()).build();
    }

    private ReviewDTO toDTO(Review r) {
        return ReviewDTO.builder().id(r.getId()).bookingId(r.getBookingId())
                .propertyId(r.getPropertyId()).guestId(r.getGuestId()).hostId(r.getHostId())
                .overallRating(r.getOverallRating()).cleanlinessRating(r.getCleanlinessRating())
                .accuracyRating(r.getAccuracyRating()).checkInRating(r.getCheckInRating())
                .communicationRating(r.getCommunicationRating()).locationRating(r.getLocationRating())
                .valueRating(r.getValueRating()).comment(r.getComment())
                .hostResponse(r.getHostResponse()).hostResponseAt(r.getHostResponseAt())
                .createdAt(r.getCreatedAt()).build();
    }
}
