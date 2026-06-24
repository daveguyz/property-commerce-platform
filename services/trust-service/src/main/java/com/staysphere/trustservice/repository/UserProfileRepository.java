package com.staysphere.trustservice.repository;
import com.staysphere.trustservice.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUserId(String userId);
    Optional<UserProfile> findByEmail(String email);
    Optional<UserProfile> findByShopifyCustomerId(String shopifyCustomerId);
}
