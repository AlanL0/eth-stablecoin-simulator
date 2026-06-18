package com.ethsimulator.protocol;

import java.util.List;

/**
 * Read-only mainnet protocol adapter. Implementations must not submit transactions.
 */
public interface ProtocolAdapter {

    String protocolId();

    boolean enabled();

    List<ProtocolRateQuote> fetchQuotes();
}