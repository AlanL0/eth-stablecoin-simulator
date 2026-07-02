package com.ethsimulator.web;

import com.ethsimulator.blockchain.TransferEventFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eth-simulator.rate-limit-enabled=true",
        "eth-simulator.rate-limit-requests-per-minute=60",
        "eth-simulator.rate-limit-burst=1",
        "eth-simulator.rate-limit-max-clients=100",
        "eth-simulator.rate-limit-trust-forwarded-for=false"
})
class RateLimitFilterTest {

    private static final String VALID_ADDRESS = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimiter rateLimiter;

    @MockitoBean
    private TransferEventFetcher transferEventFetcher;

    @BeforeEach
    void setUpFetcher() {
        rateLimiter.resetForTests();
        when(transferEventFetcher.source()).thenReturn("chain");
        when(transferEventFetcher.fetchTransferEvents(anyString())).thenReturn(List.of());
    }

    @Test
    void auditEndpointReturns429AfterBurst() throws Exception {
        mockMvc.perform(get("/api/audit/" + VALID_ADDRESS))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/audit/" + VALID_ADDRESS))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code", is("RATE_LIMITED")))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void simulationsEndpointIsNotThrottled() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/simulations")
                            .contentType("application/json")
                            .content("""
                                    {"collateralUsd":7600,"protocol":"maker_sky","deployYieldPct":5,"years":1,"compoundsPerYear":12}
                                    """))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void ignoresForwardedForWhenTrustDisabled() throws Exception {
        mockMvc.perform(get("/api/audit/" + VALID_ADDRESS)
                        .header("X-Forwarded-For", "203.0.113.10"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/audit/" + VALID_ADDRESS)
                        .header("X-Forwarded-For", "203.0.113.99"))
                .andExpect(status().isTooManyRequests());
    }
}