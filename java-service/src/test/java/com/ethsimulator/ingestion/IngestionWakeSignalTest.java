package com.ethsimulator.ingestion;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionWakeSignalTest {

    @Test
    void websocketDisconnectLeavesPollingFallbackActive() {
        IngestionProperties properties = new IngestionProperties();
        properties.setWebsocketWakeEnabled(true);

        IngestionWakeSignal wakeSignal = new IngestionWakeSignal();
        AtomicInteger pollCycles = new AtomicInteger();

        wakeSignal.onWake(block -> pollCycles.incrementAndGet());
        assertThat(wakeSignal.connectIfConfigured(null, properties, () -> {
        })).isFalse();
        assertThat(wakeSignal.websocketConnected()).isFalse();

        wakeSignal.signalNewBlock("0x1");
        pollCycles.incrementAndGet();

        assertThat(pollCycles.get()).isEqualTo(2);
    }
}