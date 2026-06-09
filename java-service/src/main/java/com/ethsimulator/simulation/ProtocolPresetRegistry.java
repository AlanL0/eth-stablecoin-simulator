package com.ethsimulator.simulation;

import com.ethsimulator.util.UsdMath;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ProtocolPresetRegistry {

    private final Map<String, ProtocolPreset> presets = new LinkedHashMap<>();

    public ProtocolPresetRegistry() {
        register(new ProtocolPreset("maker_sky", "Maker/Sky-style vault",
                UsdMath.bd("1.80"), UsdMath.bd("1.50"), UsdMath.bd("5.00")));
        register(new ProtocolPreset("liquity", "Liquity-style borrowing",
                UsdMath.bd("2.00"), UsdMath.bd("1.10"), UsdMath.bd("0.50")));
        register(new ProtocolPreset("aave_gho", "Aave/GHO-style borrowing",
                UsdMath.bd("2.20"), UsdMath.bd("1.25"), UsdMath.bd("4.00")));
        register(new ProtocolPreset("custom", "Custom",
                UsdMath.bd("2.00"), UsdMath.bd("1.50"), UsdMath.bd("5.00")));
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