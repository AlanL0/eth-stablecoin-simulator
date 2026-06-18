package com.ethsimulator.protocol.support;

import com.ethsimulator.protocol.ProtocolSourcesProperties;
import com.ethsimulator.protocol.rpc.EthCallClient;
import okhttp3.OkHttpClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

public final class AdapterTestSupport {

    public static final long FIXTURE_BLOCK = 21_000_000L;
    public static final long FIXTURE_TIMESTAMP = 1_730_000_000L;
    public static final String FIXTURE_BLOCK_HASH = "0x1111111111111111111111111111111111111111111111111111111111111111";

    private AdapterTestSupport() {
    }

    public static ProtocolSourcesProperties defaultProperties() {
        ProtocolSourcesProperties properties = new ProtocolSourcesProperties();
        properties.getChainlink().setAddress("0x5f4eC3Df9cbd43714FE2740f5E3616155c5B8419");
        properties.getSky().setEnabled(true);
        properties.getLiquity().setEnabled(true);
        properties.getAave().setEnabled(true);
        return properties;
    }

    public static EthCallClient ethCallClient(String rpcUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        return new EthCallClient(Web3j.build(new HttpService(rpcUrl, client)));
    }

    public static Clock fixedClock() {
        return Clock.fixed(Instant.ofEpochSecond(FIXTURE_TIMESTAMP + 120), ZoneOffset.UTC);
    }
}