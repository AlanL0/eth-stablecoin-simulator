package com.ethsimulator.protocol.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public final class JsonRpcFixtureServer implements AutoCloseable {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final WireMockServer wireMock;
    private final long blockNumber;
    private final long blockTimestamp;
    private final String blockHash;
    private final Map<String, String> ethCallResponses = new LinkedHashMap<>();

    public JsonRpcFixtureServer(long blockNumber, long blockTimestamp, String blockHash) {
        this.blockNumber = blockNumber;
        this.blockTimestamp = blockTimestamp;
        this.blockHash = blockHash;
        this.wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    }

    public JsonRpcFixtureServer registerEthCall(String contract, String selectorOrData, String result) {
        String key = normalize(contract) + ":" + normalizeCallData(selectorOrData);
        ethCallResponses.put(key, result);
        return this;
    }

    public JsonRpcFixtureServer start() {
        wireMock.start();
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(WireMock.matchingJsonPath("$.method", WireMock.equalTo("eth_blockNumber")))
                .willReturn(jsonResponse(jsonResult(quoteHex(blockNumber)))));

        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(WireMock.matchingJsonPath("$.method", WireMock.equalTo("eth_getBlockByNumber")))
                .willReturn(jsonResponse(jsonResult(blockJson()))));

        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(WireMock.matchingJsonPath("$.method", WireMock.equalTo("eth_getLogs")))
                .inScenario("logs")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(jsonResponse(jsonResult("[]"))));

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

    public void stubTransferLogs(String jsonArray) {
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(WireMock.matchingJsonPath("$.method", WireMock.equalTo("eth_getLogs")))
                .willReturn(jsonResponse(jsonResult(jsonArray))));
    }

    public String rpcUrl() {
        return "http://localhost:" + wireMock.port();
    }

    @Override
    public void close() {
        wireMock.stop();
    }

    private String blockJson() {
        return "{"
                + "\"number\":\"" + quoteHex(blockNumber) + "\","
                + "\"hash\":\"" + blockHash + "\","
                + "\"timestamp\":\"" + quoteHex(blockTimestamp) + "\""
                + "}";
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

    private static String normalize(String address) {
        return address.toLowerCase(Locale.ROOT);
    }

    private static String normalizeCallData(String selectorOrData) {
        String normalized = selectorOrData.toLowerCase(Locale.ROOT);
        return normalized.startsWith("0x") ? normalized : "0x" + normalized;
    }

    public static String transferLog(
            String token,
            String from,
            String to,
            BigInteger value,
            long logBlockNumber
    ) {
        String fromTopic = "0x000000000000000000000000" + from.substring(2).toLowerCase(Locale.ROOT);
        String toTopic = "0x000000000000000000000000" + to.substring(2).toLowerCase(Locale.ROOT);
        String data = "0x" + String.format("%064x", value);
        return "{"
                + "\"address\":\"" + token.toLowerCase(Locale.ROOT) + "\","
                + "\"topics\":[\"0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef\",\""
                + fromTopic + "\",\"" + toTopic + "\"],"
                + "\"data\":\"" + data + "\","
                + "\"blockNumber\":\"" + quoteHex(logBlockNumber) + "\""
                + "}";
    }

    public static String logsArray(List<String> logs) {
        return "[" + String.join(",", logs) + "]";
    }

    public static JsonNode parseRpcBody(String body) throws Exception {
        return MAPPER.readTree(body);
    }
}