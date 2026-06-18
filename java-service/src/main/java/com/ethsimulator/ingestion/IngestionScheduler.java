package com.ethsimulator.ingestion;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.websocket.WebSocketService;

import javax.sql.DataSource;
import java.util.UUID;

@Component
@ConditionalOnBean(DataSource.class)
public class IngestionScheduler {

    private final IngestionProperties properties;
    private final IngestionCoordinator coordinator;
    private final IngestionWakeSignal wakeSignal;
    private final ObjectProvider<WebSocketService> webSocketServiceProvider;
    private final String ownerId = UUID.randomUUID().toString();

    public IngestionScheduler(
            IngestionProperties properties,
            IngestionCoordinator coordinator,
            IngestionWakeSignal wakeSignal,
            ObjectProvider<WebSocketService> webSocketServiceProvider
    ) {
        this.properties = properties;
        this.coordinator = coordinator;
        this.wakeSignal = wakeSignal;
        this.webSocketServiceProvider = webSocketServiceProvider;
        wakeSignal.onWake(block -> coordinator.runCycle(ownerId));
    }

    @Scheduled(fixedDelayString = "${eth-simulator.ingestion.poll-interval-ms:30000}")
    public void poll() {
        if (!properties.isEnabled()) {
            return;
        }
        WebSocketService webSocketService = webSocketServiceProvider.getIfAvailable();
        if (properties.isWebsocketWakeEnabled() && !wakeSignal.websocketConnected()) {
            wakeSignal.connectIfConfigured(webSocketService, properties, () -> coordinator.runCycle(ownerId));
        }
        coordinator.runCycle(ownerId);
    }
}