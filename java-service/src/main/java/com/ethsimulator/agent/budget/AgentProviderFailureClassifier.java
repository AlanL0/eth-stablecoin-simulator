package com.ethsimulator.agent.budget;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public final class AgentProviderFailureClassifier {

    private AgentProviderFailureClassifier() {
    }

    public static boolean isTransient(RuntimeException ex) {
        if (ex instanceof AgentBudgetViolationException) {
            return false;
        }
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TimeoutException
                    || current instanceof SocketTimeoutException
                    || current instanceof IOException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lowered = message.toLowerCase();
                if (lowered.contains("timeout")
                        || lowered.contains("temporarily unavailable")
                        || lowered.contains("connection reset")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}