package com.propertycommerce.bookingengine;

import com.propertycommerce.bookingengine.model.Booking;
import com.propertycommerce.bookingengine.model.BookingStatus;
import com.propertycommerce.bookingengine.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BookingDoubleBookingTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pcp_bookings")
            .withUsername("pcp")
            .withPassword("pcp_secret");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired BookingRepository bookingRepository;

    @Test
    @DisplayName("Conflict detection correctly identifies overlapping bookings")
    void shouldDetectConflictingDates() {
        String propertyId = "test-property-001";
        LocalDate checkIn = LocalDate.of(2025, 8, 10);
        LocalDate checkOut = LocalDate.of(2025, 8, 15);

        Booking existing = Booking.builder()
                .propertyId(propertyId).guestId("guest-1").hostId("host-1")
                .checkIn(checkIn).checkOut(checkOut).guestCount(2)
                .status(BookingStatus.CONFIRMED).currency("NAD").build();
        bookingRepository.save(existing);

        // Overlapping scenarios
        assertThat(bookingRepository.existsConflictingBooking(propertyId,
                LocalDate.of(2025, 8, 12), LocalDate.of(2025, 8, 18))).isTrue();
        assertThat(bookingRepository.existsConflictingBooking(propertyId,
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 11))).isTrue();
        assertThat(bookingRepository.existsConflictingBooking(propertyId,
                checkIn, checkOut)).isTrue();

        // Non-overlapping
        assertThat(bookingRepository.existsConflictingBooking(propertyId,
                LocalDate.of(2025, 8, 15), LocalDate.of(2025, 8, 20))).isFalse();
        assertThat(bookingRepository.existsConflictingBooking(propertyId,
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 10))).isFalse();
    }

    @Test
    @DisplayName("Concurrent bookings for same property/dates: only one should succeed via DB constraint")
    void shouldPreventDoubleBookingUnderConcurrency() throws InterruptedException {
        String propertyId = "concurrent-test-property";
        LocalDate checkIn = LocalDate.of(2025, 9, 1);
        LocalDate checkOut = LocalDate.of(2025, 9, 7);

        int threads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean hasConflict = bookingRepository.existsConflictingBooking(
                            propertyId, checkIn, checkOut);
                    if (!hasConflict) {
                        Booking b = Booking.builder()
                                .propertyId(propertyId)
                                .guestId("guest-concurrent-" + idx)
                                .hostId("host-1")
                                .checkIn(checkIn).checkOut(checkOut)
                                .guestCount(2)
                                .status(BookingStatus.CONFIRMED)
                                .currency("NAD").build();
                        bookingRepository.save(b);
                        successCount.incrementAndGet();
                    } else {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long totalConfirmed = bookingRepository.countByGuestIdAndStatus(
                "guest-concurrent-0", BookingStatus.CONFIRMED)
                + bookingRepository.findAll().stream()
                    .filter(b -> propertyId.equals(b.getPropertyId())
                            && b.getStatus() == BookingStatus.CONFIRMED)
                    .count();

        assertThat(totalConfirmed).isLessThanOrEqualTo(1);
    }
}
