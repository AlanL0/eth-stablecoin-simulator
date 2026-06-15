package com.ethsimulator.blockchain;

import com.ethsimulator.config.TokenAllowlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Web3jTransferEventFetcher implements TransferEventFetcher {

    private static final Logger log = LoggerFactory.getLogger(Web3jTransferEventFetcher.class);

    private static final Event TRANSFER_EVENT = new Event(
            "Transfer",
            List.of(
                    new TypeReference<Address>(true) {
                    },
                    new TypeReference<Address>(true) {
                    },
                    new TypeReference<Uint256>(false) {
                    }
            )
    );

    private static final String TRANSFER_TOPIC = EventEncoder.encode(TRANSFER_EVENT);

    private final Web3j web3j;

    public Web3jTransferEventFetcher(Web3j web3j) {
        this.web3j = web3j;
    }

    @Override
    public List<TransferEventRecord> fetchTransferEvents(String walletAddress) {
        String normalizedWallet = walletAddress.toLowerCase(Locale.ROOT);
        List<TransferEventRecord> events = new ArrayList<>();
        Map<Long, Instant> blockTimestamps = new HashMap<>();

        for (TokenAllowlist.TokenEntry token : TokenAllowlist.all()) {
            try {
                EthFilter filter = new EthFilter(
                        DefaultBlockParameterName.EARLIEST,
                        DefaultBlockParameterName.LATEST,
                        token.contractAddress()
                );
                filter.addSingleTopic(TRANSFER_TOPIC);

                EthLog response = web3j.ethGetLogs(filter).send();
                if (response.hasError()) {
                    log.warn("eth_getLogs failed for {}: {}", token.symbol(), response.getError().getMessage());
                    continue;
                }

                for (EthLog.LogResult<?> logResult : response.getLogs()) {
                    Log logEntry = (Log) logResult.get();
                    if (logEntry.getTopics() == null || logEntry.getTopics().size() < 3) {
                        continue;
                    }

                    String from = topicToAddress(logEntry.getTopics().get(1));
                    String to = topicToAddress(logEntry.getTopics().get(2));
                    if (!from.equals(normalizedWallet) && !to.equals(normalizedWallet)) {
                        continue;
                    }

                    List<org.web3j.abi.datatypes.Type> decoded = FunctionReturnDecoder.decode(
                            logEntry.getData(),
                            TRANSFER_EVENT.getNonIndexedParameters()
                    );
                    if (decoded.isEmpty()) {
                        continue;
                    }

                    BigInteger rawAmount = (BigInteger) decoded.getFirst().getValue();
                    BigDecimal amount = new BigDecimal(rawAmount).movePointLeft(token.decimals())
                            .setScale(token.decimals(), RoundingMode.HALF_UP);
                    long blockNumber = logEntry.getBlockNumber().longValue();
                    Instant occurredAt = blockTimestamps.computeIfAbsent(blockNumber, this::blockTimestamp);

                    events.add(new TransferEventRecord(
                            token.symbol(),
                            logEntry.getTransactionHash(),
                            logEntry.getLogIndex().intValue(),
                            from,
                            to,
                            amount,
                            blockNumber,
                            occurredAt
                    ));
                }
            } catch (Exception ex) {
                log.warn("Transfer log fetch failed for {}: {}", token.symbol(), ex.getMessage());
            }
        }

        return events;
    }

    @Override
    public String source() {
        return "chain";
    }

    private String topicToAddress(String topic) {
        return new Address(topic).getValue().toLowerCase(Locale.ROOT);
    }

    private Instant blockTimestamp(long blockNumber) {
        try {
            EthBlock response = web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)),
                    false
            ).send();
            if (response.getBlock() != null && response.getBlock().getTimestamp() != null) {
                return Instant.ofEpochSecond(response.getBlock().getTimestamp().longValue());
            }
        } catch (Exception ex) {
            log.warn("Block timestamp lookup failed for {}: {}", blockNumber, ex.getMessage());
        }
        return Instant.EPOCH;
    }
}