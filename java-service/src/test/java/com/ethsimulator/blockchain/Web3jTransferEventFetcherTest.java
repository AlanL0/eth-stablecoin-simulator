package com.ethsimulator.blockchain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Web3jTransferEventFetcherTest {

    @Test
    void addressToTopicPadsWalletAddress() {
        String topic = Web3jTransferEventFetcher.addressToTopic(
                "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
        );
        assertThat(topic).isEqualTo(
                "0x000000000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045"
        );
    }
}