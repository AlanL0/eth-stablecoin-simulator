package com.ethsimulator.agent.support;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates a provider-backed {@link ChatModel} that returns tool-call responses
 * without executing registered callbacks. Spring AI 2.0 {@link ChatModel#call(Prompt)}
 * follows this contract; tool execution belongs to the orchestrator loop.
 */
public class ProviderShapedChatModel implements ChatModel {

    private final Deque<ChatResponse> responses = new ArrayDeque<>();
    private final AtomicInteger callCount = new AtomicInteger();
    private final AtomicBoolean toolCallbacksObserved = new AtomicBoolean(false);
    private RuntimeException failure;

    public void enqueue(ChatResponse response) {
        responses.addLast(response);
    }

    public void failWith(RuntimeException exception) {
        this.failure = exception;
    }

    public int callCount() {
        return callCount.get();
    }

    public boolean toolCallbacksObserved() {
        return toolCallbacksObserved.get();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        if (failure != null) {
            throw failure;
        }
        callCount.incrementAndGet();
        observeToolCallbacks(prompt);
        ChatResponse next = responses.pollFirst();
        if (next == null) {
            throw new IllegalStateException("No provider-shaped ChatModel response remaining");
        }
        return next;
    }

    private void observeToolCallbacks(Prompt prompt) {
        ChatOptions options = prompt.getOptions();
        if (options instanceof ToolCallingChatOptions toolOptions
                && toolOptions.getToolCallbacks() != null
                && !toolOptions.getToolCallbacks().isEmpty()) {
            toolCallbacksObserved.set(true);
        }
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }
}