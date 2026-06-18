package com.ethsimulator.protocol.aave;

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

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AaveAdapter implements ProtocolAdapter {

    public static final String PROTOCOL_ID = "aave_gho";

    private final ProtocolSourcesProperties properties;
    private final EthCallClient ethCallClient;
    private final Clock clock;

    public AaveAdapter(
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
        return properties.getAave().isEnabled();
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
        String pool = properties.getAave().getPool();
        String gho = properties.getAave().getGho();
        EthCallResult reserveData = ethCallClient.call(pool, ProtocolAbi.getReserveData(gho), block.number());
        if (reserveData.reverted()) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "GHO",
                    RateSide.BORROW,
                    "pool.getReserveData(GHO) unavailable",
                    "latest",
                    pool,
                    provenance,
                    observedAt,
                    true
            );
        }

        BigInteger variableBorrowRate = AbiDecoder.aaveVariableBorrowRate(reserveData.returnData());
        return ProtocolRateQuote.available(
                PROTOCOL_ID,
                "GHO",
                RateSide.BORROW,
                AnnualizedRate.of(
                        RateMath.rayAnnualToDecimalRate(variableBorrowRate),
                        RateConvention.APR_SIMPLE,
                        "aave.pool.getReserveData(GHO).variableBorrowRate / RAY (annualized ray rate)"
                ),
                "latest",
                pool,
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
        String sgho = properties.getAave().getSgho();
        EthCallResult targetRate = ethCallClient.call(sgho, ProtocolAbi.TARGET_RATE, block.number());
        if (targetRate.reverted()) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "sGHO",
                    RateSide.SAVINGS,
                    "sGHO.targetRate() unavailable",
                    "latest",
                    sgho,
                    provenance,
                    observedAt,
                    true
            );
        }

        BigInteger rateRay = AbiDecoder.uint256(targetRate.returnData(), ProtocolAbi.TARGET_RATE, 0);
        return ProtocolRateQuote.available(
                PROTOCOL_ID,
                "sGHO",
                RateSide.SAVINGS,
                AnnualizedRate.of(
                        RateMath.rayAnnualToDecimalRate(rateRay),
                        RateConvention.APR_SIMPLE,
                        "sGHO ERC-4626 targetRate() / RAY"
                ),
                "latest",
                sgho,
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