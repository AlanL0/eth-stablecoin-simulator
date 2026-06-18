package com.ethsimulator.market;

import com.ethsimulator.util.FinancialMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Component
public class PublicApiEthPriceClient {

    private static final Logger log = LoggerFactory.getLogger(PublicApiEthPriceClient.class);

    private final RestClient restClient;

    public PublicApiEthPriceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<BigDecimal> fetchPriceUsd(String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = restClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null) {
                return Optional.empty();
            }
            return parsePrice(root);
        } catch (RuntimeException ex) {
            log.warn("Public price API failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> parsePrice(JsonNode root) {
        JsonNode ethereum = root.path("ethereum").path("usd");
        if (ethereum.isNumber()) {
            return Optional.of(FinancialMath.parseJsonNumber(ethereum));
        }
        Iterator<Map.Entry<String, JsonNode>> fields = root.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode usd = entry.getValue().path("usd");
            if (usd.isNumber()) {
                return Optional.of(FinancialMath.parseJsonNumber(usd));
            }
        }
        JsonNode priceUsd = root.path("priceUsd");
        if (priceUsd.isNumber()) {
            return Optional.of(FinancialMath.parseJsonNumber(priceUsd));
        }
        if (root.isNumber()) {
            return Optional.of(FinancialMath.parseJsonNumber(root));
        }
        return Optional.empty();
    }
}