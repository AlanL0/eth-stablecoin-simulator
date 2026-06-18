package com.ethsimulator.protocol.liquity;

import com.ethsimulator.protocol.AnnualizedRate;
import com.ethsimulator.protocol.BlockProvenance;
import com.ethsimulator.protocol.ProtocolAdapter;
import com.ethsimulator.protocol.ProtocolRateQuote;
import com.ethsimulator.protocol.ProtocolSourcesProperties;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.protocol.RateMath;
import com.ethsimulator.protocol.RateSide;
import com.ethsimulator.protocol.abi.AbiDecoder;
import com.ethsimulator.protocol.abi.ProtocolAbi;
import com.ethsimulator.protocol.rpc.EthBlockHeader;
import com.ethsimulator.protocol.rpc.EthCallClient;
import com.ethsimulator.protocol.rpc.EthCallResult;
import com.ethsimulator.protocol.rpc.TransferLog;
import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LiquityAdapter implements ProtocolAdapter {

    public static final String PROTOCOL_ID = "liquity";

    private final ProtocolSourcesProperties properties;
    private final EthCallClient ethCallClient;
    private final Clock clock;

    public LiquityAdapter(
            ProtocolSourcesProperties properties,
            EthCallClient ethCallClient,
            Clock clock
    ) {
        this.properties = properties;
        this.ethCallClient = ethCallClient;
        this.clock = clock;
    }

    @Override
    public String protocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public boolean enabled() {
        return properties.getLiquity().isEnabled();
    }

    @Override
    public List<ProtocolRateQuote> fetchQuotes() {
        if (!enabled()) {
            return List.of();
        }

        EthBlockHeader block = ethCallClient.latestBlock();
        BlockProvenance provenance = provenance(block);
        Instant observedAt = clock.instant();
        List<ProtocolRateQuote> quotes = new ArrayList<>();

        quotes.add(fetchBorrowQuote(block, provenance, observedAt));
        quotes.add(fetchSavingsQuote(block, provenance, observedAt));
        return List.copyOf(quotes);
    }

    private ProtocolRateQuote fetchBorrowQuote(
            EthBlockHeader block,
            BlockProvenance provenance,
            Instant observedAt
    ) {
        String activePool = properties.getLiquity().getWethActivePool();
        EthCallResult weighted = ethCallClient.call(activePool, ProtocolAbi.AGG_WEIGHTED_DEBT_SUM, block.number());
        EthCallResult recorded = ethCallClient.call(activePool, ProtocolAbi.AGG_RECORDED_DEBT, block.number());
        if (weighted.reverted() || recorded.reverted()) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "WETH-branch",
                    RateSide.BORROW,
                    "activePool debt aggregates unavailable",
                    "latest",
                    activePool,
                    provenance,
                    observedAt,
                    true
            );
        }

        BigInteger weightedSum = AbiDecoder.uint256(weighted.returnData(), ProtocolAbi.AGG_WEIGHTED_DEBT_SUM, 0);
        BigInteger recordedDebt = AbiDecoder.uint256(recorded.returnData(), ProtocolAbi.AGG_RECORDED_DEBT, 0);
        if (recordedDebt.signum() <= 0) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "WETH-branch",
                    RateSide.BORROW,
                    "no recorded debt for weighted APR",
                    "latest",
                    activePool,
                    provenance,
                    observedAt,
                    false
            );
        }

        BigInteger avgWad = weightedSum.multiply(BigInteger.TEN.pow(18)).divide(recordedDebt);
        return ProtocolRateQuote.available(
                PROTOCOL_ID,
                "WETH-branch",
                RateSide.BORROW,
                AnnualizedRate.of(
                        RateMath.wadRatioToDecimalRate(avgWad),
                        RateConvention.APR_SIMPLE,
                        "activePool.aggWeightedDebtSum / activePool.aggRecordedDebt (WAD ratio)"
                ),
                "latest",
                activePool,
                provenance,
                observedAt,
                block.timestamp(),
                false
        );
    }

    private ProtocolRateQuote fetchSavingsQuote(
            EthBlockHeader block,
            BlockProvenance provenance,
            Instant observedAt
    ) {
        int lookbackDays = properties.getLiquity().getSavingsLookbackDays();
        String activePool = properties.getLiquity().getWethActivePool();
        String stabilityPool = properties.getLiquity().getWethStabilityPool();
        String boldToken = properties.getLiquity().getBoldToken();

        BigInteger lookbackBlocks = BigInteger.valueOf((long) lookbackDays * 7200L);
        BigInteger fromBlock = block.number().subtract(lookbackBlocks).max(BigInteger.ONE);

        List<TransferLog> transfers;
        try {
            transfers = ethCallClient.transferLogs(
                    boldToken,
                    activePool,
                    stabilityPool,
                    fromBlock,
                    block.number()
            );
        } catch (RuntimeException ex) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "WETH-stability-pool",
                    RateSide.SAVINGS,
                    "bold transfer log scan failed: " + ex.getMessage(),
                    lookbackDays + "d",
                    stabilityPool,
                    provenance,
                    observedAt,
                    true
            );
        }

        BigInteger totalRewards = transfers.stream()
                .map(TransferLog::value)
                .reduce(BigInteger.ZERO, BigInteger::add);

        EthCallResult depositsResult = ethCallClient.call(
                stabilityPool,
                ProtocolAbi.GET_TOTAL_BOLD_DEPOSITS,
                block.number()
        );
        if (depositsResult.reverted()) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "WETH-stability-pool",
                    RateSide.SAVINGS,
                    "stabilityPool.getTotalBoldDeposits unavailable",
                    lookbackDays + "d",
                    stabilityPool,
                    provenance,
                    observedAt,
                    true
            );
        }

        BigInteger depositsRaw = AbiDecoder.uint256(depositsResult.returnData(), ProtocolAbi.GET_TOTAL_BOLD_DEPOSITS, 0);
        if (depositsRaw.signum() <= 0 || totalRewards.signum() <= 0) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "WETH-stability-pool",
                    RateSide.SAVINGS,
                    "insufficient BOLD SP deposits or realized rewards in lookback window",
                    lookbackDays + "d",
                    stabilityPool,
                    provenance,
                    observedAt,
                    false
            );
        }

        BigDecimal rewards = RateMath.tokenAmount(totalRewards, 18);
        BigDecimal deposits = RateMath.tokenAmount(depositsRaw, 18);
        BigDecimal cumulativeReturn = FinancialMath.divide(rewards, deposits, FinancialMath.RATE_SCALE);

        return ProtocolRateQuote.available(
                PROTOCOL_ID,
                "WETH-stability-pool",
                RateSide.SAVINGS,
                AnnualizedRate.of(
                        RateMath.trailingRealizedApr(cumulativeReturn, lookbackDays, 365),
                        RateConvention.APR_SIMPLE,
                        "sum(BOLD Transfer activePool→stabilityPool over " + lookbackDays
                                + "d) / getTotalBoldDeposits, annualized; excludes liquidation collateral gains"
                ),
                lookbackDays + "d",
                stabilityPool,
                provenance,
                observedAt,
                block.timestamp(),
                false
        );
    }

    private BlockProvenance provenance(EthBlockHeader block) {
        return new BlockProvenance(
                properties.getChainId(),
                block.number().longValue(),
                block.hash(),
                block.timestamp()
        );
    }
}