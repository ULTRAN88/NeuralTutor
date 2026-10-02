package com.neuraltutor.engine;

import com.neuraltutor.model.*;

public final class KnowledgeTracingEngine {
    public double update(LearnerProfile learner, Attempt attempt) {
        LearnerProfile.TopicStats stats = learner.stats(attempt.skill());
        double previous = stats.getMastery();

        // Difficulty-aware evidence update: harder questions contribute more information.
        double positiveStep = 0.08 + attempt.difficulty() * 0.018;
        double negativeStep = 0.07 + attempt.difficulty() * 0.014;
        double next = attempt.correct()
                ? previous + positiveStep * (1.0 - previous)
                : previous - negativeStep * previous;

        return Math.max(0.0, Math.min(1.0, next));
    }

    public String level(double mastery) {
        if (mastery >= 0.80) return "Mastered";
        if (mastery >= 0.60) return "Proficient";
        if (mastery >= 0.35) return "Familiar";
        return "New";
    }
}