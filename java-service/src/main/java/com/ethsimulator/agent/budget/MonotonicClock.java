package com.ethsimulator.agent.budget;

@FunctionalInterface
public interface MonotonicClock {

    long nanoTime();

    static MonotonicClock system() {
        return System::nanoTime;
    }
}