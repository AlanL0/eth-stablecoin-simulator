package com.ethsimulator.protocol;

import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;

/**
 * Exact annualized value with an explicit convention and derivation note.
 */
public record AnnualizedRate(
        BigDecimal value,
        RateConvention convention,
        String methodology
) {
    public AnnualizedRate {
        if (value != null) {
            value = convention == RateConvention.SPOT_USD
                    ? FinancialMath.scaleUsd(value)
                    : FinancialMath.scaleRate(value);
        }
        if (convention == null) {
            throw new IllegalArgumentException("convention is required");
        }
        if (methodology == null || methodology.isBlank()) {
            throw new IllegalArgumentException("methodology is required");
        }
    }

    public static AnnualizedRate of(BigDecimal value, RateConvention convention, String methodology) {
        return new AnnualizedRate(value, convention, methodology);
    }
}