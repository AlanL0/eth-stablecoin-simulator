package com.ethsimulator.market;

import java.util.List;

public record YieldSnapshotResponse(String asset, List<YieldQuote> yields) {
}