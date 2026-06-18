package com.ethsimulator.config;

import com.ethsimulator.protocol.ProtocolAdapter;
import com.ethsimulator.protocol.ProtocolSourcesProperties;
import com.ethsimulator.protocol.aave.AaveAdapter;
import com.ethsimulator.protocol.chainlink.ChainlinkAdapter;
import com.ethsimulator.protocol.liquity.LiquityAdapter;
import com.ethsimulator.protocol.rpc.EthCallClient;
import com.ethsimulator.protocol.sky.SkyAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableConfigurationProperties(ProtocolSourcesProperties.class)
public class ProtocolAdapterConfig {

    @Bean
    public List<ProtocolAdapter> protocolAdapters(
            ProtocolSourcesProperties properties,
            EthSimulatorProperties ethSimulatorProperties,
            ObjectProvider<Web3j> web3jProvider,
            Clock clock
    ) {
        if (!StringUtils.hasText(properties.getChainlink().getAddress())) {
            properties.getChainlink().setAddress(ethSimulatorProperties.getChainlinkEthUsdFeed());
        }

        Web3j web3j = web3jProvider.getIfAvailable();
        if (web3j == null) {
            return List.of();
        }
        EthCallClient ethCallClient = new EthCallClient(web3j);

        return List.of(
                new ChainlinkAdapter(properties, ethCallClient, clock),
                new SkyAdapter(properties, ethCallClient, clock),
                new LiquityAdapter(properties, ethCallClient, clock),
                new AaveAdapter(properties, ethCallClient, clock)
        );
    }
}