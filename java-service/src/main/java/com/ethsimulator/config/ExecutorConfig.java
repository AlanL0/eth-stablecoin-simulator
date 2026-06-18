package com.ethsimulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean(name = "blockingIoExecutor")
    public BlockingIoExecutor blockingIoExecutor(EthSimulatorProperties properties) {
        Executor virtualThreads = Executors.newVirtualThreadPerTaskExecutor();
        return new BlockingIoExecutor(virtualThreads, properties.getBlockingIoMaxConcurrency());
    }
}