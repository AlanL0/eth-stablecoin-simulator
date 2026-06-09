package com.ethsimulator.blockchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public class Web3jChainlinkEthUsdReader implements ChainlinkEthUsdReader {

    private static final Logger log = LoggerFactory.getLogger(Web3jChainlinkEthUsdReader.class);
    private static final int CHAINLINK_USD_DECIMALS = 8;

    private static final Function LATEST_ROUND_DATA = new Function(
            "latestRoundData",
            List.of(),
            List.of(
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Int256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    }
            )
    );

    private final Web3j web3j;
    private final String feedAddress;

    public Web3jChainlinkEthUsdReader(Web3j web3j, String feedAddress) {
        this.web3j = web3j;
        this.feedAddress = feedAddress;
    }

    @Override
    public Optional<BigDecimal> readPriceUsd() {
        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, feedAddress, FunctionEncoder.encode(LATEST_ROUND_DATA)),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError() || response.getValue() == null) {
                log.warn("Chainlink eth_call failed: {}", response.hasError() ? response.getError().getMessage() : "empty");
                return Optional.empty();
            }

            List<org.web3j.abi.datatypes.Type> decoded = FunctionReturnDecoder.decode(
                    response.getValue(),
                    LATEST_ROUND_DATA.getOutputParameters()
            );
            if (decoded.size() < 2) {
                return Optional.empty();
            }

            BigInteger answer = (BigInteger) decoded.get(1).getValue();
            if (answer.signum() <= 0) {
                return Optional.empty();
            }

            BigDecimal price = new BigDecimal(answer).movePointLeft(CHAINLINK_USD_DECIMALS)
                    .setScale(2, RoundingMode.HALF_UP);
            return Optional.of(price);
        } catch (Exception ex) {
            log.warn("Chainlink price read failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}