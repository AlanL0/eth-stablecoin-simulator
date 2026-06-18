package com.ethsimulator.market;

import com.ethsimulator.api.error.ApiException;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.util.FinancialMath;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class YieldService {

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

    public YieldService(Clock clock) {
        this.clock = clock;
    }

    public YieldSnapshotResponse getYields(String asset) {
        String normalized = asset == null ? "" : asset.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ApiException("INVALID_YIELD_INPUT", "asset query parameter is required", HttpStatus.BAD_REQUEST);
        }

        Instant observedAt = clock.instant();
        List<SeedYield> seeds = SEED.get(normalized);
        if (seeds != null) {
            return new YieldSnapshotResponse(normalized, seeds.stream()
                    .map(seed -> new YieldQuote(
                            seed.protocol(),
                            seed.apyPct(),
                            seed.convention(),
                            "seed",
                            seed.riskTier(),
                            observedAt
                    ))
                    .toList());
        }

        return new YieldSnapshotResponse(normalized, List.of(
                new YieldQuote(
                        "static_conservative",
                        FinancialMath.bd("3.0"),
                        RateConvention.APR_EFFECTIVE,
                        "static_fallback",
                        RiskTier.LOW,
                        observedAt
                )
        ));
    }

    public Map<String, List<YieldQuote>> seedCatalog() {
        Map<String, List<YieldQuote>> catalog = new LinkedHashMap<>();
        Instant observedAt = clock.instant();
        SEED.forEach((asset, yields) -> catalog.put(asset, yields.stream()
                .map(seed -> new YieldQuote(
                        seed.protocol(),
                        seed.apyPct(),
                        seed.convention(),
                        "seed",
                        seed.riskTier(),
                        observedAt))
                .toList()));
        return catalog;
    }

    private record SeedYield(String protocol, BigDecimal apyPct, RateConvention convention, RiskTier riskTier) {
    }
}