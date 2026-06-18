package com.ethsimulator.ingestion;

public record IngestionCycleResult(
        boolean success,
        long fromBlock,
        long toBlock,
        int blocksIngested,
        String status
) {
    public static IngestionCycleResult ingested(long fromBlock, long toBlock, int blocksIngested) {
        return new IngestionCycleResult(true, fromBlock, toBlock, blocksIngested, "ingested");
    }

    public static IngestionCycleResult upToDate(long nextBlock) {
        return new IngestionCycleResult(true, nextBlock, nextBlock - 1, 0, "up_to_date");
    }

    public static IngestionCycleResult catchUpCapped(long nextBlock) {
        return new IngestionCycleResult(true, nextBlock, nextBlock - 1, 0, "catch_up_capped");
    }

    public static IngestionCycleResult reorgHandled(long rewindTo) {
        return new IngestionCycleResult(true, rewindTo, rewindTo - 1, 0, "reorg_handled");
    }
}