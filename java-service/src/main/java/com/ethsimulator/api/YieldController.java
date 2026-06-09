package com.ethsimulator.api;

import com.ethsimulator.market.YieldService;
import com.ethsimulator.market.YieldSnapshotResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yields")
public class YieldController {

    private final YieldService yieldService;

    public YieldController(YieldService yieldService) {
        this.yieldService = yieldService;
    }

    @GetMapping
    public YieldSnapshotResponse yields(@RequestParam String asset) {
        return yieldService.getYields(asset);
    }
}