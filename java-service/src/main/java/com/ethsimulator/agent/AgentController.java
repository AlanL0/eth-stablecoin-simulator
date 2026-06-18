package com.ethsimulator.agent;

import com.ethsimulator.agent.dto.ParseGoalRequest;
import com.ethsimulator.agent.dto.ParseGoalResponse;
import com.ethsimulator.agent.dto.RecommendYieldRequest;
import com.ethsimulator.agent.dto.RecommendYieldResponse;
import com.ethsimulator.agent.dto.SummarizeAuditRequest;
import com.ethsimulator.agent.dto.SummarizeAuditResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentStubService agentStubService;

    public AgentController(AgentStubService agentStubService) {
        this.agentStubService = agentStubService;
    }

    @PostMapping("/recommend-yield")
    public RecommendYieldResponse recommendYield(@Valid @RequestBody RecommendYieldRequest request) {
        return agentStubService.recommendYield(request);
    }

    @PostMapping("/parse-goal")
    public ParseGoalResponse parseGoal(@Valid @RequestBody ParseGoalRequest request) {
        return agentStubService.parseGoal(request);
    }

    @PostMapping("/summarize-audit")
    public SummarizeAuditResponse summarizeAudit(@Valid @RequestBody SummarizeAuditRequest request) {
        return agentStubService.summarizeAudit(request);
    }
}