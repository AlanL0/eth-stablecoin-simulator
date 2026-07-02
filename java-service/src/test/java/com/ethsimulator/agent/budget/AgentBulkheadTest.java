package com.ethsimulator.agent.budget;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AgentBulkheadTest {

    @Test
    void concurrentModelCallsCannotExceedCap() throws Exception {
        AgentBudgetProperties properties = new AgentBudgetProperties();
        properties.setMaxConcurrentModelCalls(1);
        properties.validateAndClamp();

        AgentModelBulkhead bulkhead = new AgentModelBulkhead(properties);
        assertThat(bulkhead.tryAcquire()).isTrue();
        assertThat(bulkhead.tryAcquire()).isFalse();

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch releaseParent = new CountDownLatch(1);
        AtomicBoolean secondAcquired = new AtomicBoolean(false);

        Thread waiter = new Thread(() -> {
            started.countDown();
            try {
                releaseParent.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            secondAcquired.set(bulkhead.tryAcquire());
        });
        waiter.start();
        started.await(2, TimeUnit.SECONDS);
        assertThat(secondAcquired.get()).isFalse();

        bulkhead.release();
        releaseParent.countDown();
        waiter.join(2_000);
        assertThat(secondAcquired.get()).isTrue();
        bulkhead.release();
    }
}