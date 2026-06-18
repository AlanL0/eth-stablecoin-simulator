package com.ethsimulator.config;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Bounded wrapper over a virtual-thread executor for blocking adapter boundaries.
 */
public final class BlockingIoExecutor implements Executor {

    private final Executor delegate;
    private final Semaphore permits;

    public BlockingIoExecutor(Executor delegate, int maxConcurrency) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be >= 1");
        }
        this.permits = new Semaphore(maxConcurrency);
    }

    @Override
    public void execute(Runnable command) {
        if (!permits.tryAcquire()) {
            throw new RejectedExecutionException("Blocking I/O concurrency limit reached");
        }
        delegate.execute(() -> {
            try {
                command.run();
            } finally {
                permits.release();
            }
        });
    }
}