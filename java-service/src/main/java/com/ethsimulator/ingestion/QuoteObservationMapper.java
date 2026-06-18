package com.ethsimulator.ingestion;

import com.ethsimulator.persistence.PriceObservation;
import com.ethsimulator.persistence.RateObservation;
import com.ethsimulator.protocol.ProtocolRateQuote;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.protocol.RateSide;

import java.util.ArrayList;
import java.util.List;

public final class QuoteObservationMapper {

    private QuoteObservationMapper() {
    }

    public static List<PriceObservation> toPriceObservations(ProtocolRateQuote quote) {
        if (quote.side() != RateSide.PRICE || quote.rateOptional().isEmpty()) {
            return List.of();
        }
        String[] pair = quote.product().split("/");
        if (pair.length != 2) {
            return List.of();
        }
        return List.of(PriceObservation.newObservation(
                pair[0],
                pair[1],
                quote.rate().value(),
                quote.protocol(),
                quote.provenance().chainId(),
                quote.provenance().blockNumber(),
                quote.provenance().blockHash(),
                quote.lookbackWindow(),
                quote.observedAt(),
                quote.sourceTimestamp(),
                quote.stale(),
                true,
                false
        ));
    }

    public static List<RateObservation> toRateObservations(ProtocolRateQuote quote) {
        if (quote.side() == RateSide.PRICE || quote.rateOptional().isEmpty()) {
            return List.of();
        }
        return List.of(RateObservation.newObservation(
                quote.protocol(),
                quote.product(),
                quote.side().name(),
                quote.rate().value(),
                quote.rate().convention().name(),
                quote.rate().methodology(),
                quote.lookbackWindow(),
                quote.sourceContract(),
                quote.provenance().chainId(),
                quote.provenance().blockNumber(),
                quote.provenance().blockHash(),
                quote.observedAt(),
                quote.sourceTimestamp(),
                true,
                false
        ));
    }

    public static MappedObservations mapQuotes(List<ProtocolRateQuote> quotes) {
        List<PriceObservation> prices = new ArrayList<>();
        List<RateObservation> rates = new ArrayList<>();
        for (ProtocolRateQuote quote : quotes) {
            if (quote.rateOptional().isEmpty()) {
                continue;
            }
            if (quote.side() == RateSide.PRICE
                    && quote.rate().convention() == RateConvention.SPOT_USD) {
                prices.addAll(toPriceObservations(quote));
            } else if (quote.side() != RateSide.PRICE) {
                rates.addAll(toRateObservations(quote));
            }
        }
        return new MappedObservations(List.copyOf(prices), List.copyOf(rates));
    }

    public record MappedObservations(List<PriceObservation> prices, List<RateObservation> rates) {
    }
}