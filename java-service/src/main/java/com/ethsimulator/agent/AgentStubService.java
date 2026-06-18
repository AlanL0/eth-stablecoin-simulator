package com.ethsimulator.agent;

import com.ethsimulator.agent.dto.ParseGoalRequest;
import com.ethsimulator.agent.dto.ParseGoalResponse;
import com.ethsimulator.agent.dto.RecommendYieldRequest;
import com.ethsimulator.agent.dto.RecommendYieldResponse;
import com.ethsimulator.agent.dto.SummarizeAuditRequest;
import com.ethsimulator.agent.dto.SummarizeAuditResponse;
import com.ethsimulator.util.FinancialMath;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AgentStubService {

    public RecommendYieldResponse recommendYield(RecommendYieldRequest request) {
        Map<String, Object> simulation = request.simulationResult();
        String debt = stringValue(simulation.get("stablecoinDebtUsd"));
        String liquidation = stringValue(simulation.get("liquidationPriceUsd"));
        String health = stringValue(simulation.get("healthRatio"));
        String riskTier = stringValue(simulation.get("riskTier"));
        String netYield = stringValue(simulation.get("projectedNetYieldUsd"));
        @SuppressWarnings("unchecked")
        Map<String, Object> assumptions = simulation.get("assumptions") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        String ethPrice = stringValue(assumptions.get("ethPriceUsd"));

        String summary = "Simulation shows about $%s stablecoin debt with health ratio %s (%s). "
                .formatted(debt, health, riskTier)
                + "Liquidation price is near $%s per ETH versus spot $%s. "
                    .formatted(liquidation, ethPrice)
                + "Projected net yield after fees is about $%s over the modeled horizon."
                    .formatted(netYield);
        if (request.message() != null && !request.message().isBlank()) {
            summary = summary + " Your question: " + request.message();
        }

        List<String> recommendations = new java.util.ArrayList<>(List.of(
                "Review collateral buffer before increasing borrow size.",
                "Compare deploy yield assumptions against the stability fee model."
        ));
        if ("conservative".equalsIgnoreCase(request.riskPreference())) {
            recommendations.add("Consider adding collateral to move away from the HIGH risk band.");
        }

        return new RecommendYieldResponse(
                summary,
                recommendations,
                List.of(
                        "Health ratio is %s with liquidation near $%s per ETH.".formatted(health, liquidation),
                        "Net yield after fees is only $%s in the deterministic model.".formatted(netYield)
                ),
                List.of(
                        "Protocol presets are model assumptions, not live on-chain guarantees.",
                        "Stability fee model: %s.".formatted(
                                assumptions.getOrDefault("stabilityFeeModel", "linear_annualized_v1"))
                ),
                List.of(),
                List.of(),
                List.of(),
                "deterministic-fallback",
                true
        );
    }

    public ParseGoalResponse parseGoal(ParseGoalRequest request) {
        String message = request.message() == null ? "" : request.message();
        String lowered = message.toLowerCase(Locale.ROOT);

        if (lowered.contains("chart") || lowered.contains("scenario") || lowered.contains("bear") || lowered.contains("bull")) {
            return new ParseGoalResponse(
                    "request_chart",
                    Map.of("chartType", "scenario_chart"),
                    "run_simulation",
                    FinancialMath.bd("0.86"),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (lowered.contains("wallet") || lowered.contains("balance")) {
            return new ParseGoalResponse("unknown", Map.of(), "open_wallet", FinancialMath.bd("0.7"), List.of(), List.of(), List.of());
        }
        if (lowered.contains("audit") || lowered.contains("transfer")) {
            return new ParseGoalResponse("audit_help", Map.of(), "open_audit", FinancialMath.bd("0.7"), List.of(), List.of(), List.of());
        }
        if (lowered.contains("borrow") || lowered.contains("simulate") || lowered.contains("how much")) {
            return new ParseGoalResponse("run_simulation", Map.of(), "run_simulation", FinancialMath.bd("0.75"), List.of(), List.of(), List.of());
        }
        return new ParseGoalResponse("unknown", Map.of(), "none", FinancialMath.bd("0.5"), List.of(), List.of(), List.of());
    }

    public SummarizeAuditResponse summarizeAudit(SummarizeAuditRequest request) {
        List<Map<String, Object>> events = request.events() == null ? List.of() : request.events();
        if (events.isEmpty()) {
            return new SummarizeAuditResponse(
                    "No transfer events were found for this address and date range. "
                            + "Verify the wallet address and widen the date filter.",
                    List.of(),
                    List.of("An empty audit may mean the address had no allowlisted stablecoin activity."),
                    List.of("Audit is lite, allowlisted tokens only, and may be incomplete."),
                    "deterministic-fallback",
                    true
            );
        }
        int count = events.size();
        String summary = request.hideValues()
                ? "Found %d hidden transfers in the selected period.".formatted(count)
                : "Found %d allowlisted stablecoin transfers in the selected period.".formatted(count);
        return new SummarizeAuditResponse(
                summary,
                List.of("%d transfer event(s) in the supplied audit payload.".formatted(count)),
                List.of("Transfer visibility depends on allowlisted tokens and cached fetch windows."),
                List.of("Audit is lite, allowlisted tokens only, and may be incomplete."),
                "deterministic-fallback",
                true
        );
    }

    private static String stringValue(Object value) {
        return value == null ? "n/a" : String.valueOf(value);
    }
}