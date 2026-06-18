package com.ethsimulator.ingestion;

import com.ethsimulator.protocol.rpc.EthBlockHeader;

public interface FinalizedBlockReader {

    long latestBlockNumber();

    EthBlockHeader blockAt(long blockNumber);
}