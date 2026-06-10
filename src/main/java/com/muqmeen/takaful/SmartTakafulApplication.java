package com.muqmeen.takaful;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan("com.muqmeen.takaful.config")
@EnableCaching
public class SmartTakafulApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTakafulApplication.class, args);
    }

    /**
     * Pin the JVM to Malaysia time so every LocalDateTime.now() (timestamps on applications,
     * enquiries, etc.) and every formatted timestamp shown in the UI is in MYT — regardless of
     * the host's clock (Railway containers run in UTC by default).
     */
    @PostConstruct
    void useMalaysiaTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
    }
}
