package com.ethsimulator.ingestion;

import com.ethsimulator.config.BlockingIoExecutor;
import com.ethsimulator.persistence.IngestionCursor;
import com.ethsimulator.persistence.IngestionLeaderLock;
import com.ethsimulator.persistence.SourceHealth;
import com.ethsimulator.protocol.ProtocolAdapter;
import com.ethsimulator.protocol.ProtocolRateQuote;
import com.ethsimulator.protocol.ProtocolSourcesProperties;
import com.ethsimulator.protocol.rpc.EthBlockHeader;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnBean(DataSource.class)
public class IngestionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(IngestionCoordinator.class);

    private final IngestionProperties properties;
    private final ProtocolSourcesProperties protocolSourcesProperties;
    private final List<ProtocolAdapter> adapters;
    private final FinalizedBlockReader blockReader;
    private final IngestionLeaderLock leaderLock;
    private final IngestionPersistenceService persistenceService;
    private final IngestionReorgHandler reorgHandler;
    private final IngestionMetrics metrics;
    private final BlockingIoExecutor blockingIoExecutor;

    public IngestionCoordinator(
            IngestionProperties properties,
            ProtocolSourcesProperties protocolSourcesProperties,
            List<ProtocolAdapter> adapters,
            FinalizedBlockReader blockReader,
            IngestionLeaderLock leaderLock,
            IngestionPersistenceService persistenceService,
            IngestionReorgHandler reorgHandler,
            IngestionMetrics metrics,
            BlockingIoExecutor blockingIoExecutor
    ) {
        this.properties = properties;
        this.protocolSourcesProperties = protocolSourcesProperties;
        this.adapters = adapters;
        this.blockReader = blockReader;
        this.leaderLock = leaderLock;
        this.persistenceService = persistenceService;
        this.reorgHandler = reorgHandler;
        this.metrics = metrics;
        this.blockingIoExecutor = blockingIoExecutor;
    }

    public void runCycle(String ownerId) {
        if (!properties.isEnabled()) {
            return;
        }
        for (ProtocolAdapter adapter : adapters) {
            if (!adapter.enabled()) {
                continue;
            }
            String sourceKey = IngestionSourceKey.forAdapter(adapter);
            long chainId = protocolSourcesProperties.getChainId();
            persistenceService.ensureCursorRow(sourceKey, chainId);
            Duration lease = Duration.ofSeconds(properties.getLeaseDurationSeconds());
            if (!leaderLock.tryAcquire(sourceKey, chainId, ownerId, lease)) {
                log.debug("Skipping ingestion for {} — not leader", sourceKey);
                continue;
            }
            try {
                ingestSource(adapter, sourceKey, chainId);
            } finally {
                leaderLock.release(sourceKey, chainId);
            }
        }
    }

    public IngestionCycleResult ingestSource(ProtocolAdapter adapter, String sourceKey, long chainId) {
        Timer.Sample sample = metrics.startCycle();
        boolean success = false;
        try {
            IngestionCycleResult result = ingestSourceInternal(adapter, sourceKey, chainId);
            success = result.success();
            return result;
        } finally {
            metrics.recordCycle(sourceKey, sample, success);
        }
    }

    private IngestionCycleResult ingestSourceInternal(ProtocolAdapter adapter, String sourceKey, long chainId) {
        IngestionCursor cursor = resolveCursor(sourceKey, chainId);

        if (!reorgHandler.verifyPriorFinalizedHash(cursor)) {
            long rewindTo = reorgHandler.handleReorg(sourceKey, adapter.protocolId(), chainId, cursor);
            log.warn("Reorg handled for {} — rewound to block {}", sourceKey, rewindTo);
            return IngestionCycleResult.reorgHandled(rewindTo);
        }

        long latestHead = blockReader.latestBlockNumber();
        long finalizedHead = latestHead - properties.getConfirmationDepth();
        long nextBlock = cursor.nextBlock();
        if (nextBlock > finalizedHead) {
            metrics.recordLag(sourceKey, 0);
            return IngestionCycleResult.upToDate(nextBlock);
        }

        long lag = finalizedHead - nextBlock + 1;
        metrics.recordLag(sourceKey, lag);
        if (lag > properties.getMaxSourceLagBlocks()) {
            persistenceService.recordFailure(
                    sourceKey,
                    1,
                    lag,
                    "source lag exceeds configured maximum"
            );
        }

        long maxEnd = Math.min(finalizedHead, nextBlock + properties.getRangeSize() - 1);
        long catchUpCap = nextBlock + properties.getMaxCatchUpBlocks() - 1;
        long endBlock = Math.min(maxEnd, catchUpCap);
        if (endBlock < nextBlock) {
            return IngestionCycleResult.catchUpCapped(nextBlock);
        }

        int ingested = 0;
        for (long block = nextBlock; block <= endBlock; block++) {
            ingestBlock(adapter, sourceKey, chainId, block);
            ingested++;
        }
        metrics.recordBlocksIngested(sourceKey, ingested);
        return IngestionCycleResult.ingested(nextBlock, endBlock, ingested);
    }

    private IngestionCursor resolveCursor(String sourceKey, long chainId) {
        IngestionCursor cursor = persistenceService.findCursor(sourceKey, chainId).orElse(null);
        if (cursor == null || (cursor.lastFinalizedBlock() == null && cursor.nextBlock() == 0)) {
            long startBlock = properties.getStartBlock() != null
                    ? properties.getStartBlock()
                    : Math.max(0, blockReader.latestBlockNumber() - properties.getConfirmationDepth());
            persistenceService.initializeCursor(sourceKey, chainId, startBlock);
            return persistenceService.findCursor(sourceKey, chainId).orElseThrow();
        }
        return cursor;
    }

    private void ingestBlock(ProtocolAdapter adapter, String sourceKey, long chainId, long blockNumber) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                runOnVirtualThread(() -> ingestBlockOnce(adapter, sourceKey, chainId, blockNumber));
                return;
            } catch (RuntimeException ex) {
                if (attempt >= properties.getMaxRetries()) {
                    metrics.recordFailure(sourceKey);
                    persistenceService.recordFailure(sourceKey, attempt, null, ex.getMessage());
                    throw ex;
                }
                metrics.recordRetry(sourceKey);
                try {
                    metrics.sleepBackoff(properties.getRetryBackoffMs() * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during ingestion retry", interrupted);
                }
            }
        }
    }

    void ingestBlockOnce(ProtocolAdapter adapter, String sourceKey, long chainId, long blockNumber) {
        try {
            CompletableFuture<BlockIngestionPayload> future = CompletableFuture.supplyAsync(
                    () -> fetchBlockPayload(adapter, blockNumber),
                    blockingIoExecutor
            );
            BlockIngestionPayload payload = future.get(properties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
            runOnVirtualThread(() -> persistenceService.commitBlock(
                    sourceKey,
                    chainId,
                    blockNumber,
                    payload.header().hash(),
                    payload.mapped().prices(),
                    payload.mapped().rates(),
                    healthy(sourceKey, blockNumber, payload.quotes())
            ));
        } catch (TimeoutException ex) {
            throw new IllegalStateException("RPC timeout ingesting block " + blockNumber, ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new IllegalStateException("Failed ingesting block " + blockNumber, cause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted ingesting block " + blockNumber, ex);
        }
    }

    private BlockIngestionPayload fetchBlockPayload(ProtocolAdapter adapter, long blockNumber) {
        if (!Thread.currentThread().isVirtual()) {
            throw new IllegalStateException("Adapter fetch must run on a virtual thread");
        }
        EthBlockHeader header = blockReader.blockAt(blockNumber);
        List<ProtocolRateQuote> quotes = adapter.fetchQuotesAtBlock(blockNumber);
        QuoteObservationMapper.MappedObservations mapped = QuoteObservationMapper.mapQuotes(quotes);
        return new BlockIngestionPayload(header, quotes, mapped);
    }

    private SourceHealth healthy(String sourceKey, long blockNumber, List<ProtocolRateQuote> quotes) {
        boolean degraded = quotes.stream().anyMatch(quote -> quote.unavailableReasonOptional().isPresent());
        return new SourceHealth(
                sourceKey,
                Instant.now(),
                degraded ? Instant.now() : null,
                0,
                null,
                degraded ? "degraded" : "healthy",
                degraded ? "one or more quotes unavailable at block " + blockNumber : null,
                null
        );
    }

    private void runOnVirtualThread(Runnable action) {
        if (Thread.currentThread().isVirtual()) {
            action.run();
            return;
        }
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread worker = Thread.startVirtualThread(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        try {
            worker.join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for virtual thread", ex);
        }
        Throwable error = failure.get();
        if (error == null) {
            return;
        }
        if (error instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException("Virtual thread ingestion failed", error);
    }

    private record BlockIngestionPayload(
            EthBlockHeader header,
            List<ProtocolRateQuote> quotes,
            QuoteObservationMapper.MappedObservations mapped
    ) {
    }
}