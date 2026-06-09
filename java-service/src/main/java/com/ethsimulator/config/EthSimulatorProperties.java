package com.ethsimulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "eth-simulator")
public class EthSimulatorProperties {

    private BigDecimal staticEthPriceUsd = new BigDecimal("3800");
    private String allowedOrigins = "http://localhost:3000";
    private BigDecimal defaultSystemSupplyUsd = new BigDecimal("32000000000");
    private String ethRpcUrl = "";
    private String publicPriceApiUrl = "";
    private String chainlinkEthUsdFeed = "0x5f4eC3Df9cbd43714FE2740f5E2617432640a174";
    private int priceCacheTtlSeconds = 60;

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

    public String getEthRpcUrl() {
        return ethRpcUrl;
    }

    public void setEthRpcUrl(String ethRpcUrl) {
        this.ethRpcUrl = ethRpcUrl;
    }

    public String getPublicPriceApiUrl() {
        return publicPriceApiUrl;
    }

    public void setPublicPriceApiUrl(String publicPriceApiUrl) {
        this.publicPriceApiUrl = publicPriceApiUrl;
    }

    public String getChainlinkEthUsdFeed() {
        return chainlinkEthUsdFeed;
    }

    public void setChainlinkEthUsdFeed(String chainlinkEthUsdFeed) {
        this.chainlinkEthUsdFeed = chainlinkEthUsdFeed;
    }

    public int getPriceCacheTtlSeconds() {
        return priceCacheTtlSeconds;
    }

    public void setPriceCacheTtlSeconds(int priceCacheTtlSeconds) {
        this.priceCacheTtlSeconds = priceCacheTtlSeconds;
    }
}