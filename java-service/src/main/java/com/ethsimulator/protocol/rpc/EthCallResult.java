package com.ethsimulator.protocol.rpc;

public record EthCallResult(
        String returnData,
        boolean reverted,
        String errorMessage
) {
    public static EthCallResult success(String returnData) {
        return new EthCallResult(returnData, false, null);
    }

    public static EthCallResult revert(String errorMessage) {
        return new EthCallResult(null, true, errorMessage);
    }
}