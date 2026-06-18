package com.ethsimulator.protocol;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EthAddressValidator {

    private static final Pattern ADDRESS = Pattern.compile("^0x[0-9a-fA-F]{40}$");

    private EthAddressValidator() {
    }

    public static boolean isWellFormed(String address) {
        return StringUtils.hasText(address) && ADDRESS.matcher(address.trim()).matches();
    }

    public static String normalize(String address) {
        if (!isWellFormed(address)) {
            throw new IllegalArgumentException("Malformed Ethereum address: " + address);
        }
        return address.trim().toLowerCase(Locale.ROOT);
    }
}