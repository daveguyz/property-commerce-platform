package com.propertycommerce.pricingengine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(scanBasePackages = {"com.propertycommerce.pricingengine","com.propertycommerce.shared"})
@EnableDiscoveryClient @EnableFeignClients @EnableScheduling
public class PricingEngineApplication {
    public static void main(String[] args) { SpringApplication.run(PricingEngineApplication.class, args); }
}
