package com.staysphere.bookingengine.service;
import com.staysphere.shared.dto.PropertyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service @Slf4j
public class PricingCalculatorService {
    @Value("${staysphere.platform.service-fee-percent:10}") private double serviceFeePercent;
    @Value("${staysphere.platform.tax-percent:15}") private double taxPercent;
    @Value("${staysphere.platform.host-payout-percent:97}") private double hostPayoutPercent;

    public PricingResult calculate(PropertyDTO property, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) throw new IllegalArgumentException("Check-out must be after check-in");

        BigDecimal nightlyRate = property.getPricing().getCurrentDynamicRate() != null
                ? property.getPricing().getCurrentDynamicRate()
                : property.getPricing().getBaseRatePerNight();

        BigDecimal baseAmount = nightlyRate.multiply(BigDecimal.valueOf(nights));

        // Apply weekly/monthly discounts
        if (nights >= 28 && property.getPricing().getMonthlyDiscount() != null) {
            BigDecimal discount = baseAmount.multiply(property.getPricing().getMonthlyDiscount().divide(BigDecimal.valueOf(100)));
            baseAmount = baseAmount.subtract(discount);
        } else if (nights >= 7 && property.getPricing().getWeeklyDiscount() != null) {
            BigDecimal discount = baseAmount.multiply(property.getPricing().getWeeklyDiscount().divide(BigDecimal.valueOf(100)));
            baseAmount = baseAmount.subtract(discount);
        }

        BigDecimal cleaningFee = property.getPricing().getCleaningFee() != null
                ? property.getPricing().getCleaningFee() : BigDecimal.ZERO;
        BigDecimal serviceFee = baseAmount.multiply(BigDecimal.valueOf(serviceFeePercent / 100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = baseAmount.add(cleaningFee).add(serviceFee);
        BigDecimal taxes = subtotal.multiply(BigDecimal.valueOf(taxPercent / 100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxes);
        BigDecimal platformFee = serviceFee;
        BigDecimal hostPayout = baseAmount.add(cleaningFee)
                .multiply(BigDecimal.valueOf(hostPayoutPercent / 100)).setScale(2, RoundingMode.HALF_UP);

        return new PricingResult(baseAmount, cleaningFee, serviceFee, taxes, total, hostPayout, platformFee,
                property.getPricing().getCurrency() != null ? property.getPricing().getCurrency() : "NAD", nights);
    }

    public record PricingResult(BigDecimal baseAmount, BigDecimal cleaningFee, BigDecimal serviceFee,
                                 BigDecimal taxes, BigDecimal total, BigDecimal hostPayout,
                                 BigDecimal platformFee, String currency, long nights) {}
}
