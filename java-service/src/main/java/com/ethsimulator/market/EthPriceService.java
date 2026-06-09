package com.ethsimulator.market;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.config.EthSimulatorProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class EthPriceService {

    private static final BigDecimal CLIENT_HINT_TOLERANCE = new BigDecimal("0.005");

    private final EthSimulatorProperties properties;
    private final ChainlinkEthUsdReader chainlinkReader;
    private final PublicApiEthPriceClient publicApiClient;
    private final EthPriceCache cache;
    private final Clock clock;

    public EthPriceService(
            EthSimulatorProperties properties,
            ChainlinkEthUsdReader chainlinkReader,
            PublicApiEthPriceClient publicApiClient,
            EthPriceCache cache,
            Clock clock
    ) {
        this.properties = properties;
        this.chainlinkReader = chainlinkReader;
        this.publicApiClient = publicApiClient;
        this.cache = cache;
        this.clock = clock;
    }

    public EthPriceQuote currentPrice() {
        return resolvePrice(null);
    }

    public EthPriceQuote resolvePrice(Double clientHintUsd) {
        EthPriceQuote quote = fetchFreshQuote();
        if (clientHintUsd == null) {
            return quote;
        }
        BigDecimal hint = BigDecimal.valueOf(clientHintUsd);
        BigDecimal drift = hint.subtract(quote.priceUsd()).abs()
                .divide(quote.priceUsd(), 10, RoundingMode.HALF_UP);
        if (drift.compareTo(CLIENT_HINT_TOLERANCE) > 0) {
            return new EthPriceQuote(quote.priceUsd(), quote.source(), quote.observedAt(), true);
        }
        return quote;
    }

    public BigDecimal priceUsdForSimulation(Double clientHintUsd) {
        return resolvePrice(clientHintUsd).priceUsd();
    }

    public String sourceKey(EthPriceQuote quote) {
        return quote.source().name().toLowerCase();
    }

    private EthPriceQuote fetchFreshQuote() {
        Duration ttl = Duration.ofSeconds(properties.getPriceCacheTtlSeconds());
        Optional<EthPriceQuote> cached = cache.getIfFresh(ttl);
        if (cached.isPresent()) {
            return cached.get();
        }

        Instant observedAt = clock.instant();

        Optional<BigDecimal> chainlink = chainlinkReader.readPriceUsd();
        if (chainlink.isPresent()) {
            EthPriceQuote quote = new EthPriceQuote(chainlink.get(), EthPriceSource.CHAINLINK, observedAt, false);
            cache.put(quote, ttl);
            return quote;
        }

        Optional<BigDecimal> publicApi = publicApiClient.fetchPriceUsd(properties.getPublicPriceApiUrl());
        if (publicApi.isPresent()) {
            EthPriceQuote quote = new EthPriceQuote(publicApi.get(), EthPriceSource.PUBLIC_API, observedAt, false);
            cache.put(quote, ttl);
            return quote;
        }

        EthPriceQuote quote = new EthPriceQuote(
                properties.getStaticEthPriceUsd(),
                EthPriceSource.STATIC,
                observedAt,
                false
        );
        cache.put(quote, ttl);
        return quote;
    }
}