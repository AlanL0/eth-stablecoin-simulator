package com.ethsimulator.api.error;

import java.util.List;

public record ErrorResponse(String code, String message, List<String> details) {
}