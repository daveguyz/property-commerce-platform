package com.staysphere.propertyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.staysphere.propertyservice","com.staysphere.shared"})
@EnableDiscoveryClient @EnableCaching @EnableKafka @EnableScheduling
public class PropertyServiceApplication {
    public static void main(String[] args) { SpringApplication.run(PropertyServiceApplication.class, args); }
}
