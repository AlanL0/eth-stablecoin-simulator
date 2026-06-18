package com.ethsimulator.simulation;

import com.ethsimulator.util.FinancialMath;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ProtocolPresetRegistry {

    private final Map<String, ProtocolPreset> presets = new LinkedHashMap<>();

    public ProtocolPresetRegistry() {
        register(new ProtocolPreset("maker_sky", "Maker/Sky-style vault",
                FinancialMath.bd("1.80"), FinancialMath.bd("1.50"), FinancialMath.bd("5.00")));
        register(new ProtocolPreset("liquity", "Liquity-style borrowing",
                FinancialMath.bd("2.00"), FinancialMath.bd("1.10"), FinancialMath.bd("0.50")));
        register(new ProtocolPreset("aave_gho", "Aave/GHO-style borrowing",
                FinancialMath.bd("2.20"), FinancialMath.bd("1.25"), FinancialMath.bd("4.00")));
        register(new ProtocolPreset("custom", "Custom",
                FinancialMath.bd("2.00"), FinancialMath.bd("1.50"), FinancialMath.bd("5.00")));
    }

    private void register(ProtocolPreset preset) {
        presets.put(preset.name(), preset);
    }

    public Optional<ProtocolPreset> find(String name) {
        return Optional.ofNullable(presets.get(name));
    }

    public boolean supports(String name) {
        return presets.containsKey(name);
    }
}