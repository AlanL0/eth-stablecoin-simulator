package com.ethsimulator.market;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.config.EthSimulatorProperties;
import com.ethsimulator.util.FinancialMath;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class EthPriceService {

    private static final BigDecimal CLIENT_HINT_TOLERANCE = FinancialMath.bd("0.005");

    private final EthSimulatorProperties properties;
    private final ChainlinkEthUsdReader chainlinkReader;
    private final PublicApiEthPriceClient publicApiClient;
    private final EthPriceCache cache;
    private final Clock clock;
    private final Environment environment;

    public EthPriceService(
            EthSimulatorProperties properties,
            ChainlinkEthUsdReader chainlinkReader,
            PublicApiEthPriceClient publicApiClient,
            EthPriceCache cache,
            Clock clock,
            Environment environment
    ) {
        this.properties = properties;
        this.chainlinkReader = chainlinkReader;
        this.publicApiClient = publicApiClient;
        this.cache = cache;
        this.clock = clock;
        this.environment = environment;
    }

    public EthPriceQuote currentPrice() {
        return resolvePrice(null);
    }

    public EthPriceQuote resolvePrice(BigDecimal clientHintUsd) {
        EthPriceQuote quote = fetchFreshQuote();
        if (clientHintUsd == null) {
            return quote;
        }
        BigDecimal drift = FinancialMath.divide(
                FinancialMath.subtract(clientHintUsd, quote.priceUsd()).abs(),
                quote.priceUsd(),
                FinancialMath.RATE_SCALE);
        if (drift.compareTo(CLIENT_HINT_TOLERANCE) > 0) {
            return new EthPriceQuote(quote.priceUsd(), quote.source(), quote.observedAt(), true, quote.degraded());
        }
        return quote;
    }

    public BigDecimal priceUsdForSimulation(BigDecimal clientHintUsd) {
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
            EthPriceQuote quote = new EthPriceQuote(chainlink.get(), EthPriceSource.CHAINLINK, observedAt, false, false);
            cache.put(quote, ttl);
            return quote;
        }

        Optional<BigDecimal> publicApi = publicApiClient.fetchPriceUsd(resolvePublicPriceApiUrl());
        if (publicApi.isPresent()) {
            EthPriceQuote quote = new EthPriceQuote(publicApi.get(), EthPriceSource.PUBLIC_API, observedAt, false, false);
            cache.put(quote, ttl);
            return quote;
        }

        EthPriceQuote quote = new EthPriceQuote(
                properties.getStaticEthPriceUsd(),
                EthPriceSource.STATIC,
                observedAt,
                false,
                true
        );
        cache.put(quote, ttl);
        return quote;
    }

    private String resolvePublicPriceApiUrl() {
        String url = properties.getPublicPriceApiUrl();
        if (!StringUtils.hasText(url)) {
            url = environment.getProperty("PUBLIC_PRICE_API_URL");
        }
        return StringUtils.hasText(url) ? url.trim() : "";
    }
}