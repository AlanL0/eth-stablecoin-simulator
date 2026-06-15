package com.ethsimulator.audit;

import com.ethsimulator.blockchain.TransferEventFetcher;
import com.ethsimulator.blockchain.TransferEventRecord;
import com.ethsimulator.config.EthSimulatorProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuditCache {

    private final TransferEventFetcher transferEventFetcher;
    private final Clock clock;
    private final Duration ttl;
    private final int maxAddresses;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public AuditCache(
            TransferEventFetcher transferEventFetcher,
            Clock clock,
            EthSimulatorProperties properties
    ) {
        this.transferEventFetcher = transferEventFetcher;
        this.clock = clock;
        this.maxAddresses = Math.max(1, properties.getAuditCacheMaxAddresses());
        this.ttl = Duration.ofSeconds(Math.max(60, properties.getAuditCacheTtlSeconds()));
    }

    public List<TransferEventRecord> eventsFor(String address) {
        String normalizedAddress = address.toLowerCase(Locale.ROOT);
        Instant now = clock.instant();
        CacheEntry existing = cache.get(normalizedAddress);
        if (existing != null && existing.loadedAt.plus(ttl).isAfter(now)) {
            return existing.events;
        }

        List<TransferEventRecord> loaded = loadAndDedupe(normalizedAddress);
        cache.put(normalizedAddress, new CacheEntry(loaded, now));
        evictIfNeeded();
        return loaded;
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

    private void evictIfNeeded() {
        if (cache.size() <= maxAddresses) {
            return;
        }
        Instant now = clock.instant();
        cache.entrySet().removeIf(entry -> entry.getValue().loadedAt.plus(ttl).isBefore(now));
        while (cache.size() > maxAddresses) {
            String oldestKey = cache.entrySet().stream()
                    .min(Map.Entry.comparingByValue((a, b) -> a.loadedAt.compareTo(b.loadedAt)))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestKey == null) {
                break;
            }
            cache.remove(oldestKey);
        }
    }

    private record CacheEntry(List<TransferEventRecord> events, Instant loadedAt) {
    }
}