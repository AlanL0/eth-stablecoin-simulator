package com.ethsimulator.treasury;

import java.math.BigDecimal;
import java.util.Map;

public record TreasuryContext(
        String disclaimer,
        BigDecimal yourMintUsd,
        Map<String, Object> assumptions,
        MintContext yourMint,
        SystemContext systemContext,
        PersonalComparison personalComparison
) {
    public record MintContext(
            BigDecimal impliedTreasuryBackingUsd,
            BigDecimal annualIssuerReserveYieldUsd,
            BigDecimal projectedIssuerReserveYieldUsd
    ) {
    }

    public record SystemContext(
            BigDecimal impliedTreasuryBackingUsd,
            BigDecimal annualIssuerReserveYieldUsd,
            BigDecimal treasuryDemandProxyUsd
    ) {
    }

    public record PersonalComparison(BigDecimal yourDeFiProjectedNetYieldUsd, String note) {
    }
}