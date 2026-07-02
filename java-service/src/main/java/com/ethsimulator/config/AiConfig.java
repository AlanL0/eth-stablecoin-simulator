package com.ethsimulator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class AiConfig {

    public static boolean hasLlmCredentials(Environment environment) {
        if (StringUtils.hasText(environment.getProperty("spring.ai.openai.api-key"))) {
            return true;
        }
        if (StringUtils.hasText(environment.getProperty("LLM_API_KEY"))) {
            return true;
        }
        if (StringUtils.hasText(environment.getProperty("DEEPSEEK_API_KEY"))) {
            return true;
        }
        if (StringUtils.hasText(environment.getProperty("NVIDIA_API_KEY"))) {
            return true;
        }
        if (StringUtils.hasText(environment.getProperty("OPENROUTER_API_KEY"))) {
            return true;
        }
        String provider = AgentProviderSupport.resolveProvider(environment);
        return "ollama".equals(provider);
    }
}