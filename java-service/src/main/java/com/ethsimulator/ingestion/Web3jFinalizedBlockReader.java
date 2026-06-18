package com.ethsimulator.ingestion;

import com.ethsimulator.protocol.rpc.EthBlockHeader;
import com.ethsimulator.protocol.rpc.EthCallClient;

import java.math.BigInteger;

public class Web3jFinalizedBlockReader implements FinalizedBlockReader {

    private final EthCallClient ethCallClient;

    public Web3jFinalizedBlockReader(EthCallClient ethCallClient) {
        this.ethCallClient = ethCallClient;
    }

    @Override
    public long latestBlockNumber() {
        return ethCallClient.latestBlock().number().longValue();
    }

    @Override
    public EthBlockHeader blockAt(long blockNumber) {
        return ethCallClient.blockAt(BigInteger.valueOf(blockNumber));
    }
}