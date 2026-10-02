package com.neuraltutor.model;

import java.util.List;

public final class MCQQuestion extends Question {
    private final List<String> options;
    private final int correctOption;

    public MCQQuestion(int id, String skill, String prompt, int difficulty,
                       List<String> options, int correctOption,
                       String explanation, String hint) {
        super(id, skill, prompt, difficulty, explanation, hint);
        if (options == null || options.size() != 4) {
            throw new IllegalArgumentException("Every MCQ must have four options.");
        }
        if (correctOption < 0 || correctOption > 3) {
            throw new IllegalArgumentException("Correct option must be 0..3.");
        }
        this.options = List.copyOf(options);
        this.correctOption = correctOption;
    }

    @Override
    public boolean isCorrect(int option) {
        return option == correctOption;
    }

    public List<String> getOptions() { return options; }
    public int getCorrectOption() { return correctOption; }
}