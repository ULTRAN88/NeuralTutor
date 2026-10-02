package com.neuraltutor.ui;

import com.neuraltutor.engine.TutorEngine;
import com.neuraltutor.model.Attempt;
import com.neuraltutor.persistence.TutorRepository;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public final class MistakesView extends JPanel {
    public MistakesView(NeuralTutorFrame frame, TutorEngine engine, TutorRepository repo) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel page = UiKit.page("Mistake notebook",
                "Turn errors into the next lesson",
                "Wrong answers are not treated as failure flags. They become targeted evidence for remediation, review and future sequencing.");

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        List<Attempt> attempts = repo.recentAttempts(engine.learner().getName(), 80);
        attempts.stream().filter(a -> !a.correct()).forEach(a -> {
            JPanel card = Theme.card();
            card.setLayout(new BorderLayout(14, 0));

            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.add(Theme.title(a.skill(), 16));
            left.add(Box.createVerticalStrut(6));
            left.add(Theme.muted("Question #" + a.questionId() +
                    "  •  " + a.difficulty() +
                    "  •  " + String.format("%.1fs", a.latencyMs()/1000.0)));
            card.add(left, BorderLayout.CENTER);

            JPanel right = new JPanel();
            right.setOpaque(false);
            right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
            JLabel state = Theme.muted(a.cognitiveState().getLabel());
            state.setForeground(a.cognitiveState() == com.neuraltutor.model.CognitiveState.OVERLOAD ?
                    Theme.DANGER : Theme.WARNING);
            right.add(state);
            right.add(Box.createVerticalStrut(5));
            right.add(Theme.muted(a.intervention().label()));
            card.add(right, BorderLayout.EAST);

            list.add(card);
            list.add(Box.createVerticalStrut(10));
        });

        if (list.getComponentCount() == 0) {
            JPanel empty = Theme.card();
            empty.add(Theme.title("Your mistake notebook is empty.", 20));
            list.add(empty);
        }

        page.add(UiKit.scroll(list), BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }
}