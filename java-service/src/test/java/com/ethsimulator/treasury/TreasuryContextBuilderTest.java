package com.ethsimulator.treasury;

import com.ethsimulator.config.EthSimulatorProperties;
import com.ethsimulator.util.UsdMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreasuryContextBuilderTest {

    @Test
    void usdcStyleMatchesFixture() {
        EthSimulatorProperties properties = new EthSimulatorProperties();
        TreasuryContextBuilder builder = new TreasuryContextBuilder(properties);

        TreasuryContext context = builder.build(
                UsdMath.bd("4222.22"),
                UsdMath.bd("4.91"),
                StablecoinReserveModel.USDC_STYLE,
                null,
                null,
                null,
                1
        );

        assertEquals(4222.22, context.yourMintUsd(), 0.01);
        assertEquals(3800.0, context.yourMint().impliedTreasuryBackingUsd(), 0.01);
        assertEquals(171.0, context.yourMint().annualIssuerReserveYieldUsd(), 0.01);
        assertEquals(4.91, context.personalComparison().yourDeFiProjectedNetYieldUsd(), 0.02);
        assertEquals("usdc_style", context.assumptions().get("stablecoinReserveModel"));
    }
}