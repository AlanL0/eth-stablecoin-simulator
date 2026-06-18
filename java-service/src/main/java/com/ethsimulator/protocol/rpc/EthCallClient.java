package com.ethsimulator.protocol.rpc;

import com.ethsimulator.protocol.EthAddressValidator;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Block-pinned read-only RPC helper for protocol adapters.
 */
public class EthCallClient {

    private static final String TRANSFER_TOPIC = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private final Web3j web3j;

    public EthCallClient(Web3j web3j) {
        this.web3j = web3j;
    }

    public EthBlockHeader latestBlock() {
        try {
            EthBlock.Block block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                    .send()
                    .getBlock();
            if (block == null) {
                throw new EthCallException("Latest block unavailable");
            }
            return toHeader(block);
        } catch (EthCallException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EthCallException("Failed to read latest block", ex);
        }
    }

    public EthBlockHeader blockAt(BigInteger blockNumber) {
        try {
            EthBlock.Block block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), false)
                    .send()
                    .getBlock();
            if (block == null) {
                throw new EthCallException("Block unavailable: " + blockNumber);
            }
            return toHeader(block);
        } catch (EthCallException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EthCallException("Failed to read block " + blockNumber, ex);
        }
    }

    public EthCallResult call(String contract, Function function, BigInteger blockNumber) {
        return call(contract, FunctionEncoder.encode(function), blockNumber);
    }

    public EthCallResult call(String contract, String encodedData, BigInteger blockNumber) {
        try {
            DefaultBlockParameter block = blockNumber == null
                    ? DefaultBlockParameterName.LATEST
                    : DefaultBlockParameter.valueOf(blockNumber);
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, contract, encodedData),
                    block
            ).send();
            if (response.hasError()) {
                return EthCallResult.revert(response.getError().getMessage());
            }
            String value = response.getValue();
            if (value == null || value.isBlank() || "0x".equalsIgnoreCase(value)) {
                return EthCallResult.revert("empty eth_call result");
            }
            return EthCallResult.success(value);
        } catch (Exception ex) {
            throw new EthCallException("eth_call failed for " + contract, ex);
        }
    }

    public List<TransferLog> transferLogs(
            String tokenAddress,
            String fromAddress,
            String toAddress,
            BigInteger fromBlock,
            BigInteger toBlock
    ) {
        try {
            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(fromBlock),
                    DefaultBlockParameter.valueOf(toBlock),
                    tokenAddress
            );
            filter.addSingleTopic(TRANSFER_TOPIC);
            filter.addOptionalTopics(
                    topicForAddress(fromAddress),
                    topicForAddress(toAddress)
            );
            EthLog ethLog = web3j.ethGetLogs(filter).send();
            if (ethLog.hasError()) {
                throw new EthCallException("eth_getLogs failed: " + ethLog.getError().getMessage());
            }
            List<TransferLog> logs = new ArrayList<>();
            for (EthLog.LogResult<?> result : ethLog.getLogs()) {
                Log log = (Log) result.get();
                if (log.getTopics() == null || log.getTopics().size() < 3 || log.getData() == null) {
                    continue;
                }
                logs.add(new TransferLog(
                        log.getAddress(),
                        addressFromTopic(log.getTopics().get(1)),
                        addressFromTopic(log.getTopics().get(2)),
                        hexToBigInteger(log.getData()),
                        log.getBlockNumber()
                ));
            }
            return logs;
        } catch (EthCallException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EthCallException("eth_getLogs failed", ex);
        }
    }

    private static EthBlockHeader toHeader(EthBlock.Block block) {
        return new EthBlockHeader(
                block.getNumber(),
                block.getHash(),
                Instant.ofEpochSecond(block.getTimestamp().longValue())
        );
    }

    private static String topicForAddress(String address) {
        String normalized = EthAddressValidator.normalize(address).substring(2);
        return "0x000000000000000000000000" + normalized;
    }

    private static String addressFromTopic(String topic) {
        if (topic == null || topic.length() < 42) {
            return "0x0000000000000000000000000000000000000000";
        }
        return ("0x" + topic.substring(topic.length() - 40)).toLowerCase(Locale.ROOT);
    }

    private static BigInteger hexToBigInteger(String hex) {
        String normalized = hex == null ? "0x0" : hex.trim();
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        if (normalized.isBlank()) {
            return BigInteger.ZERO;
        }
        return new BigInteger(normalized, 16);
    }
}