package com.ethsimulator.simulation;

import java.math.BigDecimal;

public record ProtocolPreset(
        String name,
        String displayName,
        BigDecimal collateralRatio,
        BigDecimal liquidationRatio,
        BigDecimal stabilityFeePct
) {
}