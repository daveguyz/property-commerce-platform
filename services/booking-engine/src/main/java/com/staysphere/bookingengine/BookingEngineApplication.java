package com.staysphere.bookingengine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.staysphere.bookingengine","com.staysphere.shared"})
@EnableDiscoveryClient @EnableFeignClients @EnableKafka @EnableScheduling
public class BookingEngineApplication {
    public static void main(String[] args) { SpringApplication.run(BookingEngineApplication.class, args); }
}
