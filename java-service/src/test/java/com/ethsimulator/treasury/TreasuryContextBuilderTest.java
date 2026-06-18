package com.ethsimulator.treasury;

import com.ethsimulator.config.EthSimulatorProperties;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreasuryContextBuilderTest {

    @Test
    void usdcStyleMatchesFixture() {
        EthSimulatorProperties properties = new EthSimulatorProperties();
        TreasuryContextBuilder builder = new TreasuryContextBuilder(properties);

        TreasuryContext context = builder.build(
                FinancialMath.bd("4222.22"),
                FinancialMath.bd("4.91"),
                StablecoinReserveModel.USDC_STYLE,
                null,
                null,
                null,
                1
        );

        assertEquals(FinancialMath.bd("4222.22"), context.yourMintUsd());
        assertEquals(FinancialMath.bd("3800.00"), context.yourMint().impliedTreasuryBackingUsd());
        assertEquals(FinancialMath.bd("171.00"), context.yourMint().annualIssuerReserveYieldUsd());
        assertEquals(FinancialMath.bd("4.91"), context.personalComparison().yourDeFiProjectedNetYieldUsd());
        assertEquals("usdc_style", context.assumptions().get("stablecoinReserveModel"));
    }
}