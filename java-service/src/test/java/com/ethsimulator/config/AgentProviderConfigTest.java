package com.ethsimulator.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentProviderConfigTest {

    @Test
    void deepseekBaseUrlMustMatchAllowlist() {
        AgentAiProperties properties = new AgentAiProperties();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("LLM_PROVIDER", "deepseek")
                .withProperty("LLM_BASE_URL", "https://api.deepseek.com/v1");

        String baseUrl = AgentProviderSupport.resolveBaseUrl(environment, "deepseek");

        AgentProviderSupport.validateBaseUrl("deepseek", baseUrl, properties);
        assertThat(baseUrl).isEqualTo("https://api.deepseek.com/v1");
    }

    @Test
    void rejectsDisallowedProviderHost() {
        AgentAiProperties properties = new AgentAiProperties();

        assertThatThrownBy(() -> AgentProviderSupport.validateBaseUrl(
                "deepseek",
                "https://evil.example.com/v1",
                properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not allowlisted");
    }

    @Test
    void ollamaDefaultsToLocalhost() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("LLM_PROVIDER", "ollama");

        String baseUrl = AgentProviderSupport.resolveBaseUrl(environment, "ollama");

        assertThat(baseUrl).isEqualTo("http://localhost:11434");
        AgentProviderSupport.validateBaseUrl("ollama", baseUrl, new AgentAiProperties());
    }

    @Test
    void nvidiaProviderUsesIntegrateApiHost() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("LLM_PROVIDER", "nvidia");

        String baseUrl = AgentProviderSupport.resolveBaseUrl(environment, "nvidia");

        assertThat(baseUrl).startsWith("https://integrate.api.nvidia.com");
        AgentProviderSupport.validateBaseUrl("nvidia", baseUrl, new AgentAiProperties());
    }
}