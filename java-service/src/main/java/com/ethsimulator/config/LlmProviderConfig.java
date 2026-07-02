package com.ethsimulator.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class LlmProviderConfig {

    @Bean
    @Primary
    public ChatModel agentChatModel(
            Environment environment,
            AgentAiProperties agentAiProperties,
            ObjectProvider<ChatModel> chatModelProvider
    ) {
        if (!AiConfig.hasLlmCredentials(environment)) {
            return new UnavailableChatModel();
        }
        String provider = AgentProviderSupport.resolveProvider(environment);
        String baseUrl = AgentProviderSupport.resolveBaseUrl(environment, provider);
        AgentProviderSupport.validateBaseUrl(provider, baseUrl, agentAiProperties);

        return chatModelProvider.orderedStream()
                .filter(model -> !(model instanceof UnavailableChatModel))
                .findFirst()
                .orElseGet(UnavailableChatModel::new);
    }

}