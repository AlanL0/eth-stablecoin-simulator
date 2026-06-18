package com.ethsimulator.ingestion;

import com.ethsimulator.protocol.rpc.EthCallClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.web3j.protocol.Web3j;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(IngestionProperties.class)
@ConditionalOnExpression(
        "T(org.springframework.util.StringUtils).hasText('${DATABASE_URL:}')"
                + " && T(com.ethsimulator.config.BlockchainConfig).hasHttpRpcUrl(@environment)"
)
public class IngestionConfig {

    @Bean
    public TransactionTemplate ingestionTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public FinalizedBlockReader finalizedBlockReader(ObjectProvider<Web3j> web3jProvider) {
        Web3j web3j = web3jProvider.getIfAvailable();
        if (web3j == null) {
            throw new IllegalStateException("Web3j is required when ingestion is enabled");
        }
        return new Web3jFinalizedBlockReader(new EthCallClient(web3j));
    }

    @Bean
    public IngestionReorgHandler ingestionReorgHandler(
            IngestionProperties properties,
            ObjectProvider<FinalizedBlockReader> blockReaderProvider,
            IngestionPersistenceService persistenceService
    ) {
        FinalizedBlockReader blockReader = blockReaderProvider.getIfAvailable();
        if (blockReader == null) {
            blockReader = new FinalizedBlockReader() {
                @Override
                public long latestBlockNumber() {
                    throw new IllegalStateException("FinalizedBlockReader unavailable");
                }

                @Override
                public com.ethsimulator.protocol.rpc.EthBlockHeader blockAt(long blockNumber) {
                    throw new IllegalStateException("FinalizedBlockReader unavailable");
                }
            };
        }
        return new IngestionReorgHandler(properties, blockReader, persistenceService);
    }

}