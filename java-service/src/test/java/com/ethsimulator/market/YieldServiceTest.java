package com.ethsimulator.market;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class YieldServiceTest {

    private final YieldService yieldService = new YieldService(
            Clock.fixed(Instant.parse("2026-06-09T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void seedYieldsAreLabelled() {
        YieldSnapshotResponse response = yieldService.getYields("USDC");

        assertEquals("USDC", response.asset());
        assertFalse(response.yields().isEmpty());
        assertEquals("seed", response.yields().get(0).source());
    }

    @Test
    void unknownAssetUsesStaticFallback() {
        YieldSnapshotResponse response = yieldService.getYields("UNKNOWN");

        assertEquals(1, response.yields().size());
        assertEquals("static_fallback", response.yields().get(0).source());
        assertEquals("static_conservative", response.yields().get(0).protocol());
    }
}