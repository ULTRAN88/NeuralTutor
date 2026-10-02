package com.neuraltutor.ui;

import com.neuraltutor.engine.KnowledgeTracingEngine;
import com.neuraltutor.engine.SkillGraph;
import com.neuraltutor.engine.TutorEngine;
import com.neuraltutor.persistence.TutorRepository;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public final class ProgressView extends JPanel {
    public ProgressView(NeuralTutorFrame frame, TutorEngine engine, TutorRepository repo) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel page = UiKit.page("Progress intelligence",
                "See what you know — and where the effort is going",
                "Mastery is tracked separately from cognitive load so the system can distinguish 'I don't know this' from 'this is too much right now'.");

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        KnowledgeTracingEngine kt = new KnowledgeTracingEngine();
        for (SkillGraph.Skill s : engine.skillGraph().all()) {
            double m = engine.learner().mastery(s.name());
            JPanel row = Theme.card();
            row.setLayout(new BorderLayout(14, 0));

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setPreferredSize(new Dimension(300, 70));
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(Theme.title(s.name(), 15));
            text.add(Box.createVerticalStrut(5));
            text.add(Theme.muted(kt.level(m) + "  •  "
                    + engine.learner().stats(s.name()).getAttempts() + " attempts"));
            row.add(text, BorderLayout.WEST);

            JPanel bar = new JPanel();
            bar.setOpaque(false);
            bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
            JProgressBar pb = Theme.progress(m);
            pb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 11));
            bar.add(pb);
            bar.add(Box.createVerticalStrut(8));
            bar.add(Theme.muted("Avg response " +
                    formatLatency(engine.learner().averageLatency(s.name()))));
            row.add(bar, BorderLayout.CENTER);

            JLabel val = Theme.title(Theme.pct(m), 18);
            row.add(val, BorderLayout.EAST);

            list.add(row);
            list.add(Box.createVerticalStrut(10));
        }

        page.add(UiKit.scroll(list), BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }

    private String formatLatency(double ms) {
        if (ms <= 0) return "—";
        return String.format(java.util.Locale.US, "%.1fs", ms/1000.0);
    }
}