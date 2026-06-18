package com.ethsimulator.protocol.sky;

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

public class SkyAdapter implements ProtocolAdapter {

    public static final String PROTOCOL_ID = "sky";

    private final ProtocolSourcesProperties properties;
    private final EthCallClient ethCallClient;
    private final Clock clock;

    public SkyAdapter(
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
        return properties.getSky().isEnabled();
    }

    @Override
    public List<ProtocolRateQuote> fetchQuotes() {
        if (!enabled()) {
            return List.of();
        }
        return fetchQuotesAtBlock(ethCallClient.latestBlock().number().longValue());
    }

    @Override
    public List<ProtocolRateQuote> fetchQuotesAtBlock(long blockNumber) {
        if (!enabled()) {
            return List.of();
        }
        EthBlockHeader block = ethCallClient.blockAt(BigInteger.valueOf(blockNumber));
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
        String jug = resolveJug(block.number());
        EthCallResult baseResult = ethCallClient.call(jug, ProtocolAbi.JUG_BASE, block.number());
        EthCallResult ilkResult = ethCallClient.call(
                jug,
                ProtocolAbi.jugIlks(properties.getSky().getBorrowIlk()),
                block.number()
        );
        if (baseResult.reverted() || ilkResult.reverted()) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "USDS-vault",
                    RateSide.BORROW,
                    "jug/vat borrow read failed",
                    "latest",
                    jug,
                    provenance,
                    observedAt,
                    true
            );
        }

        BigInteger base = AbiDecoder.uint256(baseResult.returnData(), ProtocolAbi.JUG_BASE, 0);
        BigInteger duty = AbiDecoder.uint256(ilkResult.returnData(), ProtocolAbi.JUG_ILKS, 0);
        BigInteger perSecond = base.multiply(duty).divide(BigInteger.TEN.pow(27));

        AnnualizedRate simple = AnnualizedRate.of(
                RateMath.makerRayPerSecondMultiplierToSimpleApr(perSecond),
                RateConvention.APR_SIMPLE,
                "jug.(duty*base/RAY) linearized to APR_SIMPLE"
        );
        AnnualizedRate effective = AnnualizedRate.of(
                RateMath.makerRayPerSecondMultiplierToEffectiveApr(perSecond),
                RateConvention.APR_EFFECTIVE,
                "jug.(duty*base/RAY)^(secondsPerYear)-1"
        );

        return ProtocolRateQuote.available(
                PROTOCOL_ID,
                "USDS-vault",
                RateSide.BORROW,
                effective,
                "latest;simple=" + simple.value(),
                jug,
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
        String susds = properties.getSky().getSusds();
        EthCallResult ssrResult = ethCallClient.call(susds, ProtocolAbi.SUSDS_SSR, block.number());
        if (ssrResult.reverted()) {
            return ProtocolRateQuote.unavailable(
                    PROTOCOL_ID,
                    "sUSDS",
                    RateSide.SAVINGS,
                    "sUSDS.ssr() unavailable",
                    "latest",
                    susds,
                    provenance,
                    observedAt,
                    true
            );
        }

        BigInteger ssr = AbiDecoder.uint256(ssrResult.returnData(), ProtocolAbi.SUSDS_SSR, 0);
        AnnualizedRate effective = AnnualizedRate.of(
                RateMath.makerRayPerSecondMultiplierToEffectiveApr(ssr),
                RateConvention.APR_EFFECTIVE,
                "sUSDS.ssr RAY per-second multiplier compounded to APR_EFFECTIVE"
        );
        AnnualizedRate simple = AnnualizedRate.of(
                RateMath.makerRayPerSecondMultiplierToSimpleApr(ssr),
                RateConvention.APR_SIMPLE,
                "sUSDS.ssr linearized to APR_SIMPLE"
        );

        return ProtocolRateQuote.available(
                PROTOCOL_ID,
                "sUSDS",
                RateSide.SAVINGS,
                effective,
                "latest;simple=" + simple.value(),
                susds,
                provenance,
                observedAt,
                block.timestamp(),
                false
        );
    }

    private String resolveJug(BigInteger blockNumber) {
        String configured = properties.getSky().getJug();
        String chainlog = properties.getSky().getChainlog();
        EthCallResult resolved = ethCallClient.call(chainlog, ProtocolAbi.chainlogGetAddress("MCD_JUG"), blockNumber);
        if (resolved.reverted()) {
            return configured;
        }
        return AbiDecoder.address(resolved.returnData(), ProtocolAbi.CHAINLOG_GET_ADDRESS);
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