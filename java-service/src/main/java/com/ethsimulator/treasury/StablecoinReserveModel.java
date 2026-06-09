package com.ethsimulator.treasury;

import java.math.BigDecimal;

public enum StablecoinReserveModel {
    USDC_STYLE("usdc_style", new BigDecimal("90"), new BigDecimal("4.5")),
    USDT_STYLE("usdt_style", new BigDecimal("85"), new BigDecimal("4.5")),
    GENERIC("generic", new BigDecimal("80"), new BigDecimal("4.5"));

    private final String key;
    private final BigDecimal reserveInTreasuriesPct;
    private final BigDecimal tbillApyPct;

    StablecoinReserveModel(String key, BigDecimal reserveInTreasuriesPct, BigDecimal tbillApyPct) {
        this.key = key;
        this.reserveInTreasuriesPct = reserveInTreasuriesPct;
        this.tbillApyPct = tbillApyPct;
    }

    public String key() {
        return key;
    }

    public BigDecimal reserveInTreasuriesPct() {
        return reserveInTreasuriesPct;
    }

    public BigDecimal tbillApyPct() {
        return tbillApyPct;
    }

    public static StablecoinReserveModel fromKey(String key) {
        for (StablecoinReserveModel model : values()) {
            if (model.key.equalsIgnoreCase(key)) {
                return model;
            }
        }
        throw new IllegalArgumentException("Unsupported stablecoin reserve model: " + key);
    }
}