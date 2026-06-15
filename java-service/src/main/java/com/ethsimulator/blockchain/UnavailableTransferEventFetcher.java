package com.ethsimulator.blockchain;

import java.util.List;

public class UnavailableTransferEventFetcher implements TransferEventFetcher {

    @Override
    public List<TransferEventRecord> fetchTransferEvents(String walletAddress) {
        return List.of();
    }

    @Override
    public String source() {
        return "unavailable";
    }
}