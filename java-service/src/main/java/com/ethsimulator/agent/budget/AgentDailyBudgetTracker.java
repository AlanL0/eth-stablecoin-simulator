package com.ethsimulator.agent.budget;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AgentDailyBudgetTracker {

    private final AgentBudgetProperties properties;
    private final Clock clock;
    private final AtomicReference<LocalDate> day = new AtomicReference<>();
    private final AtomicInteger modelCalls = new AtomicInteger();
    private final AtomicReference<BigDecimal> reservedUsd = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> reconciledUsd = new AtomicReference<>(BigDecimal.ZERO);

    public AgentDailyBudgetTracker(AgentBudgetProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized boolean tryReserveModelCallAndUsd(BigDecimal worstCaseUsd) {
        resetIfNewDay();
        if (modelCalls.get() >= properties.getDailyModelCallCap()) {
            return false;
        }
        BigDecimal nextReserved = reservedUsd.get().add(nonNegative(worstCaseUsd));
        if (nextReserved.compareTo(properties.getDailyUsdBudget()) > 0) {
            return false;
        }
        modelCalls.incrementAndGet();
        reservedUsd.set(nextReserved);
        return true;
    }

    public synchronized void reconcile(BigDecimal worstCaseUsd, BigDecimal actualUsd) {
        resetIfNewDay();
        BigDecimal delta = nonNegative(actualUsd).subtract(nonNegative(worstCaseUsd));
        reconciledUsd.updateAndGet(current -> current.add(delta));
        reservedUsd.updateAndGet(current -> current.add(delta));
    }

    public synchronized int modelCallsToday() {
        resetIfNewDay();
        return modelCalls.get();
    }

    public synchronized BigDecimal reservedUsdToday() {
        resetIfNewDay();
        return reservedUsd.get();
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate current = day.get();
        if (current == null || !current.equals(today)) {
            day.set(today);
            modelCalls.set(0);
            reservedUsd.set(BigDecimal.ZERO);
            reconciledUsd.set(BigDecimal.ZERO);
        }
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}