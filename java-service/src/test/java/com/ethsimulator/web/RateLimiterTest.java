package com.ethsimulator.web;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allowsBurstThenRejectsUntilRefill() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-09T12:00:00Z"));
        RateLimiter limiter = new RateLimiter(clock, 60, 3, 100);

        assertThat(limiter.tryAcquire("client-a")).isTrue();
        assertThat(limiter.tryAcquire("client-a")).isTrue();
        assertThat(limiter.tryAcquire("client-a")).isTrue();
        assertThat(limiter.tryAcquire("client-a")).isFalse();
        assertThat(limiter.retryAfterSeconds("client-a")).isGreaterThanOrEqualTo(1L);

        clock.advanceSeconds(2);
        assertThat(limiter.tryAcquire("client-a")).isTrue();
    }

    @Test
    void tracksIndependentBucketsPerClient() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-09T12:00:00Z"));
        RateLimiter limiter = new RateLimiter(clock, 60, 1, 100);

        assertThat(limiter.tryAcquire("client-a")).isTrue();
        assertThat(limiter.tryAcquire("client-a")).isFalse();
        assertThat(limiter.tryAcquire("client-b")).isTrue();
    }

    @Test
    void evictsBeyondMaxClients() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-09T12:00:00Z"));
        RateLimiter limiter = new RateLimiter(clock, 60, 1, 2);

        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-2")).isTrue();
        assertThat(limiter.trackedClients()).isEqualTo(2);

        assertThat(limiter.tryAcquire("client-3")).isTrue();
        assertThat(limiter.trackedClients()).isLessThanOrEqualTo(2);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}