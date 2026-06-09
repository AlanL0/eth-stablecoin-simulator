package com.ethsimulator.config;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.blockchain.UnavailableChainlinkEthUsdReader;
import com.ethsimulator.blockchain.Web3jChainlinkEthUsdReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.Optional;

@Configuration
public class BlockchainConfig {

    @Bean
    public Optional<Web3j> web3jOptional(EthSimulatorProperties properties) {
        if (!StringUtils.hasText(properties.getEthRpcUrl())) {
            return Optional.empty();
        }
        return Optional.of(Web3j.build(new HttpService(properties.getEthRpcUrl())));
    }

    @Bean
    public ChainlinkEthUsdReader chainlinkEthUsdReader(
            Optional<Web3j> web3jOptional,
            EthSimulatorProperties properties
    ) {
        if (web3jOptional.isPresent() && StringUtils.hasText(properties.getChainlinkEthUsdFeed())) {
            return new Web3jChainlinkEthUsdReader(web3jOptional.get(), properties.getChainlinkEthUsdFeed());
        }
        return new UnavailableChainlinkEthUsdReader();
    }
}