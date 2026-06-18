package com.ethsimulator.charts;

import com.ethsimulator.util.FinancialMath;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.ethsimulator.charts.ChartBuilders.EthHistoryPoint;

@Service
public class EthPriceHistoryService {

    private static final List<EthHistoryPoint> SEED_HISTORY = List.of(
            new EthHistoryPoint("2026-06-01T00:00:00Z", FinancialMath.bd("3521.445678901234567890")),
            new EthHistoryPoint("2026-06-02T00:00:00Z", FinancialMath.bd("3610.12")),
            new EthHistoryPoint("2026-06-03T00:00:00Z", FinancialMath.bd("3688.99")),
            new EthHistoryPoint("2026-06-04T00:00:00Z", FinancialMath.bd("3722.50")),
            new EthHistoryPoint("2026-06-05T00:00:00Z", FinancialMath.bd("3795.333333333333333333")),
            new EthHistoryPoint("2026-06-06T00:00:00Z", FinancialMath.bd("3812.00")),
            new EthHistoryPoint("2026-06-07T00:00:00Z", FinancialMath.bd("3770.88")),
            new EthHistoryPoint("2026-06-08T00:00:00Z", FinancialMath.bd("3800.00")),
            new EthHistoryPoint("2026-06-09T00:00:00Z", FinancialMath.bd("3850.125"))
    );

    public List<EthHistoryPoint> seedHistory() {
        return SEED_HISTORY;
    }

    public BigDecimal terminalExactPrice() {
        return SEED_HISTORY.get(SEED_HISTORY.size() - 1).priceUsd();
    }
}