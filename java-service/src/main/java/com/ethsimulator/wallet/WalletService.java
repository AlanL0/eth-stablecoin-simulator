package com.ethsimulator.wallet;

import com.ethsimulator.blockchain.Erc20BalanceReader;
import com.ethsimulator.config.TokenAllowlist;
import com.ethsimulator.util.EvmAddressValidator;
import com.ethsimulator.util.UsdMath;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class WalletService {

    private static final List<String> ASSUMPTIONS = List.of(
            "Each stablecoin unit is valued at $1.00 USD (peg assumption).",
            "Only allowlisted Ethereum mainnet tokens are included: USDC, USDT, DAI, GHO.",
            "Balances are read from on-chain ERC-20 balanceOf calls."
    );

    private final Erc20BalanceReader balanceReader;
    private final Clock clock;

    public WalletService(Erc20BalanceReader balanceReader, Clock clock) {
        this.balanceReader = balanceReader;
        this.clock = clock;
    }

    public WalletStablecoinsResponse stablecoinBalances(String address) {
        String normalizedAddress = EvmAddressValidator.requireValid(address);
        Instant observedAt = clock.instant();
        List<StablecoinBalance> balances = new ArrayList<>();

        for (TokenAllowlist.TokenEntry token : TokenAllowlist.all()) {
            BigDecimal balance = balanceReader.readBalance(normalizedAddress, token)
                    .orElse(BigDecimal.ZERO);
            balances.add(new StablecoinBalance(
                    token.symbol(),
                    token.contractAddress(),
                    token.decimals(),
                    balance.stripTrailingZeros().toPlainString(),
                    UsdMath.roundUsdDouble(balance)
            ));
        }

        return new WalletStablecoinsResponse(
                normalizedAddress,
                balances,
                balanceReader.source(),
                observedAt.toString(),
                ASSUMPTIONS
        );
    }
}