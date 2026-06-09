package com.ethsimulator.market;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class PublicApiEthPriceClient {

    private static final Logger log = LoggerFactory.getLogger(PublicApiEthPriceClient.class);

    private final RestTemplate restTemplate;

    public PublicApiEthPriceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<BigDecimal> fetchPriceUsd(String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = restTemplate.getForObject(apiUrl, JsonNode.class);
            if (root == null) {
                return Optional.empty();
            }
            return parsePrice(root);
        } catch (RestClientException ex) {
            log.warn("Public price API failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> parsePrice(JsonNode root) {
        JsonNode ethereum = root.path("ethereum").path("usd");
        if (ethereum.isNumber()) {
            return Optional.of(BigDecimal.valueOf(ethereum.asDouble()));
        }
        JsonNode priceUsd = root.path("priceUsd");
        if (priceUsd.isNumber()) {
            return Optional.of(BigDecimal.valueOf(priceUsd.asDouble()));
        }
        if (root.isNumber()) {
            return Optional.of(BigDecimal.valueOf(root.asDouble()));
        }
        return Optional.empty();
    }
}