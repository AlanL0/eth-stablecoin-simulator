package com.ethsimulator.protocol;

/**
 * Explicit annualization convention for a protocol-sourced value.
 */
public enum RateConvention {
    /** Linear annualization: rate_per_period × periods_per_year. */
    APR_SIMPLE,
    /** Compounded periodic annualization: (1 + r)^n − 1. */
    APR_EFFECTIVE,
    /** Spot USD price from an oracle feed (not a borrow/savings rate). */
    SPOT_USD
}