package com.ethsimulator.protocol.abi;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;

public final class AbiDecoder {

    private AbiDecoder() {
    }

    public static List<Type> decode(String data, Function function) {
        return FunctionReturnDecoder.decode(data, function.getOutputParameters());
    }

    public static BigInteger uint256(String data, Function function, int index) {
        List<Type> decoded = decode(data, function);
        if (decoded.size() <= index) {
            throw new IllegalArgumentException("Missing output index " + index + " for " + function.getName());
        }
        return (BigInteger) decoded.get(index).getValue();
    }

    public static BigInteger int256(String data, Function function, int index) {
        return uint256(data, function, index);
    }

    public static int uint8(String data, Function function) {
        BigInteger value = uint256(data, function, 0);
        return value.intValueExact();
    }

    public static String address(String data, Function function) {
        List<Type> decoded = decode(data, function);
        Object value = decoded.getFirst().getValue();
        if (value instanceof String address) {
            return address.toLowerCase();
        }
        BigInteger numeric = (BigInteger) value;
        String hex = Numeric.toHexStringNoPrefix(numeric);
        return "0x" + hex.substring(hex.length() - 40);
    }

    public static BigInteger slotUint256(String data, int slotIndex) {
        String hex = Numeric.cleanHexPrefix(data);
        int start = slotIndex * 64;
        if (hex.length() < start + 64) {
            throw new IllegalArgumentException("Data too short for slot " + slotIndex);
        }
        return new BigInteger(hex.substring(start, start + 64), 16);
    }

    public static BigInteger aaveVariableBorrowRate(String reserveData) {
        return slotUint256(reserveData, 5);
    }
}