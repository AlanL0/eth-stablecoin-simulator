package com.ethsimulator.util;

import com.ethsimulator.api.error.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EvmAddressValidator {

    private static final Pattern EVM_ADDRESS = Pattern.compile("^0x[0-9a-fA-F]{40}$");

    private EvmAddressValidator() {
    }

    public static String requireValid(String address) {
        if (address == null || !EVM_ADDRESS.matcher(address.trim()).matches()) {
            throw new ApiException("INVALID_ADDRESS", "Invalid EVM address", HttpStatus.BAD_REQUEST);
        }
        return address.trim().toLowerCase(Locale.ROOT);
    }
}