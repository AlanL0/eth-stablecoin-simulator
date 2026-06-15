package com.ethsimulator.config;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TokenAllowlist {

    public record TokenEntry(String symbol, String contractAddress, int decimals) {
    }

    private static final List<TokenEntry> TOKENS = List.of(
            new TokenEntry("USDC", "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", 6),
            new TokenEntry("USDT", "0xdac17f958d2ee523a2206206994597c13d831ec7", 6),
            new TokenEntry("DAI", "0x6b175474e89094c44da98b954eedeac495271d0f", 18),
            new TokenEntry("GHO", "0x40d16fc0246ad3b81f7f1a5d47c172d1a55702da", 18)
    );

    private TokenAllowlist() {
    }

    public static List<TokenEntry> all() {
        return TOKENS;
    }

    public static Optional<TokenEntry> bySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return TOKENS.stream()
                .filter(token -> token.symbol().equals(normalized))
                .findFirst();
    }

    public static Optional<TokenEntry> byContractAddress(String contractAddress) {
        if (contractAddress == null || contractAddress.isBlank()) {
            return Optional.empty();
        }
        String normalized = contractAddress.trim().toLowerCase(Locale.ROOT);
        return TOKENS.stream()
                .filter(token -> token.contractAddress().equals(normalized))
                .findFirst();
    }
}