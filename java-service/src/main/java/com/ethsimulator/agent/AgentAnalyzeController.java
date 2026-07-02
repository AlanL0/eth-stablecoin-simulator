package com.ethsimulator.agent;

import com.ethsimulator.agent.dto.AgentAnalysisResponse;
import com.ethsimulator.agent.dto.AgentAnalyzeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentAnalyzeController {

    private final AgentOrchestratorService orchestratorService;

    public AgentAnalyzeController(AgentOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/analyze")
    public AgentAnalysisResponse analyze(@Valid @RequestBody AgentAnalyzeRequest request) {
        return orchestratorService.analyze(request);
    }
}