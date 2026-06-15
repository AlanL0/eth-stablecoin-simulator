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
import okhttp3.OkHttpClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.concurrent.TimeUnit;

@Configuration
public class BlockchainConfig {

    @Bean
    public Web3j web3j(Environment environment, EthSimulatorProperties properties) {
        String rpcUrl = resolveRpcUrl(environment);
        if (!StringUtils.hasText(rpcUrl)) {
            return null;
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(properties.getRpcConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getRpcReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getRpcReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
        return Web3j.build(new HttpService(rpcUrl.trim(), client));
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
    public TransferEventFetcher transferEventFetcher(
            ObjectProvider<Web3j> web3jProvider,
            EthSimulatorProperties properties
    ) {
        Web3j web3j = web3jProvider.getIfAvailable();
        if (web3j != null) {
            return new Web3jTransferEventFetcher(
                    web3j,
                    properties.getAuditLookbackBlocks(),
                    properties.getAuditMaxEventsPerWallet()
            );
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