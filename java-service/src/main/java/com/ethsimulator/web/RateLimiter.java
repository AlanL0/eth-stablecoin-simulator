package com.ethsimulator.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client token bucket with bounded client tracking.
 */
public class RateLimiter {

    private final Clock clock;
    private final int requestsPerMinute;
    private final int burst;
    private final int maxClients;
    private final ConcurrentHashMap<String, ClientBucket> buckets = new ConcurrentHashMap<>();
    private final Deque<String> clientOrder = new ArrayDeque<>();

    public RateLimiter(Clock clock, int requestsPerMinute, int burst, int maxClients) {
        this.clock = clock;
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
        this.burst = Math.max(1, burst);
        this.maxClients = Math.max(1, maxClients);
    }

    public boolean tryAcquire(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        ClientBucket bucket = buckets.computeIfAbsent(key, ignored -> registerClient(key));
        return bucket.tryConsume(clock.instant(), requestsPerMinute, burst);
    }

    public long retryAfterSeconds(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        ClientBucket bucket = buckets.get(key);
        if (bucket == null) {
            return 1L;
        }
        return bucket.retryAfterSeconds(clock.instant(), requestsPerMinute, burst);
    }

    public int trackedClients() {
        return buckets.size();
    }

    void resetForTests() {
        buckets.clear();
        synchronized (this) {
            clientOrder.clear();
        }
    }

    private synchronized ClientBucket registerClient(String key) {
        ClientBucket existing = buckets.get(key);
        if (existing != null) {
            return existing;
        }
        evictIfNeeded();
        clientOrder.addLast(key);
        return new ClientBucket(clock.instant(), burst);
    }

    private synchronized void evictIfNeeded() {
        while (buckets.size() >= maxClients && !clientOrder.isEmpty()) {
            String oldest = clientOrder.removeFirst();
            buckets.remove(oldest);
        }
    }

    private static final class ClientBucket {
        private Instant lastRefill;
        private double tokens;

        private ClientBucket(Instant now, int burst) {
            this.lastRefill = now;
            this.tokens = burst;
        }

        private boolean tryConsume(Instant now, int requestsPerMinute, int burst) {
            refill(now, requestsPerMinute, burst);
            if (tokens < 1.0d) {
                return false;
            }
            tokens -= 1.0d;
            return true;
        }

        private long retryAfterSeconds(Instant now, int requestsPerMinute, int burst) {
            refill(now, requestsPerMinute, burst);
            if (tokens >= 1.0d) {
                return 0L;
            }
            double needed = 1.0d - tokens;
            double perSecond = requestsPerMinute / 60.0d;
            return Math.max(1L, (long) Math.ceil(needed / perSecond));
        }

        private void refill(Instant now, int requestsPerMinute, int burst) {
            double elapsedSeconds = Duration.between(lastRefill, now).toMillis() / 1000.0d;
            if (elapsedSeconds <= 0.0d) {
                return;
            }
            double perSecond = requestsPerMinute / 60.0d;
            tokens = Math.min(burst, tokens + elapsedSeconds * perSecond);
            lastRefill = now;
        }
    }
}