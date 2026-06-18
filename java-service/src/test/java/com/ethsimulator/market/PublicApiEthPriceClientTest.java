package com.ethsimulator.market;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;

class PublicApiEthPriceClientTest {

    private PublicApiEthPriceClient client(WireMockServer wireMock) {
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        return new PublicApiEthPriceClient(restClient);
    }

    @Test
    void parsesCoinGeckoShapedResponse() {
        WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        try {
            wireMock.stubFor(get(urlEqualTo("/eth-price"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"ethereum\":{\"usd\":3925.5}}")));

            Optional<BigDecimal> price = client(wireMock).fetchPriceUsd(wireMock.baseUrl() + "/eth-price");

            assertTrue(price.isPresent());
            assertEquals(3925.5, price.get().doubleValue(), 0.01);
        } finally {
            wireMock.stop();
        }
    }

    @Test
    void parsesAnyCoinGeckoCoinId() {
        WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        try {
            wireMock.stubFor(get(urlEqualTo("/price"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"bitcoin\":{\"usd\":98000.0}}")));

            Optional<BigDecimal> price = client(wireMock).fetchPriceUsd(wireMock.baseUrl() + "/price");

            assertTrue(price.isPresent());
            assertEquals(98000.0, price.get().doubleValue(), 0.01);
        } finally {
            wireMock.stop();
        }
    }

    @Test
    void returnsEmptyOnServerError() {
        WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        try {
            wireMock.stubFor(get(urlEqualTo("/eth-price"))
                    .willReturn(aResponse().withStatus(503)));

            Optional<BigDecimal> price = client(wireMock).fetchPriceUsd(wireMock.baseUrl() + "/eth-price");

            assertTrue(price.isEmpty());
        } finally {
            wireMock.stop();
        }
    }

    @Test
    void returnsEmptyOnUnparseableResponse() {
        WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        try {
            wireMock.stubFor(get(urlEqualTo("/eth-price"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"foo\":\"bar\"}")));

            Optional<BigDecimal> price = client(wireMock).fetchPriceUsd(wireMock.baseUrl() + "/eth-price");

            assertTrue(price.isEmpty());
        } finally {
            wireMock.stop();
        }
    }

    @Test
    void returnsEmptyForBlankUrl() {
        WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        try {
            PublicApiEthPriceClient client = client(wireMock);
            assertTrue(client.fetchPriceUsd("").isEmpty());
            assertTrue(client.fetchPriceUsd(null).isEmpty());
        } finally {
            wireMock.stop();
        }
    }
}