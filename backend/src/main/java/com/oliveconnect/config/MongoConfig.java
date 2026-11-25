package com.oliveconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.time.Clock;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
