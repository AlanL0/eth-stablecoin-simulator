package com.ethsimulator.market;

import com.ethsimulator.api.error.ApiException;
import com.ethsimulator.config.EthSimulatorProperties;
import com.ethsimulator.persistence.RateObservation;
import com.ethsimulator.persistence.RateReadModelRepository;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.util.FinancialMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class YieldService {

    private static final Logger log = LoggerFactory.getLogger(YieldService.class);
    private static final Instant SEED_OBSERVED_AT = Instant.EPOCH;

    private static final Map<String, List<String>> ASSET_PRODUCTS = Map.of(
            "USDC", List.of("USDC", "GHO", "sGHO"),
            "USDT", List.of("USDT"),
            "DAI", List.of("DAI", "sUSDS"),
            "PYUSD", List.of("PYUSD")
    );

    private static final Map<String, List<SeedYield>> SEED = Map.of(
            "USDC", List.of(
                    new SeedYield("aave", FinancialMath.bd("4.2"), RateConvention.APR_EFFECTIVE, RiskTier.LOW),
                    new SeedYield("compound", FinancialMath.bd("3.8"), RateConvention.APR_EFFECTIVE, RiskTier.LOW),
                    new SeedYield("maker_dsr", FinancialMath.bd("5.0"), RateConvention.APR_EFFECTIVE, RiskTier.MEDIUM)
            ),
            "USDT", List.of(
                    new SeedYield("aave", FinancialMath.bd("3.9"), RateConvention.APR_EFFECTIVE, RiskTier.LOW),
                    new SeedYield("curve", FinancialMath.bd("4.1"), RateConvention.APR_EFFECTIVE, RiskTier.MEDIUM)
            ),
            "DAI", List.of(
                    new SeedYield("spark", FinancialMath.bd("4.5"), RateConvention.APR_EFFECTIVE, RiskTier.MEDIUM),
                    new SeedYield("maker_dsr", FinancialMath.bd("5.0"), RateConvention.APR_EFFECTIVE, RiskTier.MEDIUM)
            ),
            "PYUSD", List.of(
                    new SeedYield("aave", FinancialMath.bd("3.5"), RateConvention.APR_EFFECTIVE, RiskTier.LOW)
            )
    );

    private final Clock clock;
    private final ObjectProvider<RateReadModelRepository> rateReadModelRepository;
    private final EthSimulatorProperties properties;

    public YieldService(
            Clock clock,
            ObjectProvider<RateReadModelRepository> rateReadModelRepository,
            EthSimulatorProperties properties
    ) {
        this.clock = clock;
        this.rateReadModelRepository = rateReadModelRepository;
        this.properties = properties;
    }

    public YieldSnapshotResponse getYields(String asset) {
        String normalized = asset == null ? "" : asset.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ApiException("INVALID_YIELD_INPUT", "asset query parameter is required", HttpStatus.BAD_REQUEST);
        }

        List<YieldQuote> liveQuotes = loadLiveQuotes(normalized);
        if (!liveQuotes.isEmpty()) {
            return new YieldSnapshotResponse(normalized, liveQuotes, "live");
        }

        List<SeedYield> seeds = SEED.get(normalized);
        if (seeds != null) {
            return new YieldSnapshotResponse(normalized, seedQuotes(seeds), "seed_fallback");
        }

        return new YieldSnapshotResponse(normalized, List.of(
                new YieldQuote(
                        "static_conservative",
                        FinancialMath.bd("3.0"),
                        RateConvention.APR_EFFECTIVE,
                        "static_fallback",
                        RiskTier.LOW,
                        SEED_OBSERVED_AT,
                        true
                )
        ), "seed_fallback");
    }

    public Map<String, List<YieldQuote>> seedCatalog() {
        Map<String, List<YieldQuote>> catalog = new LinkedHashMap<>();
        SEED.forEach((asset, yields) -> catalog.put(asset, seedQuotes(yields)));
        return catalog;
    }

    private List<YieldQuote> loadLiveQuotes(String asset) {
        RateReadModelRepository repository = rateReadModelRepository.getIfAvailable();
        if (repository == null) {
            return List.of();
        }

        List<String> products = ASSET_PRODUCTS.getOrDefault(asset, List.of(asset));
        List<RateObservation> observations;
        try {
            observations = repository.findLatestByProducts(products);
        } catch (RuntimeException ex) {
            log.warn("Yield read model unavailable for asset={}: {}", asset, ex.getMessage());
            return List.of();
        }
        if (observations.isEmpty()) {
            return List.of();
        }

        Instant now = clock.instant();
        Duration staleThreshold = Duration.ofSeconds(Math.max(1, properties.getYieldStaleThresholdSeconds()));
        Instant newest = observations.stream()
                .map(RateObservation::observedAt)
                .max(Instant::compareTo)
                .orElse(now);
        boolean stale = newest.isBefore(now.minus(staleThreshold));

        return observations.stream()
                .map(observation -> toLiveQuote(observation, stale))
                .toList();
    }

    private YieldQuote toLiveQuote(RateObservation observation, boolean stale) {
        String protocolKey = observation.protocol();
        if ("sky".equalsIgnoreCase(protocolKey)) {
            protocolKey = "maker_dsr";
        }
        return new YieldQuote(
                protocolKey,
                FinancialMath.rateToHumanPercent(observation.annualizedValue()),
                parseConvention(observation.convention()),
                observation.protocol(),
                riskTierFor(observation.protocol()),
                observation.observedAt(),
                stale
        );
    }

    private List<YieldQuote> seedQuotes(List<SeedYield> seeds) {
        return seeds.stream()
                .map(seed -> new YieldQuote(
                        seed.protocol(),
                        seed.apyPct(),
                        seed.convention(),
                        "seed",
                        seed.riskTier(),
                        SEED_OBSERVED_AT,
                        true
                ))
                .toList();
    }

    private static RateConvention parseConvention(String convention) {
        if (convention == null || convention.isBlank()) {
            return RateConvention.APR_EFFECTIVE;
        }
        try {
            return RateConvention.valueOf(convention);
        } catch (IllegalArgumentException ignored) {
            return switch (convention.toUpperCase(Locale.ROOT)) {
                case "APR" -> RateConvention.APR_SIMPLE;
                default -> RateConvention.APR_EFFECTIVE;
            };
        }
    }

    private static RiskTier riskTierFor(String protocol) {
        if (protocol == null) {
            return RiskTier.MEDIUM;
        }
        return switch (protocol.toLowerCase(Locale.ROOT)) {
            case "aave" -> RiskTier.LOW;
            case "sky", "maker_dsr", "liquity", "curve", "spark" -> RiskTier.MEDIUM;
            default -> RiskTier.MEDIUM;
        };
    }

    private record SeedYield(String protocol, BigDecimal apyPct, RateConvention convention, RiskTier riskTier) {
    }
}