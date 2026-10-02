package com.neuraltutor.model;

public abstract class Question {
    private final int id;
    private final String skill;
    private final String prompt;
    private final String explanation;
    private final String hint;
    private final int difficulty;

    protected Question(int id, String skill, String prompt, int difficulty,
                       String explanation, String hint) {
        this.id = id;
        this.skill = skill;
        this.prompt = prompt;
        this.difficulty = Math.max(1, Math.min(5, difficulty));
        this.explanation = explanation;
        this.hint = hint;
    }

    public abstract boolean isCorrect(int option);

    public int getId() { return id; }
    public String getSkill() { return skill; }
    public String getPrompt() { return prompt; }
    public int getDifficulty() { return difficulty; }
    public String getExplanation() { return explanation; }
    public String getHint() { return hint; }

    public String difficultyLabel() {
        return switch (difficulty) {
            case 1 -> "Foundation";
            case 2 -> "Core";
            case 3 -> "Applied";
            case 4 -> "Advanced";
            default -> "Challenge";
        };
    }
}