package com.ethsimulator.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocolAddressRegistryTest {

    @Test
    void failsStartupValidationForMalformedEnabledAddress() {
        ProtocolSourcesProperties properties = new ProtocolSourcesProperties();
        properties.getAave().setSgho("not-an-address");

        ProtocolAddressRegistry registry = new ProtocolAddressRegistry(properties);
        assertThrows(IllegalStateException.class, registry::validateEnabledAddresses);
    }

    @Test
    void allowsDisabledSourceWithMalformedAddress() {
        ProtocolSourcesProperties properties = new ProtocolSourcesProperties();
        properties.getChainlink().setEnabled(false);
        properties.getChainlink().setAddress("");
        properties.getAave().setEnabled(false);
        properties.getAave().setSgho("not-an-address");

        ProtocolAddressRegistry registry = new ProtocolAddressRegistry(properties);
        assertDoesNotThrow(registry::validateEnabledAddresses);
    }
}