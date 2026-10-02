package com.neuraltutor.ui;

import com.neuraltutor.engine.CognitiveLoadEngine;
import com.neuraltutor.engine.TutorEngine;
import com.neuraltutor.model.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public final class PracticeView extends JPanel {
    private final NeuralTutorFrame frame;
    private final TutorEngine engine;
    private final String requestedSkill;

    private Question question;
    private TutorEngine.InteractionResult lastResult;
    private final JLabel qCount = new JLabel();
    private final JLabel skillLabel = new JLabel();
    private final JLabel difficultyLabel = new JLabel();
    private final JLabel timerLabel = new JLabel("0.0s");
    private final JLabel loadLabel = new JLabel("Load —");
    private final JProgressBar loadBar = Theme.progress(0);
    private final JTextArea title = new JTextArea();
    private final JTextArea prompt = new JTextArea();
    private final JPanel optionsPanel = new JPanel();
    private final JLabel feedback = new JLabel();
    private final JLabel intervention = new JLabel();
    private final JLabel hint = new JLabel();
    private final JButton submit = Theme.button("Submit answer", true);
    private final JButton next = Theme.button("Next adaptive question  →", false);
    private final JButton hintButton = Theme.button("Show hint", false);
    private final ButtonGroup group = new ButtonGroup();
    private final List<JRadioButton> options = new ArrayList<>();
    private int lastSelected = -1;
    private long shownAt;
    private ScheduledFuture<?> ticker;

    public PracticeView(NeuralTutorFrame frame, TutorEngine engine) {
        this(frame, engine, null);
    }

    public PracticeView(NeuralTutorFrame frame, TutorEngine engine, String requestedSkill) {
        this.frame = frame;
        this.engine = engine;
        this.requestedSkill = requestedSkill;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel page = UiKit.page("Adaptive practice",
                "The engine is listening to the process, not only the answer",
                "Every interaction produces evidence: response latency, corrections, correctness, difficulty and concept.");
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);

        body.add(topBar(), BorderLayout.NORTH);
        body.add(questionCard(), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);

        begin();
        startTicker();
    }

    private JPanel topBar() {
        JPanel p = Theme.card();
        p.setLayout(new BorderLayout(12, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        skillLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        skillLabel.setForeground(Theme.ACCENT_2);
        difficultyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        difficultyLabel.setForeground(Theme.MUTED);
        left.add(skillLabel);
        left.add(difficultyLabel);
        p.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout(8, 0));
        center.setOpaque(false);
        center.add(loadLabel, BorderLayout.WEST);
        center.add(loadBar, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);

        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        timerLabel.setForeground(Theme.TEXT);
        p.add(timerLabel, BorderLayout.EAST);
        return p;
    }

    private JPanel questionCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 18));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(qCount, BorderLayout.WEST);
        hintButton.addActionListener(e -> showHint());
        head.add(hintButton, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel main = new JPanel();
        main.setOpaque(false);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setEditable(false);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Theme.TEXT);
        title.setBackground(Theme.PANEL);
        title.setBorder(BorderFactory.createEmptyBorder());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(title);
        main.add(Box.createVerticalStrut(12));

        prompt.setEditable(false);
        prompt.setLineWrap(true);
        prompt.setWrapStyleWord(true);
        prompt.setFont(new Font("SansSerif", Font.PLAIN, 18));
        prompt.setForeground(Theme.TEXT);
        prompt.setBackground(Theme.PANEL);
        prompt.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        prompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(prompt);

        optionsPanel.setOpaque(false);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(optionsPanel);
        main.add(Box.createVerticalStrut(12));

        feedback.setFont(new Font("SansSerif", Font.BOLD, 15));
        main.add(feedback);
        main.add(Box.createVerticalStrut(7));
        intervention.setFont(new Font("SansSerif", Font.PLAIN, 13));
        main.add(intervention);
        main.add(Box.createVerticalStrut(5));
        hint.setFont(new Font("SansSerif", Font.PLAIN, 13));
        hint.setForeground(Theme.MUTED);
        main.add(hint);

        JScrollPane questionScroll = UiKit.scroll(main);
        questionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        questionScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        card.add(questionScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        submit.addActionListener(e -> submit());
        next.setEnabled(false);
        next.addActionListener(e -> nextQuestion());
        actions.add(submit);
        actions.add(next);
        JButton exit = Theme.button("End session", false);
        exit.addActionListener(e -> {
            engine.endSession();
            frame.showPage(NeuralTutorFrame.HOME);
        });
        actions.add(exit);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    private void begin() {
        if (requestedSkill != null) {
            question = engine.nextForSkill(requestedSkill, Math.max(1,
                    Math.min(5, (int)Math.round(engine.learner().mastery(requestedSkill) * 5) + 1)));
        } else {
            question = engine.startAdaptiveSession();
        }
        renderQuestion();
    }

    private void renderQuestion() {
        shownAt = System.currentTimeMillis();
        lastSelected = -1;
        feedback.setText("");
        intervention.setText("");
        hint.setText("");
        submit.setEnabled(true);
        next.setEnabled(false);
        hintButton.setEnabled(true);

        group.clearSelection();
        options.clear();
        optionsPanel.removeAll();

        skillLabel.setText(question.getSkill());
        difficultyLabel.setText(question.difficultyLabel() + "  •  D" + question.getDifficulty());
        qCount.setText("Q" + question.getId() + "  •  Adaptive");
        title.setText(question.getPrompt());
        prompt.setText(question.getExplanation().contains("Think:") ? question.getPrompt() : "");

        if (question instanceof MCQQuestion mcq) {
            int idx = 0;
            for (String option : mcq.getOptions()) {
                JRadioButton rb = new JRadioButton((char)('A' + idx) + "   " + option);
                rb.setActionCommand(String.valueOf(idx));
                rb.setOpaque(true);
                rb.setBackground(Theme.PANEL_2);
                rb.setForeground(Theme.TEXT);
                rb.setBorder(new CompoundBorder(
                        new LineBorder(Theme.BORDER),
                        new EmptyBorder(13, 14, 13, 14)));
                rb.setFocusPainted(false);
                final int chosen = idx;
                rb.addActionListener(e -> {
                    if (lastSelected != -1 && lastSelected != chosen) {
                        engine.correctionMade();
                    }
                    lastSelected = chosen;
                });
                group.add(rb);
                options.add(rb);
                rb.setAlignmentX(Component.LEFT_ALIGNMENT);
                optionsPanel.add(rb);
                optionsPanel.add(Box.createVerticalStrut(8));
                idx++;
            }
        }
        loadBar.setValue(0);
        loadLabel.setText("Live load — waiting for evidence");
        loadLabel.setForeground(Theme.MUTED);
        revalidate();
        repaint();
    }

    private void submit() {
        if (group.getSelection() == null) {
            JOptionPane.showMessageDialog(this, "Choose an answer before submitting.",
                    "NeuralTutor", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int selected = Integer.parseInt(group.getSelection().getActionCommand());
        lastResult = engine.submit(selected);
        submit.setEnabled(false);
        next.setEnabled(true);
        hintButton.setEnabled(false);

        Attempt a = lastResult.attempt();
        if (a.correct()) {
            feedback.setText("✓ Correct. " + question.getExplanation());
            feedback.setForeground(Theme.ACCENT_2);
        } else {
            String answer = question instanceof MCQQuestion m ?
                    m.getOptions().get(m.getCorrectOption()) : "Review the concept.";
            feedback.setText("✗ Not quite. The best answer is: " + answer);
            feedback.setForeground(Theme.DANGER);
        }

        CognitiveLoadEngine.Snapshot s = lastResult.load();
        loadBar.setValue((int)Math.round(s.load() * 100));
        loadLabel.setText(s.state().getLabel() + " • " + Theme.pct(s.load()));
        loadLabel.setForeground(s.state() == CognitiveState.OVERLOAD ? Theme.DANGER :
                s.state() == CognitiveState.HIGH_LOAD ? Theme.WARNING : Theme.ACCENT_2);

        AdaptiveMode mode = lastResult.decision().mode();
        intervention.setText("<html><b>" + lastResult.decision().title() + "</b> — "
                + lastResult.decision().message() + "</html>");
        intervention.setForeground(Theme.TEXT);
        hint.setText("<html>Engine rationale: " + lastResult.decision().rationale()
                + " &nbsp; | &nbsp; latency " + (a.latencyMs()/1000.0)
                + "s &nbsp; | &nbsp; corrections " + a.corrections() + "</html>");
    }

    private void nextQuestion() {
        question = engine.chooseNext(lastResult);
        renderQuestion();
    }

    private void showHint() {
        hint.setText("<html><b>Hint:</b> " + question.getHint() + "</html>");
        hint.setForeground(Theme.WARNING);
    }

    private void startTicker() {
        ticker = engine.schedule(() -> {
            long elapsed = System.currentTimeMillis() - shownAt;
            SwingUtilities.invokeLater(() -> timerLabel.setText(String.format(java.util.Locale.US,
                    "%.1fs", elapsed / 1000.0)));
        }, 0, 200);
    }

    public void stopTicker() {
        if (ticker != null) ticker.cancel(false);
    }
}