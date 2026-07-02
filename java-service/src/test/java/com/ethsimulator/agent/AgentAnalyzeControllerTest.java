package com.ethsimulator.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentAnalyzeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyzeEndpointReturnsDeterministicFallbackWithoutCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Compare current borrowing costs","correlationId":"test-corr"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallbackUsed", is(true)))
                .andExpect(jsonPath("$.model", is("deterministic-fallback")))
                .andExpect(jsonPath("$.narrative").isNotEmpty())
                .andExpect(jsonPath("$.traceId", is("test-corr")));
    }
}