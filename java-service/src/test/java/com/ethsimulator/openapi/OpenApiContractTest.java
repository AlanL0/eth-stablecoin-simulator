package com.ethsimulator.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OpenApiContractTest {

    private static final Path FRONTEND_OPENAPI = Path.of("../frontend/openapi/java-api.json");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @LocalServerPort
    private int port;

    @Test
    void apiDocsExposeChartContractV2() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode root = MAPPER.readTree(body);
        JsonNode chartContract = root.path("components").path("schemas").path("ChartContract");
        assertTrue(chartContract.isObject(), "ChartContract schema must be present in /v3/api-docs");
        assertEquals("2.0",
                chartContract.path("properties").path("schemaVersion")
                        .path("example").asString());

        JsonNode dataPoint = root.path("components").path("schemas").path("DataPoint");
        assertNotNull(dataPoint.path("properties").path("plotValue"));
        assertNotNull(dataPoint.path("properties").path("displayValue"));

        JsonNode simulationResponse = root.path("components").path("schemas").path("SimulationResponse");
        String chartRef = simulationResponse.path("properties").path("charts")
                .path("items").path("$ref").asString();
        assertTrue(chartRef.endsWith("/ChartContract"), "SimulationResponse.charts must reference ChartContract");
    }

    @Test
    void committedFrontendOpenApiIncludesChartContractV2() throws Exception {
        JsonNode snapshot = MAPPER.readTree(Files.readString(FRONTEND_OPENAPI, StandardCharsets.UTF_8));
        JsonNode chartContract = snapshot.path("components").path("schemas").path("ChartContract");
        assertTrue(chartContract.isObject());
        assertTrue(snapshot.path("components").path("schemas").has("DataPoint"));
        assertTrue(snapshot.path("components").path("schemas").has("ChartSeries"));
        assertFalseChartSpecV1(snapshot);
    }

    private static void assertFalseChartSpecV1(JsonNode snapshot) {
        assertTrue(!snapshot.path("components").path("schemas").has("ChartSpecV1"),
                "ChartSpecV1 must be removed from committed OpenAPI snapshot");
    }
}