package com.propertycommerce.analyticsservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(scanBasePackages = {"com.propertycommerce.analyticsservice","com.propertycommerce.shared"})
@EnableDiscoveryClient @EnableKafka @EnableScheduling
public class AnalyticsServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AnalyticsServiceApplication.class, args); }
}
