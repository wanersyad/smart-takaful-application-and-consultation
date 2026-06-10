package com.muqmeen.takaful;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ConfigurationPropertiesScan("com.muqmeen.takaful.config")
@EnableCaching
public class SmartTakafulApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTakafulApplication.class, args);
    }
}
