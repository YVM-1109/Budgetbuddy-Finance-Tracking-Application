package com.budgetbuddy.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Shared beans. The Clock is injected everywhere so reporting time
 * (Asia/Kolkata default) is explicit and testable.
 */
@Configuration
public class BeanConfig {

    @Bean
    public Clock clock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }
}
