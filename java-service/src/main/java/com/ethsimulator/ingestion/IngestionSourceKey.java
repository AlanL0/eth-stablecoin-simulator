package com.ethsimulator.ingestion;

import com.ethsimulator.protocol.ProtocolAdapter;

public final class IngestionSourceKey {

    private IngestionSourceKey() {
    }

    public static String forAdapter(ProtocolAdapter adapter) {
        return "protocol:" + adapter.protocolId();
    }
}