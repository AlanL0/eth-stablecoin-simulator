package com.ethsimulator.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.web3j.protocol.websocket.WebSocketService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * WebSocket new-block notifications wake ingestion; polling remains the reliable fallback.
 */
@Component
@ConditionalOnExpression(
        "T(org.springframework.util.StringUtils).hasText('${DATABASE_URL:}')"
                + " && T(com.ethsimulator.config.BlockchainConfig).hasHttpRpcUrl(@environment)"
)
public class IngestionWakeSignal {

    private static final Logger log = LoggerFactory.getLogger(IngestionWakeSignal.class);

    private final AtomicBoolean websocketConnected = new AtomicBoolean(false);
    private volatile Consumer<String> wakeListener = block -> {
    };

    public void onWake(Consumer<String> listener) {
        this.wakeListener = listener;
    }

    public void signalNewBlock(String blockNumberHex) {
        wakeListener.accept(blockNumberHex);
    }

    public boolean connectIfConfigured(
            WebSocketService webSocketService,
            IngestionProperties properties,
            Runnable onWake
    ) {
        if (!properties.isWebsocketWakeEnabled() || webSocketService == null) {
            return false;
        }
        try {
            webSocketService.connect();
            websocketConnected.set(true);
            onWake.run();
            return true;
        } catch (Exception ex) {
            websocketConnected.set(false);
            log.warn("WebSocket wake signal unavailable; polling fallback remains active", ex);
            return false;
        }
    }

    public void disconnect(WebSocketService webSocketService) {
        if (webSocketService == null) {
            return;
        }
        try {
            webSocketService.close();
        } catch (Exception ex) {
            log.debug("WebSocket close ignored", ex);
        } finally {
            websocketConnected.set(false);
        }
    }

    public boolean websocketConnected() {
        return websocketConnected.get();
    }
}