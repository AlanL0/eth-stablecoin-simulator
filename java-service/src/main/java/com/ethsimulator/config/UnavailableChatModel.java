package com.ethsimulator.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Explicit unavailable LLM implementation until ETH-T22 wires Spring AI.
 */
public final class UnavailableChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        throw new IllegalStateException("LLM credentials are not configured");
    }
}