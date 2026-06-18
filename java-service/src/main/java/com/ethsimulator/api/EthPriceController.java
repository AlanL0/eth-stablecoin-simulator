package com.ethsimulator.api;

import com.ethsimulator.market.EthPriceQuote;
import com.ethsimulator.market.EthPriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/price")
public class EthPriceController {

    private final EthPriceService ethPriceService;

    public EthPriceController(EthPriceService ethPriceService) {
        this.ethPriceService = ethPriceService;
    }

    @GetMapping("/eth")
    public EthPriceResponse ethPrice() {
        EthPriceQuote quote = ethPriceService.currentPrice();
        return new EthPriceResponse(
                quote.priceUsd(),
                quote.source().name().toLowerCase(),
                quote.observedAt().toString(),
                quote.stale(),
                quote.degraded()
        );
    }

    public record EthPriceResponse(BigDecimal priceUsd, String source, String observedAt, boolean stale, boolean degraded) {
    }
}