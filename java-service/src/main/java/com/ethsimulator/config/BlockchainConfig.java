package com.ethsimulator.config;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.blockchain.Erc20BalanceReader;
import com.ethsimulator.blockchain.TransferEventFetcher;
import com.ethsimulator.blockchain.UnavailableChainlinkEthUsdReader;
import com.ethsimulator.blockchain.UnavailableErc20BalanceReader;
import com.ethsimulator.blockchain.UnavailableTransferEventFetcher;
import com.ethsimulator.blockchain.Web3jChainlinkEthUsdReader;
import com.ethsimulator.blockchain.Web3jErc20BalanceReader;
import com.ethsimulator.blockchain.Web3jTransferEventFetcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class BlockchainConfig {

    @Bean
    public Web3j web3j(Environment environment) {
        String rpcUrl = resolveRpcUrl(environment);
        if (!StringUtils.hasText(rpcUrl)) {
            return null;
        }
        return Web3j.build(new HttpService(rpcUrl.trim()));
    }

    @Bean
    public ChainlinkEthUsdReader chainlinkEthUsdReader(
            ObjectProvider<Web3j> web3jProvider,
            EthSimulatorProperties properties
    ) {
        Web3j web3j = web3jProvider.getIfAvailable();
        if (web3j != null && StringUtils.hasText(properties.getChainlinkEthUsdFeed())) {
            return new Web3jChainlinkEthUsdReader(web3j, properties.getChainlinkEthUsdFeed());
        }
        return new UnavailableChainlinkEthUsdReader();
    }

    @Bean
    public Erc20BalanceReader erc20BalanceReader(ObjectProvider<Web3j> web3jProvider) {
        Web3j web3j = web3jProvider.getIfAvailable();
        if (web3j != null) {
            return new Web3jErc20BalanceReader(web3j);
        }
        return new UnavailableErc20BalanceReader();
    }

    @Bean
    public TransferEventFetcher transferEventFetcher(ObjectProvider<Web3j> web3jProvider) {
        Web3j web3j = web3jProvider.getIfAvailable();
        if (web3j != null) {
            return new Web3jTransferEventFetcher(web3j);
        }
        return new UnavailableTransferEventFetcher();
    }

    static String resolveRpcUrl(Environment environment) {
        String rpcUrl = environment.getProperty("eth-simulator.eth-rpc-url");
        if (!StringUtils.hasText(rpcUrl)) {
            rpcUrl = environment.getProperty("ETH_RPC_URL");
        }
        return rpcUrl;
    }
}