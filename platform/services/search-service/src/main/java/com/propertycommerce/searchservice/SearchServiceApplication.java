package com.propertycommerce.searchservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.kafka.annotation.EnableKafka;
@SpringBootApplication(scanBasePackages = {"com.propertycommerce.searchservice","com.propertycommerce.shared"})
@EnableDiscoveryClient @EnableKafka @EnableElasticsearchRepositories
public class SearchServiceApplication {
    public static void main(String[] args) { SpringApplication.run(SearchServiceApplication.class, args); }
}
