package com.ethsimulator.market;

import java.util.List;

public record YieldSnapshotResponse(
        String asset,
        List<YieldQuote> yields,
        @io.swagger.v3.oas.annotations.media.Schema(
                description = "live when ingested rates are served; seed_fallback otherwise",
                allowableValues = {"live", "seed_fallback"}
        )
        String dataMode
) {
}