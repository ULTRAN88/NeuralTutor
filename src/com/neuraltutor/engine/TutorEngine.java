package com.neuraltutor.engine;

import com.neuraltutor.model.*;
import com.neuraltutor.persistence.TutorRepository;

import java.util.*;
import java.util.concurrent.*;

public final class TutorEngine {
    private final List<Question> bank;
    private final SkillGraph skillGraph = new SkillGraph();
    private final CognitiveLoadEngine loadEngine = new CognitiveLoadEngine();
    private final AdaptivePolicy policy = new AdaptivePolicy();
    private final QuestionRanker ranker = new QuestionRanker();
    private final TutorRepository repository;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "NeuralTutor-Scheduler");
                t.setDaemon(true);
                return t;
            });

    private LearnerProfile learner;
    private final Set<Integer> recentIds = new LinkedHashSet<>();
    private Question currentQuestion;
    private long questionShownAt;
    private int corrections;
    private int sessionAttempts;
    private int sessionCorrect;

    public TutorEngine(List<Question> bank, TutorRepository repository) {
        this.bank = List.copyOf(bank);
        this.repository = repository;
    }

    public synchronized LearnerProfile startLearner(String name, String goal) {
        learner = repository.loadProfile(name).orElse(new LearnerProfile(name, goal));
        learner.setGoal(goal);
        repository.saveProfile(learner);
        return learner;
    }

    public synchronized Question startAdaptiveSession() {
        ensureLearner();
        String skill = skillGraph.recommend(learner);
        return nextForSkill(skill, 2);
    }

    public synchronized Question nextForSkill(String skill, int requestedDifficulty) {
        int target = Math.max(1, Math.min(5, requestedDifficulty));
        Question next = ranker.pick(bank, learner, skill, target, recentIds);
        currentQuestion = next;
        questionShownAt = System.currentTimeMillis();
        corrections = 0;
        recentIds.add(next.getId());
        while (recentIds.size() > 12) {
            recentIds.remove(recentIds.iterator().next());
        }
        return next;
    }

    public synchronized void correctionMade() {
        corrections++;
    }

    public synchronized InteractionResult submit(int option) {
        ensureLearner();
        if (currentQuestion == null) throw new IllegalStateException("No active question.");
        long latency = Math.max(250, System.currentTimeMillis() - questionShownAt);
        boolean correct = currentQuestion.isCorrect(option);

        CognitiveLoadEngine.Snapshot snapshot =
                loadEngine.analyze(learner, currentQuestion, latency, corrections, correct);

        AdaptiveDecision decision = policy.decide(learner, currentQuestion, snapshot);

        Attempt attempt = new Attempt(
                System.currentTimeMillis(),
                learner.getName(),
                currentQuestion.getId(),
                currentQuestion.getSkill(),
                currentQuestion.getDifficulty(),
                correct,
                latency,
                corrections,
                snapshot.load(),
                snapshot.state(),
                decision.mode());

        learner.applyAttempt(attempt);
        repository.recordAttempt(attempt);
        repository.saveProfile(learner);
        sessionAttempts++;
        if (correct) sessionCorrect++;

        String hint = currentQuestion.getHint();
        return new InteractionResult(currentQuestion, attempt, snapshot, decision, hint);
    }

    public synchronized Question chooseNext(InteractionResult result) {
        AdaptiveDecision d = result.decision();
        String skill = switch (d.mode()) {
            case REVIEW_PREREQUISITE -> firstMissingPrerequisite(result.question().getSkill());
            case SPACED_REVIEW -> result.question().getSkill();
            default -> result.question().getSkill();
        };
        return nextForSkill(skill, d.targetDifficulty());
    }

    private String firstMissingPrerequisite(String skill) {
        SkillGraph.Skill s = skillGraph.skill(skill);
        if (s == null) return skill;
        for (String p : s.prerequisites()) {
            if (learner.mastery(p) < 0.48) return p;
        }
        return skill;
    }

    public synchronized void resetRecent() {
        recentIds.clear();
    }

    public synchronized LearnerProfile learner() { return learner; }
    public SkillGraph skillGraph() { return skillGraph; }
    public List<Question> bank() { return bank; }
    public int getSessionAttempts() { return sessionAttempts; }
    public int getSessionCorrect() { return sessionCorrect; }

    public synchronized void endSession() {
        repository.saveProfile(learner);
        repository.recordSession(learner.getName(), sessionAttempts, sessionCorrect);
        sessionAttempts = 0;
        sessionCorrect = 0;
    }

    public ScheduledFuture<?> schedule(Runnable r, long initialDelayMs, long periodMs) {
        return scheduler.scheduleAtFixedRate(r, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
        ranker.shutdown();
    }

    private void ensureLearner() {
        if (learner == null) throw new IllegalStateException("Start a learner first.");
    }

    public record InteractionResult(
            Question question,
            Attempt attempt,
            CognitiveLoadEngine.Snapshot load,
            AdaptiveDecision decision,
            String hint) {}
}