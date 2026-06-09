package com.ethsimulator.market;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class EthPriceCache {

    private final Clock clock;
    private volatile CachedEntry entry;

    public EthPriceCache(Clock clock) {
        this.clock = clock;
    }

    public Optional<EthPriceQuote> getIfFresh(Duration ttl) {
        CachedEntry current = entry;
        if (current == null) {
            return Optional.empty();
        }
        if (clock.instant().isAfter(current.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(current.quote().withSource(EthPriceSource.CACHE));
    }

    public void put(EthPriceQuote quote, Duration ttl) {
        Instant expiresAt = clock.instant().plus(ttl);
        entry = new CachedEntry(quote, expiresAt);
    }

    public void clear() {
        entry = null;
    }

    private record CachedEntry(EthPriceQuote quote, Instant expiresAt) {
    }
}