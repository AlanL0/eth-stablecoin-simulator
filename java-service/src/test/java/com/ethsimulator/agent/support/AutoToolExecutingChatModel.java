package com.ethsimulator.agent.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Anti-pattern fake: executes tools inside {@link ChatModel#call(Prompt)} and returns only
 * a final assistant message. Documents the failure mode when tool mediation is bypassed.
 */
public class AutoToolExecutingChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        ChatOptions options = prompt.getOptions();
        if (!(options instanceof ToolCallingChatOptions toolOptions)) {
            return textResponse("No tools configured.");
        }
        List<ToolCallback> callbacks = toolOptions.getToolCallbacks();
        if (callbacks == null || callbacks.isEmpty()) {
            return textResponse("No tools configured.");
        }
        Map<String, ToolCallback> byName = callbacks.stream()
                .collect(Collectors.toMap(
                        callback -> callback.getToolDefinition().name(),
                        callback -> callback,
                        (left, right) -> left
                ));

        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "auto-call",
                "function",
                "getLatestYields",
                "{\"asset\":\"USDC\"}"
        );
        ToolCallback callback = byName.get(toolCall.name());
        if (callback != null) {
            callback.call(toolCall.arguments());
        }
        return textResponse("Auto-executed tools inside ChatModel.");
    }

    private static ChatResponse textResponse(String text) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(text))))
                .build();
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }
}