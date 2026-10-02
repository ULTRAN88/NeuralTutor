package com.neuraltutor.ui;

import com.neuraltutor.content.ContentCatalog;
import com.neuraltutor.engine.SkillGraph;
import com.neuraltutor.engine.TutorEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public final class LearnView extends JPanel {
    private final NeuralTutorFrame frame;
    private final TutorEngine engine;
    private final Map<String, ContentCatalog.Lesson> lessons;
    private final JPanel lessonDetail = new JPanel(new BorderLayout());

    public LearnView(NeuralTutorFrame frame, TutorEngine engine) {
        this.frame = frame;
        this.engine = engine;
        this.lessons = ContentCatalog.lessons();
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel page = UiKit.page("Learn Java",
                "Build understanding before testing it",
                "Each skill has a compact mental model, a focused explanation and an action. Practice then feeds the learner model.");
        JPanel split = new JPanel(new BorderLayout(18, 0));
        split.setOpaque(false);

        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(335, 0));
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        for (SkillGraph.Skill s : engine.skillGraph().all()) {
            JButton b = Theme.button(s.name() + "   " + Theme.pct(engine.learner().mastery(s.name())), false);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 47));
            b.addActionListener(e -> showLesson(s.name()));
            left.add(b);
            left.add(Box.createVerticalStrut(8));
        }
        split.add(UiKit.scroll(left), BorderLayout.WEST);

        lessonDetail.setOpaque(false);
        showLesson(engine.skillGraph().recommend(engine.learner()));
        split.add(lessonDetail, BorderLayout.CENTER);

        page.add(split, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }

    private void showLesson(String skill) {
        ContentCatalog.Lesson l = lessons.get(skill);
        lessonDetail.removeAll();

        JPanel content = Theme.card();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(Theme.muted("LESSON • " + skill.toUpperCase()));
        content.add(Box.createVerticalStrut(6));
        content.add(Theme.title(l.promise(), 24));
        content.add(Box.createVerticalStrut(18));
        content.add(section("EXPLANATION", l.explanation()));
        content.add(Box.createVerticalStrut(14));
        content.add(section("MENTAL MODEL", l.mentalModel()));
        content.add(Box.createVerticalStrut(14));
        content.add(section("ACTIVE LEARNING", l.activity()));
        content.add(Box.createVerticalStrut(22));

        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        action.setOpaque(false);
        JButton practice = Theme.button("Practice this skill  →", true);
        practice.addActionListener(e -> frame.openPractice(skill));
        action.add(practice);
        content.add(action);

        JLabel research = Theme.muted(
                "Adaptive rule: future questions consider mastery, latency, correction behavior and current task difficulty.");
        research.setBorder(new EmptyBorder(22, 0, 0, 0));
        content.add(research);

        lessonDetail.add(UiKit.scroll(content), BorderLayout.CENTER);
        lessonDetail.revalidate();
        lessonDetail.repaint();
    }

    private JPanel section(String heading, String body) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel h = Theme.muted(heading);
        h.setForeground(Theme.ACCENT_2);
        p.add(h);
        p.add(Box.createVerticalStrut(6));
        p.add(Theme.muted("<html><div style='width:620px;'>" + body + "</div></html>"));
        return p;
    }
}