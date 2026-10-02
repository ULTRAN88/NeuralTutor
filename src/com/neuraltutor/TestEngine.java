package com.neuraltutor;

import com.neuraltutor.content.ContentCatalog;
import com.neuraltutor.engine.CognitiveLoadEngine;
import com.neuraltutor.engine.TutorEngine;
import com.neuraltutor.model.*;
import com.neuraltutor.persistence.TutorRepository;

public final class TestEngine {
    public static void main(String[] args) {
        java.nio.file.Path db = java.nio.file.Path.of("build", "test-neuraltutor.db");
        try { java.nio.file.Files.deleteIfExists(db); } catch (Exception ignored) {}
        TutorRepository repo = new TutorRepository("jdbc:sqlite:" + db.toString());
        TutorEngine engine = new TutorEngine(ContentCatalog.questions(), repo);
        LearnerProfile p = engine.startLearner("Test Learner", "Validate adaptive loop");

        Question q = engine.startAdaptiveSession();
        if (q == null) throw new AssertionError("No question selected");

        TutorEngine.InteractionResult r = engine.submit(
                q instanceof MCQQuestion m ? m.getCorrectOption() : 0);
        if (!r.attempt().correct()) throw new AssertionError("Expected a correct attempt");
        if (r.load().load() < 0 || r.load().load() > 1) throw new AssertionError("Invalid load");

        engine.correctionMade();
        Question next = engine.chooseNext(r);
        if (next == null) throw new AssertionError("No adaptive next question");

        System.out.println("PASS: questions=" + ContentCatalog.questions().size()
                + ", firstSkill=" + q.getSkill()
                + ", firstState=" + r.load().state()
                + ", nextSkill=" + next.getSkill());
        engine.shutdown();
    }
}