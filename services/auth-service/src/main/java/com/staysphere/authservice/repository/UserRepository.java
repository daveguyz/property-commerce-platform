package com.staysphere.authservice.repository;
import com.staysphere.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailVerificationToken(String token);
    Optional<User> findByPasswordResetToken(String token);
    Optional<User> findByShopifyCustomerId(String shopifyCustomerId);
    boolean existsByEmail(String email);
}
