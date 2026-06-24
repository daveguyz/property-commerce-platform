package com.staysphere.messagingservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@SpringBootApplication(scanBasePackages = {"com.staysphere.messagingservice","com.staysphere.shared"})
@EnableDiscoveryClient
public class MessagingServiceApplication {
    public static void main(String[] args) { SpringApplication.run(MessagingServiceApplication.class, args); }
}
