package com.ethsimulator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({EthSimulatorProperties.class, AgentAiProperties.class})
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RestClient restClient(EthSimulatorProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getHttpConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getHttpReadTimeoutMs()));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "ethStableCoin-simulator/1.0")
                .build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer(EthSimulatorProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = properties.getAllowedOrigins().split(",");
                registry.addMapping("/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}