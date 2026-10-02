package com.neuraltutor.engine;

import com.neuraltutor.model.*;

public record AdaptiveDecision(
        AdaptiveMode mode,
        String title,
        String message,
        String rationale,
        int targetDifficulty) {}