package com.ethsimulator.agent.support;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayDeque;
import java.util.Deque;

public class ScriptedChatModel implements ChatModel {

    private final Deque<ChatResponse> responses = new ArrayDeque<>();
    private RuntimeException failure;

    public void enqueue(ChatResponse response) {
        responses.addLast(response);
    }

    public void failWith(RuntimeException exception) {
        this.failure = exception;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        if (failure != null) {
            throw failure;
        }
        ChatResponse next = responses.pollFirst();
        if (next == null) {
            throw new IllegalStateException("No scripted ChatModel response remaining");
        }
        return next;
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }
}