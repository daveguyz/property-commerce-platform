package com.staysphere.propertyservice.repository;
import com.staysphere.propertyservice.model.AvailabilityBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<AvailabilityBlock, String> {
    @Query("SELECT COUNT(a)>0 FROM AvailabilityBlock a WHERE a.propertyId=:propertyId AND a.startDate<:checkOut AND a.endDate>:checkIn")
    boolean existsConflictingBlock(@Param("propertyId") String propertyId,
                                    @Param("checkIn") LocalDate checkIn,
                                    @Param("checkOut") LocalDate checkOut);

    @Query("SELECT a FROM AvailabilityBlock a WHERE a.propertyId=:propertyId AND a.startDate>=:from AND a.endDate<=:to ORDER BY a.startDate ASC")
    List<AvailabilityBlock> findBlocksInRange(@Param("propertyId") String propertyId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    void deleteByBookingId(String bookingId);
}
