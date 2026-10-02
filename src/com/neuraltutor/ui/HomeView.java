package com.neuraltutor.ui;

import com.neuraltutor.engine.SkillGraph;
import com.neuraltutor.engine.TutorEngine;
import com.neuraltutor.model.LearnerProfile;
import com.neuraltutor.persistence.TutorRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public final class HomeView extends JPanel {
    private final NeuralTutorFrame frame;
    private final TutorEngine engine;

    public HomeView(NeuralTutorFrame frame, TutorEngine engine, TutorRepository repo) {
        this.frame = frame;
        this.engine = engine;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel page = UiKit.page("Personal learning cockpit",
                "Welcome back, " + engine.learner().getName(),
                "NeuralTutor is adapting the next learning action from your knowledge state and response behavior.");

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        body.add(statsRow());
        body.add(Box.createVerticalStrut(18));
        body.add(heroCard());
        body.add(Box.createVerticalStrut(18));
        body.add(skillMap());
        body.add(Box.createVerticalStrut(18));
        body.add(nextPath());

        page.add(UiKit.scroll(body), BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }

    private JPanel statsRow() {
        LearnerProfile p = engine.learner();
        return UiKit.row(stat("Mastery", Theme.pct(p.overallMastery()), "Across learned skills"),
                stat("Accuracy", p.totalAttempts() == 0 ? "—" :
                        String.format("%.0f%%", 100.0 * p.totalCorrect() / p.totalAttempts()),
                        p.totalAttempts() + " interactions"),
                stat("XP", String.valueOf(p.getXp()), "Adaptive practice"),
                stat("Streak", p.getStreak() + " days", "Keep the habit"));
    }

    private JPanel stat(String label, String value, String note) {
        JPanel p = Theme.card();
        p.setPreferredSize(new Dimension(215, 120));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(Theme.muted(label.toUpperCase()));
        p.add(Box.createVerticalGlue());
        p.add(UiKit.statValue(value));
        p.add(Box.createVerticalStrut(5));
        p.add(Theme.muted(note));
        return p;
    }

    private JPanel heroCard() {
        JPanel p = Theme.card();
        p.setLayout(new BorderLayout(22, 0));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel label = Theme.muted("TODAY'S ADAPTIVE PATH");
        label.setForeground(Theme.ACCENT);
        left.add(label);
        left.add(Box.createVerticalStrut(6));
        left.add(Theme.title(engine.skillGraph().recommend(engine.learner()), 24));
        left.add(Box.createVerticalStrut(7));
        left.add(Theme.muted("The engine will start with a manageable question, establish your baseline, and adjust difficulty or support from the evidence it observes."));
        left.add(Box.createVerticalStrut(16));
        JButton start = Theme.button("Start adaptive learning  →", true);
        start.addActionListener(e -> frame.showPage(NeuralTutorFrame.PRACTICE));
        left.add(start);
        p.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(270, 110));
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(Theme.title("Observe → infer → adapt", 16));
        right.add(Box.createVerticalStrut(8));
        right.add(Theme.muted("Latency"));
        right.add(Theme.muted("Corrections"));
        right.add(Theme.muted("Accuracy"));
        right.add(Theme.muted("Difficulty"));
        right.add(Box.createVerticalStrut(6));
        right.add(Theme.muted("↓"));
        right.add(Theme.title("Next best action", 16));
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel skillMap() {
        JPanel outer = Theme.card();
        outer.setLayout(new BorderLayout());
        outer.add(Theme.title("Your skill map", 21), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        for (SkillGraph.Skill s : engine.skillGraph().all()) {
            JPanel item = new JPanel();
            item.setBackground(Theme.PANEL_2);
            item.setBorder(new EmptyBorder(12, 14, 12, 14));
            item.setLayout(new BorderLayout(10, 8));

            JLabel name = new JLabel(s.name());
            name.setFont(new Font("SansSerif", Font.BOLD, 13));
            name.setForeground(Theme.TEXT);
            item.add(name, BorderLayout.NORTH);

            JProgressBar bar = Theme.progress(engine.learner().mastery(s.name()));
            item.add(bar, BorderLayout.CENTER);

            JLabel value = Theme.muted(Theme.pct(engine.learner().mastery(s.name())));
            item.add(value, BorderLayout.EAST);

            grid.add(item);
        }
        outer.add(grid, BorderLayout.CENTER);
        return outer;
    }

    private JPanel nextPath() {
        JPanel p = Theme.card();
        p.setLayout(new BorderLayout());
        p.add(Theme.title("Why this path?", 21), BorderLayout.NORTH);
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        List<SkillGraph.Skill> path = engine.skillGraph().nextPath(engine.learner(), 3);
        for (SkillGraph.Skill s : path) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(10, 0, 10, 0));
            row.add(Theme.title("•", 18), BorderLayout.WEST);
            JLabel text = new JLabel("<html><b>" + s.name() + "</b> — " + s.description() + "</html>");
            text.setForeground(Theme.TEXT);
            row.add(text, BorderLayout.CENTER);
            list.add(row);
        }
        p.add(list, BorderLayout.CENTER);
        return p;
    }
}