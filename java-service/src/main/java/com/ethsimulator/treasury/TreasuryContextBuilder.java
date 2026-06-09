package com.ethsimulator.treasury;

import com.ethsimulator.config.EthSimulatorProperties;
import com.ethsimulator.util.UsdMath;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TreasuryContextBuilder {

    private static final String DISCLAIMER =
            "Educational model only. Issuers earn reserve yield; Treasury benefits from debt demand, not issuer profits.";

    private final EthSimulatorProperties properties;

    public TreasuryContextBuilder(EthSimulatorProperties properties) {
        this.properties = properties;
    }

    public TreasuryContext build(
            BigDecimal mintedUsd,
            BigDecimal projectedNetYieldUsd,
            StablecoinReserveModel model,
            BigDecimal reserveOverride,
            BigDecimal tbillOverride,
            BigDecimal systemSupplyUsd,
            int years
    ) {
        BigDecimal reservePct = reserveOverride != null ? reserveOverride : model.reserveInTreasuriesPct();
        BigDecimal tbillApy = tbillOverride != null ? tbillOverride : model.tbillApyPct();
        BigDecimal backingRatio = BigDecimal.ONE;
        BigDecimal systemSupply = systemSupplyUsd != null ? systemSupplyUsd : properties.getDefaultSystemSupplyUsd();

        BigDecimal mintBacking = backing(mintedUsd, reservePct, backingRatio);
        BigDecimal mintAnnualYield = yieldOn(mintBacking, tbillApy);

        BigDecimal systemBacking = backing(systemSupply, reservePct, backingRatio);
        BigDecimal systemAnnualYield = yieldOn(systemBacking, tbillApy);

        Map<String, Object> assumptions = new LinkedHashMap<>();
        assumptions.put("stablecoinReserveModel", model.key());
        assumptions.put("reserveInTreasuriesPct", reservePct.doubleValue());
        assumptions.put("tbillApyPct", tbillApy.doubleValue());
        assumptions.put("backingRatio", 1.0);
        assumptions.put("years", years);
        assumptions.put("systemSupplyUsd", systemSupply.doubleValue());
        assumptions.put("systemSupplySource", "static_seed");

        return new TreasuryContext(
                DISCLAIMER,
                UsdMath.roundUsdDouble(mintedUsd),
                assumptions,
                new TreasuryContext.MintContext(
                        UsdMath.roundUsdDouble(mintBacking),
                        UsdMath.roundUsdDouble(mintAnnualYield),
                        UsdMath.roundUsdDouble(mintAnnualYield.multiply(BigDecimal.valueOf(years)))
                ),
                new TreasuryContext.SystemContext(
                        UsdMath.roundUsdDouble(systemBacking),
                        UsdMath.roundUsdDouble(systemAnnualYield),
                        UsdMath.roundUsdDouble(systemBacking)
                ),
                new TreasuryContext.PersonalComparison(
                        UsdMath.roundUsdDouble(projectedNetYieldUsd),
                        "Your DeFi deploy yield vs illustrative issuer reserve yield on the same notional — different economics."
                )
        );
    }

    private static BigDecimal backing(BigDecimal amount, BigDecimal reservePct, BigDecimal backingRatio) {
        return amount.multiply(UsdMath.percentToRate(reservePct)).multiply(backingRatio);
    }

    private static BigDecimal yieldOn(BigDecimal backing, BigDecimal tbillApy) {
        return backing.multiply(UsdMath.percentToRate(tbillApy));
    }
}