package com.ethsimulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ethSimulatorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ETH Stablecoin Java Service")
                        .version("0.1.0")
                        .description("Deterministic simulation, ChartContract v2, and market quote APIs"));
    }
}