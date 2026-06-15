package com.ethsimulator.blockchain;

import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Filter;

import static org.assertj.core.api.Assertions.assertThat;

class Web3jTransferEventFetcherTest {

    private static final String WALLET_TOPIC =
            "0x000000000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045";

    @Test
    void addressToTopicPadsWalletAddress() {
        String topic = Web3jTransferEventFetcher.addressToTopic(
                "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
        );
        assertThat(topic).isEqualTo(WALLET_TOPIC);
    }

    @Test
    void senderFilterSetsTopic1Only() {
        EthFilter filter = Web3jTransferEventFetcher.buildTransferFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                "0xcontract",
                WALLET_TOPIC,
                Web3jTransferEventFetcher.WalletTransferDirection.SENT
        );

        assertThat(filter.getTopics()).hasSize(3);
        assertThat(Web3jTransferEventFetcher.topicValue(filter.getTopics().get(0)))
                .isEqualTo(Web3jTransferEventFetcher.TRANSFER_TOPIC);
        assertThat(Web3jTransferEventFetcher.topicValue(filter.getTopics().get(1))).isEqualTo(WALLET_TOPIC);
        assertThat(Web3jTransferEventFetcher.topicValue(filter.getTopics().get(2))).isNull();
    }

    @Test
    void receiverFilterSetsTopic2Only() {
        EthFilter filter = Web3jTransferEventFetcher.buildTransferFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                "0xcontract",
                WALLET_TOPIC,
                Web3jTransferEventFetcher.WalletTransferDirection.RECEIVED
        );

        assertThat(filter.getTopics()).hasSize(3);
        assertThat(Web3jTransferEventFetcher.topicValue(filter.getTopics().get(0)))
                .isEqualTo(Web3jTransferEventFetcher.TRANSFER_TOPIC);
        assertThat(Web3jTransferEventFetcher.topicValue(filter.getTopics().get(1))).isNull();
        assertThat(Web3jTransferEventFetcher.topicValue(filter.getTopics().get(2))).isEqualTo(WALLET_TOPIC);
    }

    @Test
    void senderAndReceiverFiltersUseDistinctTopicPositions() {
        EthFilter sent = Web3jTransferEventFetcher.buildTransferFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                "0xcontract",
                WALLET_TOPIC,
                Web3jTransferEventFetcher.WalletTransferDirection.SENT
        );
        EthFilter received = Web3jTransferEventFetcher.buildTransferFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                "0xcontract",
                WALLET_TOPIC,
                Web3jTransferEventFetcher.WalletTransferDirection.RECEIVED
        );

        assertThat(sent.getTopics().get(1)).isInstanceOf(Filter.SingleTopic.class);
        assertThat(received.getTopics().get(2)).isInstanceOf(Filter.SingleTopic.class);
        assertThat(sent.getTopics().get(1)).isNotEqualTo(received.getTopics().get(1));
    }
}