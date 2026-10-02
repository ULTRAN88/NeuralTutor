package com.neuraltutor.model;

public enum AdaptiveMode {
    TEACH,
    PRACTICE,
    HINT,
    EASIER,
    CHALLENGE,
    REVIEW_PREREQUISITE,
    SPACED_REVIEW;

    public String label() {
        return switch (this) {
            case TEACH -> "Micro lesson";
            case PRACTICE -> "Targeted practice";
            case HINT -> "Scaffold with hint";
            case EASIER -> "Reduce complexity";
            case CHALLENGE -> "Stretch";
            case REVIEW_PREREQUISITE -> "Review prerequisite";
            case SPACED_REVIEW -> "Spaced review";
        };
    }
}