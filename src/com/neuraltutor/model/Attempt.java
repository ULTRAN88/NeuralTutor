package com.neuraltutor.model;

import java.time.Instant;

public record Attempt(
        long timestamp,
        String learner,
        int questionId,
        String skill,
        int difficulty,
        boolean correct,
        long latencyMs,
        int corrections,
        double loadScore,
        CognitiveState cognitiveState,
        AdaptiveMode intervention) {

    public Instant instant() {
        return Instant.ofEpochMilli(timestamp);
    }
}