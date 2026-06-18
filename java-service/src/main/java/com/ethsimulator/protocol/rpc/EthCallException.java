package com.ethsimulator.protocol.rpc;

public class EthCallException extends RuntimeException {

    public EthCallException(String message) {
        super(message);
    }

    public EthCallException(String message, Throwable cause) {
        super(message, cause);
    }
}