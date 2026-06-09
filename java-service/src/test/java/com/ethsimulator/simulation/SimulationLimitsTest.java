package com.ethsimulator.simulation;

import com.ethsimulator.api.error.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationLimitsTest {

    @Test
    void acceptsCanonicalCompounding() {
        assertDoesNotThrow(() -> SimulationLimits.validateCompounding(1, 12));
    }

    @Test
    void rejectsExcessivePeriodProduct() {
        ApiException ex = assertThrows(ApiException.class,
                () -> SimulationLimits.validateCompounding(50, 365));
        assertEquals("INVALID_SIMULATION_INPUT", ex.getCode());
    }
}