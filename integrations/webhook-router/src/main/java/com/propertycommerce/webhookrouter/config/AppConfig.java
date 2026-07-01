package com.propertycommerce.webhookrouter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class AppConfig {
    @Bean
    public ObjectMapper objectMapper() { return new ObjectMapper(); }
}
