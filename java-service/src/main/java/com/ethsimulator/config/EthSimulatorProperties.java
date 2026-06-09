package com.ethsimulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "eth-simulator")
public class EthSimulatorProperties {

    private BigDecimal staticEthPriceUsd = new BigDecimal("3800");
    private String allowedOrigins = "http://localhost:3000";
    private BigDecimal defaultSystemSupplyUsd = new BigDecimal("32000000000");

    public BigDecimal getStaticEthPriceUsd() {
        return staticEthPriceUsd;
    }

    public void setStaticEthPriceUsd(BigDecimal staticEthPriceUsd) {
        this.staticEthPriceUsd = staticEthPriceUsd;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public BigDecimal getDefaultSystemSupplyUsd() {
        return defaultSystemSupplyUsd;
    }

    public void setDefaultSystemSupplyUsd(BigDecimal defaultSystemSupplyUsd) {
        this.defaultSystemSupplyUsd = defaultSystemSupplyUsd;
    }
}