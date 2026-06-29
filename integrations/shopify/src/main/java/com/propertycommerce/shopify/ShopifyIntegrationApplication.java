package com.propertycommerce.shopify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ShopifyIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopifyIntegrationApplication.class, args);
    }
}
