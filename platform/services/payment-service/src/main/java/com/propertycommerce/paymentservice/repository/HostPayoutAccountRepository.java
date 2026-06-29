package com.propertycommerce.paymentservice.repository;
import com.propertycommerce.paymentservice.model.HostPayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HostPayoutAccountRepository extends JpaRepository<HostPayoutAccount, String> {
    Optional<HostPayoutAccount> findByHostId(String hostId);
}
