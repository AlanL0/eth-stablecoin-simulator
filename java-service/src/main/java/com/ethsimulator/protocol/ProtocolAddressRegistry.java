package com.ethsimulator.protocol;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class ProtocolAddressRegistry {

    private final ProtocolSourcesProperties properties;

    public ProtocolAddressRegistry(ProtocolSourcesProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateEnabledAddresses() {
        validateSource("chainlink", properties.getChainlink().isEnabled(), properties.getChainlink().getAddress());
        if (properties.getSky().isEnabled()) {
            validateAddress("sky.chainlog", properties.getSky().getChainlog());
            validateAddress("sky.jug", properties.getSky().getJug());
            validateAddress("sky.vat", properties.getSky().getVat());
            validateAddress("sky.susds", properties.getSky().getSusds());
        }
        if (properties.getLiquity().isEnabled()) {
            validateAddress("liquity.weth-active-pool", properties.getLiquity().getWethActivePool());
            validateAddress("liquity.weth-stability-pool", properties.getLiquity().getWethStabilityPool());
            validateAddress("liquity.bold-token", properties.getLiquity().getBoldToken());
        }
        if (properties.getAave().isEnabled()) {
            validateAddress("aave.pool", properties.getAave().getPool());
            validateAddress("aave.gho", properties.getAave().getGho());
            validateAddress("aave.sgho", properties.getAave().getSgho());
        }
    }

    private static void validateSource(String name, boolean enabled, String address) {
        if (enabled && address != null && !address.isBlank()) {
            validateAddress(name, address);
        }
    }

    private static void validateAddress(String name, String address) {
        if (!EthAddressValidator.isWellFormed(address)) {
            throw new IllegalStateException("Malformed protocol address for " + name + ": " + address);
        }
    }
}