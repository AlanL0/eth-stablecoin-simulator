package com.ethsimulator.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recommendYieldReturnsDeterministicFallback() throws Exception {
        mockMvc.perform(post("/agent/recommend-yield")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "simulationResult": {
                                    "healthRatio": 1.2,
                                    "riskTier": "HIGH",
                                    "stablecoinDebtUsd": 4222.22,
                                    "liquidationPriceUsd": 3166.67,
                                    "projectedNetYieldUsd": 4.91,
                                    "assumptions": { "ethPriceUsd": 3800 }
                                  },
                                  "message": "How risky is this?",
                                  "riskPreference": "conservative"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.fallbackUsed", is(true)))
                .andExpect(jsonPath("$.model", is("deterministic-fallback")));
    }

    @Test
    void parseGoalDetectsChartIntent() throws Exception {
        mockMvc.perform(post("/agent/parse-goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Show me bear and bull ETH scenarios as a chart",
                                  "sessionId": "test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent", is("request_chart")));
    }
}