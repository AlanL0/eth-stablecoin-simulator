package com.ethsimulator.protocol.rpc;

import java.math.BigInteger;
import java.time.Instant;

public record EthBlockHeader(
        BigInteger number,
        String hash,
        Instant timestamp
) {
}