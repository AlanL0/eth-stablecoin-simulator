package com.ethsimulator.blockchain;

import com.ethsimulator.config.TokenAllowlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
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

public class Web3jErc20BalanceReader implements Erc20BalanceReader {

    private static final Logger log = LoggerFactory.getLogger(Web3jErc20BalanceReader.class);

    private final Web3j web3j;

    public Web3jErc20BalanceReader(Web3j web3j) {
        this.web3j = web3j;
    }

    @Override
    public Optional<BigDecimal> readBalance(String walletAddress, TokenAllowlist.TokenEntry token) {
        Function balanceOf = new Function(
                "balanceOf",
                List.of(new Address(walletAddress)),
                List.of(new TypeReference<Uint256>() {
                })
        );

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            token.contractAddress(),
                            FunctionEncoder.encode(balanceOf)
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError() || response.getValue() == null) {
                log.warn(
                        "ERC-20 balanceOf failed for {}: {}",
                        token.symbol(),
                        response.hasError() ? response.getError().getMessage() : "empty"
                );
                return Optional.empty();
            }

            List<org.web3j.abi.datatypes.Type> decoded = FunctionReturnDecoder.decode(
                    response.getValue(),
                    balanceOf.getOutputParameters()
            );
            if (decoded.isEmpty()) {
                return Optional.empty();
            }

            BigInteger raw = (BigInteger) decoded.getFirst().getValue();
            BigDecimal balance = new BigDecimal(raw).movePointLeft(token.decimals())
                    .setScale(token.decimals(), RoundingMode.HALF_UP);
            return Optional.of(balance);
        } catch (Exception ex) {
            log.warn("ERC-20 balance read failed for {}: {}", token.symbol(), ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String source() {
        return "chain";
    }
}