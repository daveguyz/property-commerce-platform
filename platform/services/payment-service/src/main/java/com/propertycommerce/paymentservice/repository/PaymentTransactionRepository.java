package com.propertycommerce.paymentservice.repository;
import com.propertycommerce.paymentservice.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    Optional<PaymentTransaction> findByPaymentIntentId(String paymentIntentId);
    Optional<PaymentTransaction> findByBookingId(String bookingId);
}
