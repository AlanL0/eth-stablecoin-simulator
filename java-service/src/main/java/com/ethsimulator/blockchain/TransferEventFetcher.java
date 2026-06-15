package com.ethsimulator.blockchain;

import java.util.List;

public interface TransferEventFetcher {

    List<TransferEventRecord> fetchTransferEvents(String walletAddress);

    String source();
}