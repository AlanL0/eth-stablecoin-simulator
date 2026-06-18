package com.ethsimulator.protocol.chainlink;

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
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ChainlinkAdapter implements ProtocolAdapter {

    public static final String PROTOCOL_ID = "chainlink";

    private final ProtocolSourcesProperties properties;
    private final EthCallClient ethCallClient;
    private final Clock clock;

    public ChainlinkAdapter(
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
        return properties.getChainlink().isEnabled() && StringUtils.hasText(properties.getChainlink().getAddress());
    }

    @Override
    public List<ProtocolRateQuote> fetchQuotes() {
        if (!enabled()) {
            return List.of();
        }

        EthBlockHeader block = ethCallClient.latestBlock();
        BlockProvenance provenance = provenance(block);
        Instant observedAt = clock.instant();
        String feed = properties.getChainlink().getAddress();

        EthCallResult latest = ethCallClient.call(feed, ProtocolAbi.LATEST_ROUND_DATA, block.number());
        if (latest.reverted()) {
            return List.of(unavailable(feed, provenance, observedAt, latest.errorMessage(), true));
        }

        BigInteger roundId = AbiDecoder.uint256(latest.returnData(), ProtocolAbi.LATEST_ROUND_DATA, 0);
        BigInteger answer = AbiDecoder.int256(latest.returnData(), ProtocolAbi.LATEST_ROUND_DATA, 1);
        BigInteger updatedAt = AbiDecoder.uint256(latest.returnData(), ProtocolAbi.LATEST_ROUND_DATA, 3);
        BigInteger answeredInRound = AbiDecoder.uint256(latest.returnData(), ProtocolAbi.LATEST_ROUND_DATA, 4);

        if (answer.signum() <= 0 || updatedAt.signum() <= 0 || answeredInRound.compareTo(roundId) < 0) {
            return List.of(unavailable(feed, provenance, observedAt, "incomplete Chainlink round", false));
        }

        EthCallResult decimalsResult = ethCallClient.call(feed, ProtocolAbi.DECIMALS, block.number());
        if (decimalsResult.reverted()) {
            return List.of(unavailable(feed, provenance, observedAt, decimalsResult.errorMessage(), true));
        }
        int decimals = AbiDecoder.uint8(decimalsResult.returnData(), ProtocolAbi.DECIMALS);

        Instant sourceTimestamp = Instant.ofEpochSecond(updatedAt.longValue());
        boolean stale = Duration.between(sourceTimestamp, block.timestamp()).getSeconds()
                > properties.getChainlink().getStaleThresholdSeconds();

        List<ProtocolRateQuote> quotes = new ArrayList<>();
        quotes.add(ProtocolRateQuote.available(
                PROTOCOL_ID,
                "ETH/USD",
                RateSide.PRICE,
                AnnualizedRate.of(
                        RateMath.chainlinkAnswerToUsd(answer, decimals),
                        RateConvention.SPOT_USD,
                        "chainlink.latestRoundData.answer / 10^decimals"
                ),
                "latest",
                feed,
                provenance,
                observedAt,
                sourceTimestamp,
                stale
        ));

        if (roundId.signum() > 1) {
            EthCallResult historical = ethCallClient.call(
                    feed,
                    ProtocolAbi.getRoundData(roundId.subtract(BigInteger.ONE).longValue()),
                    block.number()
            );
            if (!historical.reverted()) {
                BigInteger historicalAnswer = AbiDecoder.int256(historical.returnData(), ProtocolAbi.GET_ROUND_DATA, 1);
                BigInteger historicalUpdatedAt = AbiDecoder.uint256(historical.returnData(), ProtocolAbi.GET_ROUND_DATA, 3);
                if (historicalAnswer.signum() > 0 && historicalUpdatedAt.signum() > 0) {
                    quotes.add(ProtocolRateQuote.available(
                            PROTOCOL_ID,
                            "ETH/USD",
                            RateSide.PRICE,
                            AnnualizedRate.of(
                                    RateMath.chainlinkAnswerToUsd(historicalAnswer, decimals),
                                    RateConvention.SPOT_USD,
                                    "chainlink.getRoundData(roundId-1).answer / 10^decimals"
                            ),
                            "round:" + roundId.subtract(BigInteger.ONE),
                            feed,
                            provenance,
                            observedAt,
                            Instant.ofEpochSecond(historicalUpdatedAt.longValue()),
                            true
                    ));
                }
            }
        }

        return List.copyOf(quotes);
    }

    private ProtocolRateQuote unavailable(
            String feed,
            BlockProvenance provenance,
            Instant observedAt,
            String reason,
            boolean reverted
    ) {
        return ProtocolRateQuote.unavailable(
                PROTOCOL_ID,
                "ETH/USD",
                RateSide.PRICE,
                reason,
                "latest",
                feed,
                provenance,
                observedAt,
                reverted
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