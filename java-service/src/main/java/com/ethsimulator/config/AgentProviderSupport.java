package com.ethsimulator.config;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public final class AgentProviderSupport {

    private AgentProviderSupport() {
    }

    public static String resolveProvider(Environment environment) {
        String provider = environment.getProperty("LLM_PROVIDER");
        if (!StringUtils.hasText(provider)) {
            provider = environment.getProperty("eth-simulator.agent.default-provider", "deepseek");
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    public static String resolveBaseUrl(Environment environment, String provider) {
        String baseUrl = switch (provider) {
            case "deepseek" -> firstNonBlank(
                    environment.getProperty("LLM_BASE_URL"),
                    "https://api.deepseek.com");
            case "nvidia" -> firstNonBlank(
                    environment.getProperty("LLM_BASE_URL"),
                    environment.getProperty("NVIDIA_BASE_URL"),
                    "https://integrate.api.nvidia.com/v1");
            case "openrouter" -> firstNonBlank(
                    environment.getProperty("LLM_BASE_URL"),
                    environment.getProperty("OPENROUTER_BASE_URL"),
                    "https://openrouter.ai/api/v1");
            case "ollama" -> firstNonBlank(
                    environment.getProperty("OLLAMA_BASE_URL"),
                    environment.getProperty("spring.ai.ollama.base-url"),
                    "http://localhost:11434");
            default -> environment.getProperty("LLM_BASE_URL", "https://api.openai.com");
        };
        return normalizeBaseUrl(baseUrl);
    }

    public static void validateBaseUrl(String provider, String baseUrl, AgentAiProperties properties) {
        List<String> allowedPrefixes = properties.prefixesForProvider(provider);
        if (allowedPrefixes.isEmpty()) {
            return;
        }
        String normalized = normalizeBaseUrl(baseUrl);
        boolean allowed = allowedPrefixes.stream().anyMatch(normalized::startsWith);
        if (!allowed) {
            throw new IllegalStateException(
                    "LLM base URL '%s' is not allowlisted for provider '%s'".formatted(normalized, provider));
        }
        URI uri = URI.create(normalized);
        if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
            throw new IllegalStateException("LLM base URL must include scheme and host: " + normalized);
        }
    }

    public static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return "";
    }
}