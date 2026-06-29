package com.propertycommerce.messagingservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@SpringBootApplication(scanBasePackages = {"com.propertycommerce.messagingservice","com.propertycommerce.shared"})
@EnableDiscoveryClient
public class MessagingServiceApplication {
    public static void main(String[] args) { SpringApplication.run(MessagingServiceApplication.class, args); }
}
