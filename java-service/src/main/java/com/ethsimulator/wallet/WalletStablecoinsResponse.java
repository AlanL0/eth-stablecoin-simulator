package com.ethsimulator.wallet;

import java.util.List;

public record WalletStablecoinsResponse(
        String address,
        List<StablecoinBalance> balances,
        String source,
        String observedAt,
        List<String> assumptions
) {
}