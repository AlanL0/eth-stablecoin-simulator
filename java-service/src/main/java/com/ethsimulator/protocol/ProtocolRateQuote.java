package com.ethsimulator.protocol;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Normalized on-chain quote with full provenance. Never substitutes static catalog rates.
 */
public record ProtocolRateQuote(
        String protocol,
        String product,
        RateSide side,
        AnnualizedRate rate,
        String unavailableReason,
        String lookbackWindow,
        String sourceContract,
        BlockProvenance provenance,
        Instant observedAt,
        Instant sourceTimestamp,
        boolean stale,
        boolean reverted
) {
    public ProtocolRateQuote {
        protocol = requireText(protocol, "protocol");
        product = requireText(product, "product");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(observedAt, "observedAt");
        if (rate == null && (unavailableReason == null || unavailableReason.isBlank())) {
            throw new IllegalArgumentException("rate or unavailableReason is required");
        }
        if (rate != null && unavailableReason != null && !unavailableReason.isBlank()) {
            throw new IllegalArgumentException("rate and unavailableReason are mutually exclusive");
        }
        lookbackWindow = lookbackWindow == null ? "" : lookbackWindow;
        sourceContract = sourceContract == null ? "" : sourceContract;
    }

    public Optional<AnnualizedRate> rateOptional() {
        return Optional.ofNullable(rate);
    }

    public Optional<String> unavailableReasonOptional() {
        return Optional.ofNullable(unavailableReason).filter(reason -> !reason.isBlank());
    }

    public static ProtocolRateQuote available(
            String protocol,
            String product,
            RateSide side,
            AnnualizedRate rate,
            String lookbackWindow,
            String sourceContract,
            BlockProvenance provenance,
            Instant observedAt,
            Instant sourceTimestamp,
            boolean stale
    ) {
        return new ProtocolRateQuote(
                protocol,
                product,
                side,
                rate,
                null,
                lookbackWindow,
                sourceContract,
                provenance,
                observedAt,
                sourceTimestamp,
                stale,
                false
        );
    }

    public static ProtocolRateQuote unavailable(
            String protocol,
            String product,
            RateSide side,
            String reason,
            String lookbackWindow,
            String sourceContract,
            BlockProvenance provenance,
            Instant observedAt,
            boolean reverted
    ) {
        return new ProtocolRateQuote(
                protocol,
                product,
                side,
                null,
                reason,
                lookbackWindow,
                sourceContract,
                provenance,
                observedAt,
                null,
                false,
                reverted
        );
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}