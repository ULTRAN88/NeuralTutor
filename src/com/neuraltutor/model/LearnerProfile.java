package com.neuraltutor.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LearnerProfile {
    private final String name;
    private String goal;
    private int xp;
    private int streak;
    private long lastActiveAt;
    private final Map<String, TopicStats> topicStats = new ConcurrentHashMap<>();
    private final Map<String, Double> latencyBaselines = new ConcurrentHashMap<>();

    public LearnerProfile(String name, String goal) {
        this.name = Objects.requireNonNull(name).isBlank() ? "Learner" : name.trim();
        this.goal = goal == null || goal.isBlank() ? "Master Java foundations" : goal.trim();
        this.lastActiveAt = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public synchronized String getGoal() { return goal; }
    public synchronized void setGoal(String goal) { this.goal = goal; }
    public synchronized int getXp() { return xp; }
    public synchronized int getStreak() { return streak; }
    public synchronized long getLastActiveAt() { return lastActiveAt; }

    public TopicStats stats(String skill) {
        return topicStats.computeIfAbsent(skill, k -> new TopicStats());
    }

    public Map<String, TopicStats> allTopicStats() {
        return Collections.unmodifiableMap(topicStats);
    }

    public synchronized void applyAttempt(Attempt a) {
        TopicStats s = stats(a.skill());
        s.attempts++;
        if (a.correct()) s.correct++;
        s.totalLatencyMs += a.latencyMs();
        s.totalCorrections += a.corrections();
        s.mastery = Math.max(0.0, Math.min(1.0,
                s.mastery + (a.correct() ? (0.10 + a.difficulty() * 0.015)
                        : -(0.08 + a.difficulty() * 0.012))));
        if (a.correct()) {
            xp += 10 + a.difficulty() * 4;
        } else {
            xp += 2;
        }
        long now = System.currentTimeMillis();
        if (lastActiveAt == 0 || now - lastActiveAt > 36L * 60 * 60 * 1000) {
            streak = Math.max(streak, 1);
        } else if (now - lastActiveAt > 20L * 60 * 60 * 1000) {
            streak++;
        }
        lastActiveAt = now;
        latencyBaselines.put(a.skill(), rollingLatency(a.skill()));
    }

    private double rollingLatency(String skill) {
        TopicStats s = stats(skill);
        return s.attempts == 0 ? 5000.0 : (double)s.totalLatencyMs / s.attempts;
    }

    public synchronized double mastery(String skill) {
        return stats(skill).mastery;
    }

    public synchronized double averageLatency(String skill) {
        TopicStats s = stats(skill);
        return s.attempts == 0 ? 0.0 : (double) s.totalLatencyMs / s.attempts;
    }

    public synchronized double baselineLatency(String skill) {
        return latencyBaselines.getOrDefault(skill, averageLatency(skill));
    }

    public synchronized double overallMastery() {
        if (topicStats.isEmpty()) return 0.0;
        return topicStats.values().stream().mapToDouble(t -> t.mastery).average().orElse(0.0);
    }

    public synchronized int totalAttempts() {
        return topicStats.values().stream().mapToInt(t -> t.attempts).sum();
    }

    public synchronized int totalCorrect() {
        return topicStats.values().stream().mapToInt(t -> t.correct).sum();
    }

    public static final class TopicStats {
        private int attempts;
        private int correct;
        private long totalLatencyMs;
        private int totalCorrections;
        private double mastery;

        public int getAttempts() { return attempts; }
        public int getCorrect() { return correct; }
        public long getTotalLatencyMs() { return totalLatencyMs; }
        public int getTotalCorrections() { return totalCorrections; }
        public double getMastery() { return mastery; }
        public double accuracy() { return attempts == 0 ? 0.0 : (double) correct / attempts; }
        public double averageLatency() { return attempts == 0 ? 0.0 : (double) totalLatencyMs / attempts; }
    }
}