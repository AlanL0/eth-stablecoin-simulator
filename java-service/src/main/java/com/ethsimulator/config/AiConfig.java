package com.ethsimulator.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel unavailableChatModel() {
        return new UnavailableChatModel();
    }

    public static boolean hasLlmCredentials(Environment environment) {
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (!StringUtils.hasText(apiKey)) {
            apiKey = environment.getProperty("LLM_API_KEY");
        }
        return StringUtils.hasText(apiKey);
    }
}