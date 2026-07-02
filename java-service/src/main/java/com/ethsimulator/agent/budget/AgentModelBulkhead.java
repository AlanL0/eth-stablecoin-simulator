package com.ethsimulator.agent.budget;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
public class AgentModelBulkhead {

    private final Semaphore permits;

    public AgentModelBulkhead(AgentBudgetProperties properties) {
        this.permits = new Semaphore(properties.getMaxConcurrentModelCalls(), true);
    }

    public boolean tryAcquire() {
        return permits.tryAcquire();
    }

    public void release() {
        permits.release();
    }
}