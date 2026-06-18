package com.ethsimulator.protocol.rpc;

import java.math.BigInteger;

public record TransferLog(
        String contractAddress,
        String from,
        String to,
        BigInteger value,
        BigInteger blockNumber
) {
}