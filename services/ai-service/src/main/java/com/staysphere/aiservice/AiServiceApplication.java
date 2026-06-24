package com.staysphere.aiservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
@SpringBootApplication(scanBasePackages = {"com.staysphere.aiservice","com.staysphere.shared"})
@EnableDiscoveryClient @EnableFeignClients
public class AiServiceApplication {
    public static void main(String[] args) { SpringApplication.run(AiServiceApplication.class, args); }
}
