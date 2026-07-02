package com.ethsimulator.agent;

import com.ethsimulator.agent.dto.AgentAnalysisResponse;
import com.ethsimulator.agent.dto.ToolProvenanceRecord;
import com.ethsimulator.agent.tools.FixedIncomeAnalyticsTools;
import com.ethsimulator.agent.tools.SimulationToolRequest;
import com.ethsimulator.charts.ChartContract;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AgentDeterministicFallbackService {

    private final FixedIncomeAnalyticsTools analyticsTools;
    private final Clock clock;

    public AgentDeterministicFallbackService(FixedIncomeAnalyticsTools analyticsTools, Clock clock) {
        this.analyticsTools = analyticsTools;
        this.clock = clock;
    }

    public AgentAnalysisResponse analyze(String message, String correlationId, String reason) {
        String lowered = message == null ? "" : message.toLowerCase(Locale.ROOT);
        List<ToolProvenanceRecord> provenance = new ArrayList<>();
        ChartContract chart = null;
        String narrative;

        if (containsAny(lowered, "borrow", "cost", "rate", "yield", "compare", "venue", "protocol")) {
            var yields = analyticsTools.getLatestYields("USDC");
            chart = analyticsTools.buildProtocolRatesChart("USDC");
            provenance.add(record("getLatestYields", "USDC yields (" + yields.dataMode() + ")"));
            provenance.add(record("buildProtocolRatesChart", "USDC protocol comparison"));
            narrative = "Deterministic fallback compared USDC borrowing costs and protocol yields using Java read models. "
                    + "Data mode: " + yields.dataMode() + ". "
                    + "All figures come from backend tools, not model estimates.";
        } else if (containsAny(lowered, "simulate", "health", "liquidation", "how much")) {
            var simulation = analyticsTools.runSimulation(new SimulationToolRequest(null, "maker_sky", null, null, null, "USDC"));
            chart = simulation.charts().isEmpty() ? null : simulation.charts().getFirst();
            provenance.add(record("runSimulation", "maker_sky baseline"));
            narrative = "Deterministic fallback ran a maker_sky simulation. Stablecoin debt is $"
                    + simulation.stablecoinDebtUsd() + " with health ratio "
                    + simulation.healthRatio() + " (" + simulation.riskTier() + ").";
        } else if (containsAny(lowered, "audit", "transfer", "wallet")) {
            narrative = "Deterministic fallback: open the wallet audit view to inspect allowlisted stablecoin transfers.";
        } else if (containsAny(lowered, "chart", "scenario", "bear", "bull")) {
            chart = analyticsTools.buildProtocolRatesChart("USDC");
            provenance.add(record("buildProtocolRatesChart", "USDC protocol comparison"));
            narrative = "Deterministic fallback prepared a protocol return comparison chart for USDC.";
        } else {
            narrative = "Deterministic fallback: ask about borrowing costs, simulations, liquidation risk, or wallet audit. "
                    + "Numeric answers require backend tools.";
        }

        return new AgentAnalysisResponse(
                narrative,
                chart,
                List.of("Response composed without LLM orchestration."),
                provenance,
                true,
                reason,
                traceId(correlationId),
                "deterministic-fallback"
        );
    }

    private ToolProvenanceRecord record(String toolName, String summary) {
        return new ToolProvenanceRecord(toolName, summary, clock.instant());
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String traceId(String correlationId) {
        return correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
    }
}