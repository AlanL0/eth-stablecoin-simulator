package com.ethsimulator.ingestion;

import com.ethsimulator.persistence.IngestionCursor;
import com.ethsimulator.persistence.SourceHealth;
import com.ethsimulator.protocol.rpc.EthBlockHeader;

import java.time.Instant;
import java.util.Map;

public class IngestionReorgHandler {

    private final IngestionProperties properties;
    private final FinalizedBlockReader blockReader;
    private final IngestionPersistenceService persistenceService;

    public IngestionReorgHandler(
            IngestionProperties properties,
            FinalizedBlockReader blockReader,
            IngestionPersistenceService persistenceService
    ) {
        this.properties = properties;
        this.blockReader = blockReader;
        this.persistenceService = persistenceService;
    }

    public boolean verifyPriorFinalizedHash(IngestionCursor cursor) {
        if (cursor.lastFinalizedBlock() == null || cursor.lastFinalizedBlockHash() == null) {
            return true;
        }
        EthBlockHeader header = blockReader.blockAt(cursor.lastFinalizedBlock());
        return header.hash().equalsIgnoreCase(cursor.lastFinalizedBlockHash());
    }

    public long handleReorg(String sourceKey, String protocolId, long chainId, IngestionCursor cursor) {
        long reorgTip = cursor.lastFinalizedBlock();
        long searchFloor = Math.max(0, reorgTip - properties.getMaxReorgWindowBlocks());
        Map<Long, String> storedHashes = persistenceService.storedBlockHashes(
                protocolId,
                chainId,
                searchFloor,
                reorgTip
        );

        long ancestor = findCommonAncestor(searchFloor, reorgTip, storedHashes);
        if (ancestor < searchFloor) {
            sourceHealthUnavailable(sourceKey, "deep reorg beyond window; ingestion paused");
            throw new DeepReorgException("Reorg deeper than " + properties.getMaxReorgWindowBlocks() + " blocks");
        }

        long rewindFrom = ancestor + 1;
        String ancestorHash = ancestor < 0
                ? null
                : blockReader.blockAt(ancestor).hash();

        persistenceService.rollbackReorg(
                sourceKey,
                protocolId,
                chainId,
                rewindFrom,
                reorgTip,
                rewindFrom,
                ancestorHash,
                new SourceHealth(
                        sourceKey,
                        null,
                        Instant.now(),
                        0,
                        null,
                        "degraded",
                        "reorg rollback to block " + ancestor,
                        null
                )
        );
        return rewindFrom;
    }

    private void sourceHealthUnavailable(String sourceKey, String reason) {
        persistenceService.recordFailure(sourceKey, 3, null, reason);
    }

    private long findCommonAncestor(long searchFloor, long reorgTip, Map<Long, String> storedHashes) {
        for (long block = reorgTip; block >= searchFloor; block--) {
            String stored = storedHashes.get(block);
            if (stored == null) {
                continue;
            }
            EthBlockHeader header = blockReader.blockAt(block);
            if (header.hash().equalsIgnoreCase(stored)) {
                return block;
            }
        }
        return searchFloor - 1;
    }
}