package com.propertycommerce.propertyservice.service;
import com.propertycommerce.propertyservice.model.AvailabilityBlock;
import com.propertycommerce.propertyservice.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class AvailabilityService {
    private final AvailabilityRepository availabilityRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration LOCK_TTL = Duration.ofMinutes(15);

    public boolean isAvailable(String propertyId, LocalDate checkIn, LocalDate checkOut) {
        if (availabilityRepository.existsConflictingBlock(propertyId, checkIn, checkOut)) return false;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(buildLockKey(propertyId, checkIn, checkOut)));
    }

    public boolean acquireLock(String propertyId, LocalDate checkIn, LocalDate checkOut, String sessionId) {
        if (!isAvailable(propertyId, checkIn, checkOut)) return false;
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(buildLockKey(propertyId, checkIn, checkOut), sessionId, LOCK_TTL));
    }

    public void releaseLock(String propertyId, LocalDate checkIn, LocalDate checkOut, String sessionId) {
        String key = buildLockKey(propertyId, checkIn, checkOut);
        if (sessionId.equals(redisTemplate.opsForValue().get(key))) redisTemplate.delete(key);
    }

    @Transactional
    public void blockDates(String propertyId, LocalDate startDate, LocalDate endDate,
                            AvailabilityBlock.BlockType blockType, String bookingId, String reason) {
        availabilityRepository.save(AvailabilityBlock.builder()
                .propertyId(propertyId).startDate(startDate).endDate(endDate)
                .blockType(blockType).bookingId(bookingId).reason(reason).build());
    }

    @Transactional
    public void unblockBookingDates(String bookingId) { availabilityRepository.deleteByBookingId(bookingId); }

    public Map<LocalDate, String> getAvailabilityCalendar(String propertyId, LocalDate from, LocalDate to) {
        Map<LocalDate, String> calendar = new TreeMap<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) { calendar.put(cur, "AVAILABLE"); cur = cur.plusDays(1); }
        for (AvailabilityBlock b : availabilityRepository.findBlocksInRange(propertyId, from, to)) {
            LocalDate d = b.getStartDate();
            while (!d.isAfter(b.getEndDate())) { if (calendar.containsKey(d)) calendar.put(d, b.getBlockType().name()); d = d.plusDays(1); }
        }
        return calendar;
    }

    private String buildLockKey(String propertyId, LocalDate checkIn, LocalDate checkOut) {
        return "lock:property:" + propertyId + ":dates:" + checkIn + ":" + checkOut;
    }
}
