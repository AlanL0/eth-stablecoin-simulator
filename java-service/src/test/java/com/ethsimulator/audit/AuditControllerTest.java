package com.ethsimulator.audit;

import com.ethsimulator.blockchain.TransferEventFetcher;
import com.ethsimulator.blockchain.TransferEventRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditControllerTest {

    private static final String VALID_ADDRESS = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045";
    private static final Instant EVENT_TIME = Instant.parse("2026-01-10T15:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditCache auditCache;

    @MockitoBean
    private TransferEventFetcher transferEventFetcher;

    @BeforeEach
    void setUp() {
        auditCache.clear();
        when(transferEventFetcher.source()).thenReturn("chain");
        when(transferEventFetcher.fetchTransferEvents(VALID_ADDRESS.toLowerCase()))
                .thenReturn(List.of(
                        new TransferEventRecord(
                                "USDC",
                                "0xabc123",
                                1,
                                VALID_ADDRESS.toLowerCase(),
                                "0x1111111111111111111111111111111111111111",
                                new BigDecimal("100"),
                                19000000L,
                                EVENT_TIME
                        ),
                        new TransferEventRecord(
                                "DAI",
                                "0xdef456",
                                0,
                                "0x2222222222222222222222222222222222222222",
                                VALID_ADDRESS.toLowerCase(),
                                new BigDecimal("50"),
                                19000001L,
                                Instant.parse("2026-01-11T10:00:00Z")
                        )
                ));
    }

    @Test
    void returnsAuditEvents() throws Exception {
        mockMvc.perform(get("/api/audit/{address}", VALID_ADDRESS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address", is(VALID_ADDRESS.toLowerCase())))
                .andExpect(jsonPath("$.events", hasSize(2)))
                .andExpect(jsonPath("$.events[0].token", is("DAI")))
                .andExpect(jsonPath("$.events[0].amount", is("50")))
                .andExpect(jsonPath("$.hideValues", is(false)))
                .andExpect(jsonPath("$.assumptions", hasSize(4)));

        verify(transferEventFetcher, times(1)).fetchTransferEvents(VALID_ADDRESS.toLowerCase());
    }

    @Test
    void filtersByToken() throws Exception {
        mockMvc.perform(get("/api/audit/{address}", VALID_ADDRESS).param("token", "USDC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].token", is("USDC")));
    }

    @Test
    void rejectsInvalidTokenFilter() throws Exception {
        mockMvc.perform(get("/api/audit/{address}", VALID_ADDRESS).param("token", "BTC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_TOKEN")));
    }

    @Test
    void rejectsInvalidAddress() throws Exception {
        mockMvc.perform(get("/api/audit/{address}", "0x123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_ADDRESS")));
    }

    @Test
    void masksAmountsWhenHideValuesEnabled() throws Exception {
        mockMvc.perform(get("/api/audit/{address}", VALID_ADDRESS).param("hideValues", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hideValues", is(true)))
                .andExpect(jsonPath("$.events[0].amount", is("***")))
                .andExpect(jsonPath("$.events[1].amount", is("***")));
    }

    @Test
    void exportsCsvWithHeaderAndRows() throws Exception {
        mockMvc.perform(get("/api/audit/{address}/export.csv", VALID_ADDRESS).param("token", "USDC"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                .andExpect(content().string(containsString("token,tx_hash,log_index,from_address,to_address,amount,block_number,occurred_at")))
                .andExpect(content().string(containsString("USDC,0xabc123,1")));
    }

    @Test
    void exportsJsonWithSameFilters() throws Exception {
        mockMvc.perform(get("/api/audit/{address}/export.json", VALID_ADDRESS)
                        .param("token", "DAI")
                        .param("hideValues", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].token", is("DAI")))
                .andExpect(jsonPath("$.events[0].amount", is("***")))
                .andExpect(jsonPath("$.hideValues", is(true)));
    }
}