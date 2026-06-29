package com.propertycommerce.pricingengine.repository;
import com.propertycommerce.pricingengine.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, String> {
    Optional<PriceHistory> findByPropertyIdAndDate(String propertyId, LocalDate date);
    List<PriceHistory> findByPropertyIdAndDateBetweenOrderByDateAsc(String propertyId, LocalDate from, LocalDate to);

    @Query("SELECT AVG(p.dynamicRate) FROM PriceHistory p WHERE p.propertyId=:pid AND p.date >= :from")
    Double getAverageRateSince(@Param("pid") String propertyId, @Param("from") LocalDate from);
}
