package com.ethsimulator.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));
    }

    @Test
    void happyPathEthAmount() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "protocol": "maker_sky",
                                  "deployYieldPct": 5,
                                  "years": 1,
                                  "compoundsPerYear": 12
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collateralValueUsd", closeTo(7600.0, 0.01)))
                .andExpect(jsonPath("$.stablecoinDebtUsd", closeTo(4222.22, 0.01)))
                .andExpect(jsonPath("$.liquidationPriceUsd", closeTo(3166.67, 0.01)))
                .andExpect(jsonPath("$.projectedNetYieldUsd", closeTo(4.91, 0.02)))
                .andExpect(jsonPath("$.healthRatio", closeTo(1.2, 0.01)))
                .andExpect(jsonPath("$.riskTier", is("HIGH")))
                .andExpect(jsonPath("$.assumptions.ethPriceSource", is("static")))
                .andExpect(jsonPath("$.treasuryContext.yourMint.impliedTreasuryBackingUsd", closeTo(3800.0, 0.01)))
                .andExpect(jsonPath("$.treasuryContext.yourMint.annualIssuerReserveYieldUsd", closeTo(171.0, 0.01)))
                .andExpect(jsonPath("$.charts", hasSize(4)))
                .andExpect(jsonPath("$.charts[0].chartId", is("simulation_yield_projection")))
                .andExpect(jsonPath("$.charts[0].generatedAt", notNullValue()))
                .andExpect(jsonPath("$.charts[1].chartId", is("liquidation_price_band")))
                .andExpect(jsonPath("$.charts[2].chartId", is("health_ratio_sweep")))
                .andExpect(jsonPath("$.charts[3].chartId", is("stablecoin_treasury_context")));
    }

    @Test
    void collateralUsdMatchesEthPath() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collateralUsd": 7600,
                                  "protocol": "maker_sky",
                                  "deployYieldPct": 5,
                                  "years": 1,
                                  "compoundsPerYear": 12
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stablecoinDebtUsd", closeTo(4222.22, 0.01)))
                .andExpect(jsonPath("$.assumptions.ethAmount", closeTo(2.0, 0.01)));
    }

    @Test
    void acceptsMatchingDualInputs() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "collateralUsd": 7600,
                                  "protocol": "maker_sky"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stablecoinDebtUsd", closeTo(4222.22, 0.01)));
    }

    @Test
    void rejectsInconsistentDualInputs() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "collateralUsd": 7000,
                                  "protocol": "maker_sky"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_SIMULATION_INPUT")))
                .andExpect(jsonPath("$.message", is("ethAmount and collateralUsd disagree beyond 0.5% tolerance")));
    }

    @Test
    void rejectsBadProtocol() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "protocol": "not_a_protocol"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_SIMULATION_INPUT")));
    }

    @Test
    void rejectsZeroEth() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 0,
                                  "protocol": "maker_sky"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_SIMULATION_INPUT")));
    }

    @Test
    void rejectsMissingCollateralInput() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocol": "maker_sky"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_SIMULATION_INPUT")));
    }

    @Test
    void rejectsExcessiveYears() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "protocol": "maker_sky",
                                  "years": 51
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_SIMULATION_INPUT")));
    }

    @Test
    void rejectsTreasuryOverrideOnNamedModel() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "protocol": "maker_sky",
                                  "stablecoinReserveModel": "usdc_style",
                                  "reserveInTreasuriesPct": 95
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_SIMULATION_INPUT")));
    }

    @Test
    void allowsTreasuryOverrideOnGenericModel() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "protocol": "maker_sky",
                                  "stablecoinReserveModel": "generic",
                                  "reserveInTreasuriesPct": 75,
                                  "tbillApyPct": 4.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treasuryContext.assumptions.reserveInTreasuriesPct", closeTo(75.0, 0.01)));
    }

    @Test
    void treasuryDisabledOmitsContextAndChart() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "protocol": "maker_sky",
                                  "treasuryContextEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treasuryContext").doesNotExist())
                .andExpect(jsonPath("$.charts", hasSize(3)));
    }

    @Test
    void responseIncludesId() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ethAmount": 2,
                                  "protocol": "maker_sky"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()));
    }
}