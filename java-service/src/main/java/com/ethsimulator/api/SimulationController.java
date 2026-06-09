package com.ethsimulator.api;

import com.ethsimulator.api.dto.SimulationRequest;
import com.ethsimulator.api.dto.SimulationResponse;
import com.ethsimulator.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
        return simulationService.simulate(request);
    }
}