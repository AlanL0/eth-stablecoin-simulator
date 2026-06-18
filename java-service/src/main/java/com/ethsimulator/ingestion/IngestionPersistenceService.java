package com.ethsimulator.ingestion;

import com.ethsimulator.persistence.IngestionCursor;
import com.ethsimulator.persistence.IngestionCursorRepository;
import com.ethsimulator.persistence.PriceObservation;
import com.ethsimulator.persistence.PriceObservationRepository;
import com.ethsimulator.persistence.RateObservation;
import com.ethsimulator.persistence.RateObservationRepository;
import com.ethsimulator.persistence.SourceHealth;
import com.ethsimulator.persistence.SourceHealthRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnBean(DataSource.class)
public class IngestionPersistenceService {

    private final JdbcClient jdbcClient;
    private final TransactionTemplate transactionTemplate;
    private final IngestionCursorRepository cursorRepository;
    private final PriceObservationRepository priceObservationRepository;
    private final RateObservationRepository rateObservationRepository;
    private final SourceHealthRepository sourceHealthRepository;

    public IngestionPersistenceService(
            JdbcClient jdbcClient,
            TransactionTemplate transactionTemplate,
            IngestionCursorRepository cursorRepository,
            PriceObservationRepository priceObservationRepository,
            RateObservationRepository rateObservationRepository,
            SourceHealthRepository sourceHealthRepository
    ) {
        this.jdbcClient = jdbcClient;
        this.transactionTemplate = transactionTemplate;
        this.cursorRepository = cursorRepository;
        this.priceObservationRepository = priceObservationRepository;
        this.rateObservationRepository = rateObservationRepository;
        this.sourceHealthRepository = sourceHealthRepository;
    }

    public Optional<IngestionCursor> findCursor(String sourceKey, long chainId) {
        return cursorRepository.find(sourceKey, chainId);
    }

    public void initializeCursor(String sourceKey, long chainId, long nextBlock) {
        cursorRepository.upsert(new IngestionCursor(
                sourceKey,
                chainId,
                nextBlock,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    public void ensureCursorRow(String sourceKey, long chainId) {
        if (cursorRepository.find(sourceKey, chainId).isEmpty()) {
            initializeCursor(sourceKey, chainId, 0);
        }
    }

    public void commitBlock(
            String sourceKey,
            long chainId,
            long blockNumber,
            String blockHash,
            List<PriceObservation> prices,
            List<RateObservation> rates,
            SourceHealth health
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            for (PriceObservation price : prices) {
                priceObservationRepository.insertIdempotent(price);
            }
            for (RateObservation rate : rates) {
                rateObservationRepository.insertIdempotent(rate);
            }
            cursorRepository.upsert(new IngestionCursor(
                    sourceKey,
                    chainId,
                    blockNumber + 1,
                    blockNumber,
                    blockHash,
                    null,
                    null,
                    null,
                    null
            ));
            sourceHealthRepository.upsert(health);
        });
    }

    public void rollbackReorg(
            String sourceKey,
            String protocolId,
            long chainId,
            long rewindFromBlock,
            long rewindThroughBlock,
            long nextBlock,
            String lastFinalizedBlockHash,
            SourceHealth health
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            priceObservationRepository.markReverted(protocolId, chainId, rewindFromBlock, rewindThroughBlock);
            rateObservationRepository.markReverted(protocolId, chainId, rewindFromBlock, rewindThroughBlock);
            cursorRepository.upsert(new IngestionCursor(
                    sourceKey,
                    chainId,
                    nextBlock,
                    nextBlock - 1,
                    lastFinalizedBlockHash,
                    null,
                    null,
                    null,
                    null
            ));
            sourceHealthRepository.upsert(health);
        });
    }

    public Map<Long, String> storedBlockHashes(String protocolId, long chainId, long fromBlock, long throughBlock) {
        List<BlockHashRow> rows = jdbcClient.sql("""
                select block_number, block_hash
                from price_observations
                where source = ?
                  and chain_id = ?
                  and block_number between ? and ?
                  and is_reverted = false
                union
                select block_number, block_hash
                from rate_observations
                where protocol = ?
                  and chain_id = ?
                  and block_number between ? and ?
                  and is_reverted = false
                """)
                .param(protocolId)
                .param(chainId)
                .param(fromBlock)
                .param(throughBlock)
                .param(protocolId)
                .param(chainId)
                .param(fromBlock)
                .param(throughBlock)
                .query((rs, rowNum) -> new BlockHashRow(rs.getLong("block_number"), rs.getString("block_hash")))
                .list();

        Map<Long, String> hashes = new HashMap<>();
        for (BlockHashRow row : rows) {
            hashes.putIfAbsent(row.blockNumber(), row.blockHash());
        }
        return hashes;
    }

    public void recordFailure(String sourceKey, int consecutiveFailures, Long lagBlocks, String reason) {
        sourceHealthRepository.upsert(new SourceHealth(
                sourceKey,
                null,
                Instant.now(),
                consecutiveFailures,
                lagBlocks,
                consecutiveFailures >= 3 ? "unavailable" : "degraded",
                reason,
                null
        ));
    }

    private record BlockHashRow(long blockNumber, String blockHash) {
    }
}