package com.staysphere.searchservice.service;

import com.staysphere.searchservice.model.AuctionSearchDocument;
import com.staysphere.searchservice.repository.AuctionSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
public class AuctionSearchService {

    private final AuctionSearchRepository auctionSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // ─── Index operations ────────────────────────────────────────────────────

    public void indexLot(AuctionSearchDocument doc) {
        doc.setIndexedAt(LocalDateTime.now());
        auctionSearchRepository.save(doc);
        log.debug("[AuctionSearch] Indexed lot {}", doc.getId());
    }

    public void removeLot(String lotId) {
        auctionSearchRepository.deleteById(lotId);
        log.info("[AuctionSearch] Removed lot {} from index", lotId);
    }

    public void updateLotStatus(String lotId, String newStatus, BigDecimal currentBid,
                                Integer totalBids, Integer uniqueBidders, boolean livestreamActive) {
        Optional<AuctionSearchDocument> opt = auctionSearchRepository.findById(lotId);
        opt.ifPresent(doc -> {
            doc.setStatus(newStatus);
            if (currentBid != null) doc.setCurrentBidAmount(currentBid);
            if (totalBids != null) doc.setTotalBids(totalBids);
            if (uniqueBidders != null) doc.setUniqueBidders(uniqueBidders);
            doc.setLivestreamActive(livestreamActive);
            doc.setIndexedAt(LocalDateTime.now());
            auctionSearchRepository.save(doc);
        });
    }

    // ─── Search ──────────────────────────────────────────────────────────────

    /**
     * Full-text + faceted search across auction lots.
     * Mirrors PropertySearchService query building style.
     */
    public List<AuctionSearchDocument> search(AuctionSearchRequest request, int page, int size) {
        Criteria criteria = new Criteria();

        // Status filter — default to active statuses if not specified
        List<String> statuses = request.statuses() != null && !request.statuses().isEmpty()
                ? request.statuses()
                : List.of("SCHEDULED", "OPEN", "EXTENDED");
        criteria = criteria.and(new Criteria("status").in(statuses));

        // Free-text query across title + description
        if (request.query() != null && !request.query().isBlank()) {
            criteria = criteria.and(
                    new Criteria("title").contains(request.query())
                    .or(new Criteria("description").contains(request.query()))
            );
        }

        // Location filters
        if (request.city() != null && !request.city().isBlank()) {
            criteria = criteria.and(new Criteria("city").is(request.city()));
        }

        // Auction type filter
        if (request.auctionType() != null && !request.auctionType().isBlank()) {
            criteria = criteria.and(new Criteria("auctionType").is(request.auctionType()));
        }

        // Price range (against startingPrice for upcoming, currentBidAmount for live)
        if (request.minPrice() != null) {
            criteria = criteria.and(new Criteria("startingPrice").greaterThanEqual(request.minPrice()));
        }
        if (request.maxPrice() != null) {
            criteria = criteria.and(new Criteria("startingPrice").lessThanEqual(request.maxPrice()));
        }

        // Feature flags
        if (Boolean.TRUE.equals(request.liveOnly())) {
            criteria = criteria.and(new Criteria("status").in(List.of("OPEN", "EXTENDED")));
        }
        if (Boolean.TRUE.equals(request.hasLivestream())) {
            criteria = criteria.and(new Criteria("livestreamActive").is(true));
        }

        // Time window — only lots starting within next N days
        if (request.startsWithinDays() != null) {
            criteria = criteria.and(new Criteria("startsAt")
                    .between(LocalDateTime.now(), LocalDateTime.now().plusDays(request.startsWithinDays())));
        }

        CriteriaQuery query = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(page, size));

        SearchHits<AuctionSearchDocument> hits = elasticsearchOperations.search(
                query, AuctionSearchDocument.class);

        List<AuctionSearchDocument> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        log.debug("[AuctionSearch] Query returned {} hits", results.size());
        return results;
    }

    /** Live lots only — ordered by time remaining (ascending). */
    public List<AuctionSearchDocument> findLiveLots() {
        return auctionSearchRepository.findByStatusInOrderByStartsAtAsc(
                List.of("OPEN", "EXTENDED"));
    }

    /** Upcoming lots in a specific city. */
    public List<AuctionSearchDocument> findUpcomingByCity(String city) {
        return auctionSearchRepository.findByCityAndStatusIn(
                city, List.of("SCHEDULED", "OPEN", "EXTENDED"));
    }

    public record AuctionSearchRequest(
            String query,
            String city,
            String auctionType,
            List<String> statuses,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean liveOnly,
            Boolean hasLivestream,
            Integer startsWithinDays
    ) {}
}
