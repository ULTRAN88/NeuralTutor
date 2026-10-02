package com.neuraltutor.engine;

import com.neuraltutor.model.*;

public final class AdaptivePolicy {
    public AdaptiveDecision decide(LearnerProfile learner, Question q,
                                   CognitiveLoadEngine.Snapshot load) {
        double mastery = learner.mastery(q.getSkill());

        if (load.state() == CognitiveState.OVERLOAD) {
            return new AdaptiveDecision(
                    mastery < 0.30 ? AdaptiveMode.REVIEW_PREREQUISITE : AdaptiveMode.EASIER,
                    mastery < 0.30 ? "Let's rebuild the foundation" : "Let's reduce the load",
                    mastery < 0.30
                            ? "A prerequisite is consuming too much working memory. We will step back before adding new complexity."
                            : "Your response pattern suggests this task is currently heavier than your normal baseline.",
                    "High latency/corrections + current accuracy indicate overload.",
                    Math.max(1, q.getDifficulty() - 1));
        }

        if (load.state() == CognitiveState.HIGH_LOAD) {
            return new AdaptiveDecision(
                    AdaptiveMode.HINT,
                    "Scaffold, don't stop",
                    "Here's a targeted hint. Keep the concept, remove unnecessary search.",
                    "The learner is still engaging, but effort is elevated.",
                    Math.max(1, q.getDifficulty()));
        }

        if (mastery >= 0.80 && load.state() == CognitiveState.FLOW) {
            return new AdaptiveDecision(
                    AdaptiveMode.CHALLENGE,
                    "You're ready for a stretch",
                    "Your performance is stable at this level. The next question will increase conceptual depth.",
                    "High mastery + low load supports a difficulty increase.",
                    Math.min(5, q.getDifficulty() + 1));
        }

        if (mastery < 0.35 && load.state() == CognitiveState.FLOW) {
            return new AdaptiveDecision(
                    AdaptiveMode.TEACH,
                    "Build the mental model",
                    "You are not overloaded, but this skill is still emerging. A short explanation before the next question will help.",
                    "Low mastery with manageable effort suggests teaching is more useful than simply escalating practice.",
                    Math.max(1, q.getDifficulty()));
        }

        return new AdaptiveDecision(
                AdaptiveMode.PRACTICE,
                "Keep the concept active",
                "Your current difficulty is appropriate. We'll keep practicing and update the learner model.",
                "Current load and mastery are inside the target learning zone.",
                q.getDifficulty());
    }
}