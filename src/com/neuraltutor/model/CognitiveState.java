package com.neuraltutor.model;

public enum CognitiveState {
    FLOW("Flow", "You're working comfortably at the current level."),
    STRETCH("Stretch", "The task is demanding, but still productive."),
    HIGH_LOAD("High Load", "Your effort is elevated. Scaffolding is recommended."),
    OVERLOAD("Overload", "The current task is consuming too much effort.");

    private final String label;
    private final String guidance;

    CognitiveState(String label, String guidance) {
        this.label = label;
        this.guidance = guidance;
    }

    public String getLabel() { return label; }
    public String getGuidance() { return guidance; }
}