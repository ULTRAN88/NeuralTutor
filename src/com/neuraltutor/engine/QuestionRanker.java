package com.neuraltutor.engine;

import com.neuraltutor.model.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public final class QuestionRanker {
    private final ForkJoinPool pool = new ForkJoinPool(Math.max(2,
            Runtime.getRuntime().availableProcessors() / 2));

    public Question pick(List<Question> bank, LearnerProfile learner,
                         String skill, int targetDifficulty, Set<Integer> recentIds) {
        List<Question> candidates = bank.stream()
                .filter(q -> q.getSkill().equals(skill))
                .filter(q -> !recentIds.contains(q.getId()))
                .toList();

        if (candidates.isEmpty()) {
            candidates = bank.stream()
                    .filter(q -> q.getSkill().equals(skill))
                    .toList();
        }

        final int desired = targetDifficulty;
        final List<Question> finalCandidates = candidates;

        return pool.submit(() ->
                finalCandidates.parallelStream()
                        .max(Comparator.comparingDouble(q -> score(q, learner, skill, desired)))
                        .orElseGet(() -> bank.get(0))
        ).join();
    }

    private double score(Question q, LearnerProfile learner, String skill, int desired) {
        double mastery = learner.mastery(skill);
        double difficultyFit = 1.0 - Math.min(1.0, Math.abs(q.getDifficulty() - desired) / 4.0);
        double masteryNeed = 0.65 + (1.0 - mastery) * 0.35;
        return difficultyFit * 0.55 + masteryNeed * 0.45;
    }

    public void shutdown() {
        pool.shutdown();
    }
}