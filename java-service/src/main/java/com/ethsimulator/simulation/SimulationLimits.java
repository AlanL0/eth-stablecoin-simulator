package com.ethsimulator.simulation;

import com.ethsimulator.api.error.ApiException;
import org.springframework.http.HttpStatus;

public final class SimulationLimits {

    public static final int MAX_YEARS = 50;
    public static final int MAX_COMPOUNDS_PER_YEAR = 365;
    public static final int MAX_COMPOUNDING_PERIODS = 10_000;

    private SimulationLimits() {
    }

    public static void validateCompounding(int years, int compoundsPerYear) {
        if (years > MAX_YEARS) {
            throw limitError("years", "must be <= " + MAX_YEARS);
        }
        if (compoundsPerYear > MAX_COMPOUNDS_PER_YEAR) {
            throw limitError("compoundsPerYear", "must be <= " + MAX_COMPOUNDS_PER_YEAR);
        }
        long periods = (long) years * compoundsPerYear;
        if (periods > MAX_COMPOUNDING_PERIODS) {
            throw limitError("years * compoundsPerYear",
                    "must be <= " + MAX_COMPOUNDING_PERIODS);
        }
    }

    private static ApiException limitError(String field, String message) {
        return new ApiException(
                "INVALID_SIMULATION_INPUT",
                field + " " + message,
                HttpStatus.BAD_REQUEST);
    }
}