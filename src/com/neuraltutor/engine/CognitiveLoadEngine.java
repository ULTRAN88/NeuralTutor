package com.neuraltutor.engine;

import com.neuraltutor.model.*;

public final class CognitiveLoadEngine {

    public Snapshot analyze(LearnerProfile learner, Question q,
                            long latencyMs, int corrections, boolean correct) {
        double baseline = learner.baselineLatency(q.getSkill());
        if (baseline < 1200) baseline = 5000;
        double ratio = latencyMs / baseline;

        double latencySignal = clamp((ratio - 0.85) / 1.25);
        double correctionSignal = clamp(corrections / 3.0);
        double errorSignal = correct ? 0.0 : 1.0;
        double difficultySignal = (q.getDifficulty() - 1) / 4.0;

        double load = clamp(
                0.40 * latencySignal +
                0.25 * correctionSignal +
                0.25 * errorSignal +
                0.10 * difficultySignal);

        CognitiveState state =
                load >= 0.82 ? CognitiveState.OVERLOAD :
                load >= 0.62 ? CognitiveState.HIGH_LOAD :
                load >= 0.38 ? CognitiveState.STRETCH :
                CognitiveState.FLOW;

        return new Snapshot(load, state, latencySignal, correctionSignal,
                errorSignal, difficultySignal, baseline);
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    public record Snapshot(
            double load,
            CognitiveState state,
            double latencySignal,
            double correctionSignal,
            double errorSignal,
            double difficultySignal,
            double baselineMs) {}
}