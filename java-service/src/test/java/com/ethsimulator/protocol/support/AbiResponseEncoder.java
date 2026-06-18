package com.ethsimulator.protocol.support;

import java.math.BigInteger;
import java.util.List;

public final class AbiResponseEncoder {

    private AbiResponseEncoder() {
    }

    public static String uint256(BigInteger value) {
        return encode(List.of(value));
    }

    public static String uint8(int value) {
        return encode(List.of(BigInteger.valueOf(value)));
    }

    public static String address(String address) {
        return uint256(new BigInteger(address.substring(2), 16));
    }

    public static String encode(List<BigInteger> values) {
        StringBuilder encoded = new StringBuilder("0x");
        for (BigInteger value : values) {
            encoded.append(String.format("%064x", value));
        }
        return encoded.toString();
    }

    public static String latestRoundData(
            long roundId,
            long answer,
            long startedAt,
            long updatedAt,
            long answeredInRound
    ) {
        return encode(List.of(
                BigInteger.valueOf(roundId),
                BigInteger.valueOf(answer),
                BigInteger.valueOf(startedAt),
                BigInteger.valueOf(updatedAt),
                BigInteger.valueOf(answeredInRound)
        ));
    }

    public static String jugIlks(long duty, long rate) {
        return encode(List.of(BigInteger.valueOf(duty), BigInteger.valueOf(rate)));
    }

    public static String aaveReserveData(BigInteger variableBorrowRateRay) {
        StringBuilder slots = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            BigInteger value = i == 5 ? variableBorrowRateRay : BigInteger.ZERO;
            slots.append(String.format("%064x", value));
        }
        return "0x" + slots;
    }
}