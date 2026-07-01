package com.propertycommerce.webhookrouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableRetry
@EnableAsync
@EnableScheduling
public class WebhookRouterApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebhookRouterApplication.class, args);
    }
}
