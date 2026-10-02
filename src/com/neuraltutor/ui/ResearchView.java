package com.neuraltutor.ui;

import com.neuraltutor.engine.TutorEngine;
import com.neuraltutor.model.Attempt;
import com.neuraltutor.persistence.TutorRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ResearchView extends JPanel {
    private final NeuralTutorFrame frame;
    private final TutorEngine engine;
    private final TutorRepository repo;

    public ResearchView(NeuralTutorFrame frame, TutorEngine engine, TutorRepository repo) {
        this.frame = frame;
        this.engine = engine;
        this.repo = repo;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel page = UiKit.page("Research lab",
                "Observe the feedback loop",
                "This screen is deliberately transparent: you can inspect the behavioral signals and instructional actions that drove adaptation.");

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(metrics(), BorderLayout.CENTER);
        JButton export = Theme.button("Export experiment CSV", false);
        export.addActionListener(e -> exportCsv());
        top.add(export, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(timeline(), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }

    private JPanel metrics() {
        List<Attempt> a = repo.recentAttempts(engine.learner().getName(), 500);
        double avgLoad = a.stream().mapToDouble(Attempt::loadScore).average().orElse(0);
        double avgLatency = a.stream().mapToLong(Attempt::latencyMs).average().orElse(0);
        Map<String,Integer> counts = repo.interventionCounts(engine.learner().getName());

        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        p.add(metric("Observed attempts", String.valueOf(a.size())));
        p.add(metric("Average load", Theme.pct(avgLoad)));
        p.add(metric("Average latency", String.format("%.1fs", avgLatency/1000.0)));
        p.add(metric("Top intervention", counts.isEmpty() ? "—" : counts.keySet().iterator().next()));
        return p;
    }

    private JPanel metric(String label, String value) {
        JPanel p = Theme.card();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(Theme.muted(label.toUpperCase()));
        p.add(Box.createVerticalGlue());
        p.add(Theme.title(value, 20));
        return p;
    }

    private JPanel timeline() {
        List<Attempt> list = repo.recentAttempts(engine.learner().getName(), 50);
        String[] cols = {"Time", "Skill", "Result", "Latency", "Corrections", "Load", "State", "Intervention"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        for (Attempt a : list) {
            model.addRow(new Object[]{
                    new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(a.timestamp())),
                    a.skill(),
                    a.correct() ? "Correct" : "Wrong",
                    String.format("%.1fs", a.latencyMs()/1000.0),
                    a.corrections(),
                    Theme.pct(a.loadScore()),
                    a.cognitiveState().getLabel(),
                    a.intervention().label()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.PANEL);
        table.setGridColor(Theme.BORDER);
        table.getTableHeader().setBackground(Theme.PANEL_2);
        table.getTableHeader().setForeground(Theme.TEXT);

        JPanel p = Theme.card();
        p.setLayout(new BorderLayout());
        p.add(Theme.title("Interaction timeline", 19), BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("neuraltutor-experiment.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            repo.exportAttemptsCsv(engine.learner().getName(), chooser.getSelectedFile().toPath());
            JOptionPane.showMessageDialog(this, "Experiment data exported.",
                    "NeuralTutor", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}