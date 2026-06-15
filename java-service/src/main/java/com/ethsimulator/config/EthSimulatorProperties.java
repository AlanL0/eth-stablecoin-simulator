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
    private String chainlinkEthUsdFeed = "0x5f4eC3Df9cbd43714FE2740f5E3616155c5B8419";
    private int priceCacheTtlSeconds = 60;
    /** Max blocks to scan per token for wallet audit (mainnet ~2s/block). Default ~7 days. */
    private int auditLookbackBlocks = 300_000;
    private int auditMaxEventsPerWallet = 500;
    private int auditCacheMaxAddresses = 100;
    private int auditCacheTtlSeconds = 900;
    private int httpConnectTimeoutMs = 5_000;
    private int httpReadTimeoutMs = 10_000;
    private int rpcConnectTimeoutMs = 5_000;
    private int rpcReadTimeoutMs = 30_000;

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

    public int getAuditLookbackBlocks() {
        return auditLookbackBlocks;
    }

    public void setAuditLookbackBlocks(int auditLookbackBlocks) {
        this.auditLookbackBlocks = auditLookbackBlocks;
    }

    public int getAuditMaxEventsPerWallet() {
        return auditMaxEventsPerWallet;
    }

    public void setAuditMaxEventsPerWallet(int auditMaxEventsPerWallet) {
        this.auditMaxEventsPerWallet = auditMaxEventsPerWallet;
    }

    public int getAuditCacheMaxAddresses() {
        return auditCacheMaxAddresses;
    }

    public void setAuditCacheMaxAddresses(int auditCacheMaxAddresses) {
        this.auditCacheMaxAddresses = auditCacheMaxAddresses;
    }

    public int getAuditCacheTtlSeconds() {
        return auditCacheTtlSeconds;
    }

    public void setAuditCacheTtlSeconds(int auditCacheTtlSeconds) {
        this.auditCacheTtlSeconds = auditCacheTtlSeconds;
    }

    public int getHttpConnectTimeoutMs() {
        return httpConnectTimeoutMs;
    }

    public void setHttpConnectTimeoutMs(int httpConnectTimeoutMs) {
        this.httpConnectTimeoutMs = httpConnectTimeoutMs;
    }

    public int getHttpReadTimeoutMs() {
        return httpReadTimeoutMs;
    }

    public void setHttpReadTimeoutMs(int httpReadTimeoutMs) {
        this.httpReadTimeoutMs = httpReadTimeoutMs;
    }

    public int getRpcConnectTimeoutMs() {
        return rpcConnectTimeoutMs;
    }

    public void setRpcConnectTimeoutMs(int rpcConnectTimeoutMs) {
        this.rpcConnectTimeoutMs = rpcConnectTimeoutMs;
    }

    public int getRpcReadTimeoutMs() {
        return rpcReadTimeoutMs;
    }

    public void setRpcReadTimeoutMs(int rpcReadTimeoutMs) {
        this.rpcReadTimeoutMs = rpcReadTimeoutMs;
    }
}