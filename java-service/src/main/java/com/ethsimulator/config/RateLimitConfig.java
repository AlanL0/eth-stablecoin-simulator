package com.ethsimulator.config;

import com.ethsimulator.web.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter rateLimiter(Clock clock, EthSimulatorProperties properties) {
        return new RateLimiter(
                clock,
                properties.getRateLimitRequestsPerMinute(),
                properties.getRateLimitBurst(),
                properties.getRateLimitMaxClients()
        );
    }
}