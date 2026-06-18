package com.ethsimulator.ingestion.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public final class MultiBlockJsonRpcFixtureServer implements AutoCloseable {

    private final WireMockServer wireMock;
    private final long latestBlockNumber;
    private final Map<Long, BlockFixture> blocks = new LinkedHashMap<>();
    private final Map<String, String> ethCallResponses = new LinkedHashMap<>();

    public MultiBlockJsonRpcFixtureServer(long latestBlockNumber) {
        this.latestBlockNumber = latestBlockNumber;
        this.wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    }

    public MultiBlockJsonRpcFixtureServer registerBlock(long blockNumber, String blockHash, long blockTimestamp) {
        blocks.put(blockNumber, new BlockFixture(blockNumber, blockHash, blockTimestamp));
        return this;
    }

    public MultiBlockJsonRpcFixtureServer registerEthCall(String contract, String selectorOrData, String result) {
        String key = normalize(contract) + ":" + normalizeCallData(selectorOrData);
        ethCallResponses.put(key, result);
        return this;
    }

    public MultiBlockJsonRpcFixtureServer start() {
        wireMock.start();
        String headBlockHex = quoteHex(latestBlockNumber);
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(WireMock.matchingJsonPath("$.method", WireMock.equalTo("eth_blockNumber")))
                .willReturn(jsonResponse(jsonResult(headBlockHex))));

        BlockFixture head = blocks.getOrDefault(
                latestBlockNumber,
                new BlockFixture(latestBlockNumber, defaultHash(latestBlockNumber), latestBlockNumber)
        );
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(WireMock.matchingJsonPath("$.method", WireMock.equalTo("eth_getBlockByNumber")))
                .withRequestBody(WireMock.containing("\"latest\""))
                .willReturn(jsonResponse(jsonResult(head.json()))));

        for (BlockFixture block : blocks.values()) {
            String blockParam = quoteHex(block.blockNumber());
            wireMock.stubFor(post(urlEqualTo("/"))
                    .withRequestBody(WireMock.matchingJsonPath("$.method", WireMock.equalTo("eth_getBlockByNumber")))
                    .withRequestBody(WireMock.containing(blockParam))
                    .willReturn(jsonResponse(jsonResult(block.json()))));
        }

        for (Map.Entry<String, String> entry : ethCallResponses.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            String contract = parts[0];
            String callData = parts[1];
            wireMock.stubFor(post(urlEqualTo("/"))
                    .withRequestBody(WireMock.containing("\"method\":\"eth_call\""))
                    .withRequestBody(WireMock.matching("(?is).*" + contract + ".*" + callData + ".*"))
                    .willReturn(jsonResponse(jsonResult(entry.getValue()))));
        }
        return this;
    }

    public String rpcUrl() {
        return "http://localhost:" + wireMock.port();
    }

    @Override
    public void close() {
        wireMock.stop();
    }

    private record BlockFixture(long blockNumber, String blockHash, long blockTimestamp) {
        String json() {
            return "{"
                    + "\"number\":\"" + quoteHex(blockNumber) + "\","
                    + "\"hash\":\"" + blockHash + "\","
                    + "\"timestamp\":\"" + quoteHex(blockTimestamp) + "\""
                    + "}";
        }
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder jsonResponse(String body) {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static String jsonResult(String resultPayload) {
        if (resultPayload.startsWith("{") || resultPayload.startsWith("[")) {
            return "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":" + resultPayload + "}";
        }
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"" + resultPayload + "\"}";
    }

    private static String quoteHex(long value) {
        return "0x" + Long.toHexString(value);
    }

    private static String defaultHash(long blockNumber) {
        return String.format("0x%064x", blockNumber);
    }

    private static String normalize(String address) {
        return address.toLowerCase(Locale.ROOT);
    }

    private static String normalizeCallData(String selectorOrData) {
        String normalized = selectorOrData.toLowerCase(Locale.ROOT);
        return normalized.startsWith("0x") ? normalized : "0x" + normalized;
    }
}