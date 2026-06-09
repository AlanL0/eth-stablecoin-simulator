package com.ethsimulator.treasury;

import java.util.Map;

public record TreasuryContext(
        String disclaimer,
        double yourMintUsd,
        Map<String, Object> assumptions,
        MintContext yourMint,
        SystemContext systemContext,
        PersonalComparison personalComparison
) {
    public record MintContext(
            double impliedTreasuryBackingUsd,
            double annualIssuerReserveYieldUsd,
            double projectedIssuerReserveYieldUsd
    ) {
    }

    public record SystemContext(
            double impliedTreasuryBackingUsd,
            double annualIssuerReserveYieldUsd,
            double treasuryDemandProxyUsd
    ) {
    }

    public record PersonalComparison(double yourDeFiProjectedNetYieldUsd, String note) {
    }
}