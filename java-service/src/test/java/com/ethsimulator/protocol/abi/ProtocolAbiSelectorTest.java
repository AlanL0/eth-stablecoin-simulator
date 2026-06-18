package com.ethsimulator.protocol.abi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolAbiSelectorTest {

    @Test
    void selectorsMatchReviewedMainnetAbis() {
        assertEquals("0xfeaf968c", ProtocolAbi.selector(ProtocolAbi.LATEST_ROUND_DATA));
        assertEquals("0x313ce567", ProtocolAbi.selector(ProtocolAbi.DECIMALS));
        assertEquals("0x9a6fc8f5", ProtocolAbi.selector(ProtocolAbi.getRoundData(1L)));
        assertEquals("0x5001f3b5", ProtocolAbi.selector(ProtocolAbi.JUG_BASE));
        assertEquals("0xd9638d36", ProtocolAbi.selector(ProtocolAbi.jugIlks("ETH-A")));
        assertEquals("0x03607ceb", ProtocolAbi.selector(ProtocolAbi.SUSDS_SSR));
        assertEquals("0x21f8a721", ProtocolAbi.selector(ProtocolAbi.chainlogGetAddress("MCD_JUG")));
        assertEquals("0x42635a95", ProtocolAbi.selector(ProtocolAbi.AGG_WEIGHTED_DEBT_SUM));
        assertEquals("0x8d5c1d4c", ProtocolAbi.selector(ProtocolAbi.AGG_RECORDED_DEBT));
        assertEquals("0xf71c6940", ProtocolAbi.selector(ProtocolAbi.GET_TOTAL_BOLD_DEPOSITS));
        assertEquals("0x35ea6a75", ProtocolAbi.selector(ProtocolAbi.getReserveData("0x40D16FC0246aD3160Ccc09B8D0D3A2cD28aE6C2f")));
        assertEquals("0xcc8fd393", ProtocolAbi.selector(ProtocolAbi.TARGET_RATE));
        assertTrue(ProtocolAbi.selector(ProtocolAbi.TARGET_RATE).startsWith("0x"));
    }
}