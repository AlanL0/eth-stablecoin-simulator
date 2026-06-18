package com.ethsimulator.market;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.config.EthSimulatorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;

class EthPriceServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-09T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private EthSimulatorProperties properties;
    private EthPriceCache cache;

    @BeforeEach
    void setUp() {
        properties = new EthSimulatorProperties();
        properties.setStaticEthPriceUsd(new BigDecimal("3800"));
        properties.setPriceCacheTtlSeconds(60);
        cache = new EthPriceCache(FIXED_CLOCK);
    }

    @Test
    void chainlinkSuccess() {
        EthPriceService service = service(chainlinkReturning("3850.12"), "");

        EthPriceQuote quote = service.currentPrice();

        assertEquals(EthPriceSource.CHAINLINK, quote.source());
        assertEquals(3850.12, quote.priceUsd().doubleValue(), 0.01);
        assertFalse(quote.stale());
    }

    @Test
    void publicApiFallbackWhenChainlinkUnavailable() {
        WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        try {
            wireMock.stubFor(get(urlEqualTo("/eth-price"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"ethereum\":{\"usd\":3925.5}}")));

            properties.setPublicPriceApiUrl(wireMock.baseUrl() + "/eth-price");
            EthPriceService service = service(Optional::empty, properties.getPublicPriceApiUrl());

            EthPriceQuote quote = service.currentPrice();

            assertEquals(EthPriceSource.PUBLIC_API, quote.source());
            assertEquals(3925.5, quote.priceUsd().doubleValue(), 0.01);
        } finally {
            wireMock.stop();
        }
    }

    @Test
    void staticFallbackWhenUpstreamUnavailable() {
        EthPriceService service = service(Optional::empty, "");

        EthPriceQuote quote = service.currentPrice();

        assertEquals(EthPriceSource.STATIC, quote.source());
        assertEquals(3800.0, quote.priceUsd().doubleValue(), 0.01);
        assertTrue(quote.degraded());
    }

    @Test
    void cacheHitReturnsCacheSource() {
        EthPriceService service = service(chainlinkReturning("3850.00"), "");

        service.currentPrice();
        EthPriceQuote cached = service.currentPrice();

        assertEquals(EthPriceSource.CACHE, cached.source());
        assertEquals(3850.0, cached.priceUsd().doubleValue(), 0.01);
    }

    @Test
    void refetchesAfterCacheExpires() {
        MutableClock clock = new MutableClock(FIXED_INSTANT);
        properties.setPriceCacheTtlSeconds(60);
        EthPriceService service = new EthPriceService(
                properties,
                chainlinkReturning("3850.00"),
                new PublicApiEthPriceClient(RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()).build()),
                new EthPriceCache(clock),
                clock,
                mock(Environment.class)
        );

        service.currentPrice();
        clock.advanceSeconds(61);
        EthPriceQuote refreshed = service.currentPrice();

        assertEquals(EthPriceSource.CHAINLINK, refreshed.source());
        assertEquals(3850.0, refreshed.priceUsd().doubleValue(), 0.01);
    }

    @Test
    void rejectsStaleClientHintBeyondTolerance() {
        EthPriceService service = service(chainlinkReturning("3850.00"), "");

        EthPriceQuote quote = service.resolvePrice(1000.0);

        assertEquals(3850.0, quote.priceUsd().doubleValue(), 0.01);
        assertEquals(EthPriceSource.CHAINLINK, quote.source());
        assertEquals(true, quote.stale());
    }

    private EthPriceService service(ChainlinkEthUsdReader chainlinkReader, String publicApiUrl) {
        properties.setPublicPriceApiUrl(publicApiUrl);
        return new EthPriceService(
                properties,
                chainlinkReader,
                new PublicApiEthPriceClient(RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()).build()),
                cache,
                FIXED_CLOCK,
                mock(Environment.class)
        );
    }

    private static ChainlinkEthUsdReader chainlinkReturning(String price) {
        return () -> Optional.of(new BigDecimal(price));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneOffset zone;

        private MutableClock(Instant instant) {
            this.instant = instant;
            this.zone = ZoneOffset.UTC;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return zone;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}