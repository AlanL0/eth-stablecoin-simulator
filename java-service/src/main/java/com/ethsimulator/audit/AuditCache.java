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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuditCache {

    private final TransferEventFetcher transferEventFetcher;
    private final Clock clock;
    private final Duration ttl;
    private final int maxAddresses;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<List<TransferEventRecord>>> inFlight =
            new ConcurrentHashMap<>();

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

        CompletableFuture<List<TransferEventRecord>> future = inFlight.compute(normalizedAddress, (key, current) -> {
            if (current != null && !current.isDone()) {
                return current;
            }
            CompletableFuture<List<TransferEventRecord>> created = CompletableFuture.supplyAsync(
                    () -> loadAndDedupe(key)
            );
            created.whenComplete((ignored, error) -> inFlight.remove(key, created));
            return created;
        });

        try {
            List<TransferEventRecord> loaded = future.join();
            cache.put(normalizedAddress, new CacheEntry(loaded, clock.instant()));
            evictIfNeeded();
            return loaded;
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw ex;
        }
    }

    public String source() {
        return transferEventFetcher.source();
    }

    void clear() {
        cache.clear();
        inFlight.clear();
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
        String oldestKey = null;
        Instant oldestAt = null;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (oldestAt == null || entry.getValue().loadedAt.isBefore(oldestAt)) {
                oldestAt = entry.getValue().loadedAt;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    private record CacheEntry(List<TransferEventRecord> events, Instant loadedAt) {
    }
}