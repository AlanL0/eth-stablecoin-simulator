package com.ethsimulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "eth-simulator.agent")
public class AgentAiProperties {

    private boolean enabled = true;
    private int maxTurns = 3;
    private int connectTimeoutMs = 5_000;
    private int readTimeoutMs = 12_000;
    private Map<String, List<String>> allowedBaseUrlPrefixes = defaultAllowlist();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Map<String, List<String>> getAllowedBaseUrlPrefixes() {
        return allowedBaseUrlPrefixes;
    }

    public void setAllowedBaseUrlPrefixes(Map<String, List<String>> allowedBaseUrlPrefixes) {
        this.allowedBaseUrlPrefixes = allowedBaseUrlPrefixes;
    }

    private static Map<String, List<String>> defaultAllowlist() {
        Map<String, List<String>> defaults = new LinkedHashMap<>();
        defaults.put("deepseek", List.of("https://api.deepseek.com"));
        defaults.put("nvidia", List.of("https://integrate.api.nvidia.com"));
        defaults.put("openrouter", List.of("https://openrouter.ai/api"));
        defaults.put("ollama", List.of("http://localhost:11434", "http://127.0.0.1:11434"));
        return defaults;
    }

    public List<String> prefixesForProvider(String provider) {
        return allowedBaseUrlPrefixes.getOrDefault(provider, List.of());
    }
}