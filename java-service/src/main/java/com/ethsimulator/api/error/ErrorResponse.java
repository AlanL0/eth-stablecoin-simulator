package com.ethsimulator.api.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ErrorResponse")
public record ErrorResponse(String code, String message, List<String> details) {
}