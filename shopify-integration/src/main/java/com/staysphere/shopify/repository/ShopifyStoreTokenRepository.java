package com.staysphere.shopify.repository;

import com.staysphere.shopify.model.ShopifyStoreToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShopifyStoreTokenRepository extends JpaRepository<ShopifyStoreToken, String> {
    Optional<ShopifyStoreToken> findByShopDomain(String shopDomain);
    boolean existsByShopDomain(String shopDomain);
}
