package com.ethsimulator.ingestion;

import com.ethsimulator.protocol.rpc.EthCallClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(IngestionProperties.class)
@ConditionalOnBean(DataSource.class)
public class IngestionConfig {

    @Bean
    public TransactionTemplate ingestionTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    @ConditionalOnBean(EthCallClient.class)
    public FinalizedBlockReader finalizedBlockReader(EthCallClient ethCallClient) {
        return new Web3jFinalizedBlockReader(ethCallClient);
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