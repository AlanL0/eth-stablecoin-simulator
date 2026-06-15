package com.ethsimulator.audit;

import com.ethsimulator.blockchain.TransferEventFetcher;
import com.ethsimulator.blockchain.TransferEventRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuditCache {

    private final TransferEventFetcher transferEventFetcher;
    private final Map<String, List<TransferEventRecord>> cache = new ConcurrentHashMap<>();

    public AuditCache(TransferEventFetcher transferEventFetcher) {
        this.transferEventFetcher = transferEventFetcher;
    }

    public List<TransferEventRecord> eventsFor(String address) {
        String normalizedAddress = address.toLowerCase(Locale.ROOT);
        return cache.computeIfAbsent(normalizedAddress, this::loadAndDedupe);
    }

    public String source() {
        return transferEventFetcher.source();
    }

    void clear() {
        cache.clear();
    }

    private List<TransferEventRecord> loadAndDedupe(String address) {
        List<TransferEventRecord> fetched = transferEventFetcher.fetchTransferEvents(address);
        Map<String, TransferEventRecord> deduped = new LinkedHashMap<>();
        for (TransferEventRecord event : fetched) {
            String key = event.txHash().toLowerCase(Locale.ROOT) + ":" + event.logIndex();
            deduped.putIfAbsent(key, event);
        }
        return List.copyOf(new ArrayList<>(deduped.values()));
    }
}