package com.ethsimulator.ingestion;

import com.ethsimulator.protocol.ProtocolAdapter;
import com.ethsimulator.protocol.ProtocolRateQuote;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class VirtualThreadTrackingAdapter implements ProtocolAdapter {

    private final ProtocolAdapter delegate;
    private final AtomicBoolean adapterVirtual = new AtomicBoolean(false);

    VirtualThreadTrackingAdapter(ProtocolAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public String protocolId() {
        return delegate.protocolId();
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
    }

    @Override
    public List<ProtocolRateQuote> fetchQuotes() {
        return delegate.fetchQuotes();
    }

    @Override
    public List<ProtocolRateQuote> fetchQuotesAtBlock(long blockNumber) {
        adapterVirtual.set(Thread.currentThread().isVirtual());
        return delegate.fetchQuotesAtBlock(blockNumber);
    }

    boolean adapterRanOnVirtualThread() {
        return adapterVirtual.get();
    }
}