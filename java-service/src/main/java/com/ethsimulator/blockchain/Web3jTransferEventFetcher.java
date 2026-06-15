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
import org.web3j.protocol.core.methods.request.Filter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    static final String TRANSFER_TOPIC = EventEncoder.encode(TRANSFER_EVENT);

    private final Web3j web3j;
    private final int lookbackBlocks;
    private final int maxEventsPerWallet;

    public Web3jTransferEventFetcher(Web3j web3j, int lookbackBlocks, int maxEventsPerWallet) {
        this.web3j = web3j;
        this.lookbackBlocks = Math.max(1, lookbackBlocks);
        this.maxEventsPerWallet = Math.max(1, maxEventsPerWallet);
    }

    @Override
    public List<TransferEventRecord> fetchTransferEvents(String walletAddress) {
        String normalizedWallet = walletAddress.toLowerCase(Locale.ROOT);
        String walletTopic = addressToTopic(normalizedWallet);
        Map<String, TransferEventRecord> deduped = new LinkedHashMap<>();
        Map<Long, Instant> blockTimestamps = new HashMap<>();

        DefaultBlockParameter fromBlock = resolveFromBlock();
        DefaultBlockParameter toBlock = DefaultBlockParameterName.LATEST;

        for (TokenAllowlist.TokenEntry token : TokenAllowlist.all()) {
            if (deduped.size() >= maxEventsPerWallet) {
                break;
            }
            collectLogs(
                    token,
                    fromBlock,
                    toBlock,
                    walletTopic,
                    normalizedWallet,
                    blockTimestamps,
                    deduped,
                    WalletTransferDirection.SENT
            );
            if (deduped.size() >= maxEventsPerWallet) {
                break;
            }
            collectLogs(
                    token,
                    fromBlock,
                    toBlock,
                    walletTopic,
                    normalizedWallet,
                    blockTimestamps,
                    deduped,
                    WalletTransferDirection.RECEIVED
            );
        }

        return List.copyOf(deduped.values());
    }

    @Override
    public String source() {
        return "chain";
    }

    enum WalletTransferDirection {
        SENT,
        RECEIVED
    }

    static EthFilter buildTransferFilter(
            DefaultBlockParameter fromBlock,
            DefaultBlockParameter toBlock,
            String contractAddress,
            String walletTopic,
            WalletTransferDirection direction
    ) {
        EthFilter filter = new EthFilter(fromBlock, toBlock, contractAddress);
        filter.addSingleTopic(TRANSFER_TOPIC);
        if (direction == WalletTransferDirection.SENT) {
            filter.addSingleTopic(walletTopic);
            filter.addNullTopic();
        } else {
            filter.addNullTopic();
            filter.addSingleTopic(walletTopic);
        }
        return filter;
    }

    private void collectLogs(
            TokenAllowlist.TokenEntry token,
            DefaultBlockParameter fromBlock,
            DefaultBlockParameter toBlock,
            String walletTopic,
            String normalizedWallet,
            Map<Long, Instant> blockTimestamps,
            Map<String, TransferEventRecord> deduped,
            WalletTransferDirection direction
    ) {
        try {
            EthFilter filter = buildTransferFilter(
                    fromBlock,
                    toBlock,
                    token.contractAddress(),
                    walletTopic,
                    direction
            );

            EthLog response = web3j.ethGetLogs(filter).send();
            if (response.hasError()) {
                log.warn("eth_getLogs failed for {}: {}", token.symbol(), response.getError().getMessage());
                return;
            }

            for (EthLog.LogResult<?> logResult : response.getLogs()) {
                if (deduped.size() >= maxEventsPerWallet) {
                    return;
                }
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

                String key = logEntry.getTransactionHash().toLowerCase(Locale.ROOT) + ":" + logEntry.getLogIndex();
                deduped.putIfAbsent(
                        key,
                        new TransferEventRecord(
                                token.symbol(),
                                logEntry.getTransactionHash(),
                                logEntry.getLogIndex().intValue(),
                                from,
                                to,
                                amount,
                                blockNumber,
                                occurredAt
                        )
                );
            }
        } catch (Exception ex) {
            log.warn("Transfer log fetch failed for {}: {}", token.symbol(), ex.getMessage());
        }
    }

    private DefaultBlockParameter resolveFromBlock() {
        try {
            BigInteger latest = web3j.ethBlockNumber().send().getBlockNumber();
            BigInteger from = latest.subtract(BigInteger.valueOf(lookbackBlocks));
            if (from.signum() < 0) {
                from = BigInteger.ZERO;
            }
            return DefaultBlockParameter.valueOf(from);
        } catch (Exception ex) {
            log.warn("Failed to resolve audit from-block, using LATEST only: {}", ex.getMessage());
            return DefaultBlockParameterName.LATEST;
        }
    }

    static String addressToTopic(String normalizedAddress) {
        String hex = normalizedAddress.startsWith("0x") ? normalizedAddress.substring(2) : normalizedAddress;
        return "0x" + "0".repeat(24) + hex;
    }

    static String topicValue(Filter.FilterTopic<?> topic) {
        if (topic instanceof Filter.SingleTopic singleTopic) {
            return singleTopic.getValue();
        }
        return topic.getValue().toString();
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