package com.ethsimulator.ingestion;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class IngestionMetrics {

    private final MeterRegistry meterRegistry;

    public IngestionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startCycle() {
        return Timer.start(meterRegistry);
    }

    public void recordCycle(String sourceKey, Timer.Sample sample, boolean success) {
        sample.stop(Timer.builder("ingestion.cycle.duration")
                .tag("source", sourceKey)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));
        Counter.builder("ingestion.cycle.total")
                .tag("source", sourceKey)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    public void recordRetry(String sourceKey) {
        Counter.builder("ingestion.retries")
                .tag("source", sourceKey)
                .register(meterRegistry)
                .increment();
    }

    public void recordLag(String sourceKey, long lagBlocks) {
        meterRegistry.gauge("ingestion.lag.blocks", java.util.List.of(
                io.micrometer.core.instrument.Tag.of("source", sourceKey)
        ), lagBlocks);
    }

    public void recordFailure(String sourceKey) {
        Counter.builder("ingestion.failures")
                .tag("source", sourceKey)
                .register(meterRegistry)
                .increment();
    }

    public void recordBlocksIngested(String sourceKey, int blocks) {
        Counter.builder("ingestion.blocks")
                .tag("source", sourceKey)
                .register(meterRegistry)
                .increment(blocks);
    }

    public void sleepBackoff(long backoffMs) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(backoffMs);
    }
}