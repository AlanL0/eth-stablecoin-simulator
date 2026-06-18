package com.ethsimulator.health;

import com.ethsimulator.config.AiConfig;
import com.ethsimulator.config.BlockchainConfig;
import com.ethsimulator.config.UnavailableChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.websocket.WebSocketService;

import javax.sql.DataSource;

@Configuration
public class DependencyHealthIndicators {

    @Bean
    public HealthIndicator ethRpcHealth(
            Environment environment,
            ObjectProvider<Web3j> web3jProvider,
            ObjectProvider<WebSocketService> webSocketProvider
    ) {
        return () -> {
            String rpcUrl = BlockchainConfig.resolveRpcUrl(environment);
            if (!StringUtils.hasText(rpcUrl)) {
                return Health.down().withDetail("reason", "ETH_RPC_URL not configured").build();
            }
            boolean httpReady = web3jProvider.getIfAvailable() != null;
            boolean wsReady = webSocketProvider.getIfAvailable() != null;
            if (httpReady || wsReady) {
                return Health.up()
                        .withDetail("transport", httpReady ? "http" : "websocket")
                        .build();
            }
            return Health.down().withDetail("reason", "RPC URL present but Web3j client not initialized").build();
        };
    }

    @Bean
    public HealthIndicator databaseHealth(ObjectProvider<DataSource> dataSourceProvider) {
        return () -> {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            if (dataSource == null) {
                return Health.down().withDetail("reason", "DATABASE_URL not configured").build();
            }
            return Health.up().withDetail("status", "configured").build();
        };
    }

    @Bean
    public HealthIndicator llmHealth(Environment environment, ObjectProvider<ChatModel> chatModelProvider) {
        return () -> {
            ChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null || chatModel instanceof UnavailableChatModel || !AiConfig.hasLlmCredentials(environment)) {
                return Health.down().withDetail("reason", "LLM credentials not configured").build();
            }
            return Health.up().withDetail("status", "configured").build();
        };
    }
}