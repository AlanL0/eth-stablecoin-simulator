package com.ethsimulator.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eth-simulator.ingestion")
public class IngestionProperties {

    private boolean enabled = true;
    private int confirmationDepth = 12;
    private int rangeSize = 10;
    private long pollIntervalMs = 30_000;
    private long requestTimeoutMs = 30_000;
    private int maxRetries = 3;
    private long retryBackoffMs = 1_000;
    private long maxCatchUpBlocks = 1_000;
    private long maxSourceLagBlocks = 100;
    private int maxReorgWindowBlocks = 64;
    private int leaseDurationSeconds = 30;
    private boolean websocketWakeEnabled = true;
    private Long startBlock;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConfirmationDepth() {
        return confirmationDepth;
    }

    public void setConfirmationDepth(int confirmationDepth) {
        this.confirmationDepth = confirmationDepth;
    }

    public int getRangeSize() {
        return rangeSize;
    }

    public void setRangeSize(int rangeSize) {
        this.rangeSize = rangeSize;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public long getMaxCatchUpBlocks() {
        return maxCatchUpBlocks;
    }

    public void setMaxCatchUpBlocks(long maxCatchUpBlocks) {
        this.maxCatchUpBlocks = maxCatchUpBlocks;
    }

    public long getMaxSourceLagBlocks() {
        return maxSourceLagBlocks;
    }

    public void setMaxSourceLagBlocks(long maxSourceLagBlocks) {
        this.maxSourceLagBlocks = maxSourceLagBlocks;
    }

    public int getMaxReorgWindowBlocks() {
        return maxReorgWindowBlocks;
    }

    public void setMaxReorgWindowBlocks(int maxReorgWindowBlocks) {
        this.maxReorgWindowBlocks = maxReorgWindowBlocks;
    }

    public int getLeaseDurationSeconds() {
        return leaseDurationSeconds;
    }

    public void setLeaseDurationSeconds(int leaseDurationSeconds) {
        this.leaseDurationSeconds = leaseDurationSeconds;
    }

    public boolean isWebsocketWakeEnabled() {
        return websocketWakeEnabled;
    }

    public void setWebsocketWakeEnabled(boolean websocketWakeEnabled) {
        this.websocketWakeEnabled = websocketWakeEnabled;
    }

    public Long getStartBlock() {
        return startBlock;
    }

    public void setStartBlock(Long startBlock) {
        this.startBlock = startBlock;
    }
}