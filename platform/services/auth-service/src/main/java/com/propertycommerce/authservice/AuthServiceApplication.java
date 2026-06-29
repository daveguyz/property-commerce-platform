package com.propertycommerce.authservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
@SpringBootApplication(scanBasePackages = {"com.propertycommerce.authservice","com.propertycommerce.shared"})
@EnableDiscoveryClient @EnableKafka
public class AuthServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AuthServiceApplication.class, args); }
}
