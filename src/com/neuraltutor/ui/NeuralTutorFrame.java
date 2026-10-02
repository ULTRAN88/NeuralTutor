package com.neuraltutor.ui;

import com.neuraltutor.content.ContentCatalog;
import com.neuraltutor.engine.TutorEngine;
import com.neuraltutor.model.LearnerProfile;
import com.neuraltutor.persistence.TutorRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public final class NeuralTutorFrame extends JFrame {
    public static final String HOME = "HOME";
    public static final String LEARN = "LEARN";
    public static final String PRACTICE = "PRACTICE";
    public static final String PROGRESS = "PROGRESS";
    public static final String MISTAKES = "MISTAKES";
    public static final String RESEARCH = "RESEARCH";

    private final TutorRepository repository;
    private final TutorEngine engine;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel profileName = new JLabel();
    private final JLabel profileGoal = new JLabel();

    public NeuralTutorFrame(TutorRepository repository, TutorEngine engine, LearnerProfile learner) {
        this.repository = repository;
        this.engine = engine;
        engine.startLearner(learner.getName(), learner.getGoal());

        setTitle("NeuralTutor — Cognitive Learning OS");
        setSize(1420, 900);
        setMinimumSize(new Dimension(1100, 760));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);

        buildShell();
        showPage(HOME);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                engine.endSession();
                engine.shutdown();
            }
        });
    }

    private void buildShell() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);

        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(11, 14, 19));
        sidebar.setBorder(new EmptyBorder(22, 16, 22, 16));
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel brand = Theme.title("NEURAL", 24);
        JLabel brand2 = Theme.title("TUTOR", 24);
        brand.setForeground(Theme.ACCENT_2);
        sidebar.add(brand);
        sidebar.add(brand2);
        sidebar.add(Box.createVerticalStrut(5));
        JLabel tag = Theme.muted("Cognitive learning OS");
        sidebar.add(tag);
        sidebar.add(Box.createVerticalStrut(28));

        addNav(sidebar, "Home", HOME);
        addNav(sidebar, "Learn", LEARN);
        addNav(sidebar, "Adaptive Practice", PRACTICE);
        addNav(sidebar, "Progress", PROGRESS);
        addNav(sidebar, "Mistakes", MISTAKES);
        addNav(sidebar, "Research Lab", RESEARCH);

        sidebar.add(Box.createVerticalGlue());

        JPanel profile = Theme.card();
        profile.setLayout(new BoxLayout(profile, BoxLayout.Y_AXIS));
        profileName.setFont(new Font("SansSerif", Font.BOLD, 14));
        profileName.setForeground(Theme.TEXT);
        profileGoal.setFont(new Font("SansSerif", Font.PLAIN, 11));
        profileGoal.setForeground(Theme.MUTED);
        profileGoal.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileName.setAlignmentX(Component.LEFT_ALIGNMENT);
        updateProfileLabels();
        profile.add(profileName);
        profile.add(Box.createVerticalStrut(5));
        profile.add(profileGoal);
        profile.add(Box.createVerticalStrut(12));
        JButton switchUser = Theme.button("Edit profile", false);
        switchUser.addActionListener(e -> editProfile());
        switchUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        profile.add(switchUser);
        sidebar.add(profile);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.BG);
        top.setBorder(new EmptyBorder(14, 24, 10, 24));
        JLabel system = Theme.muted("LIVE ADAPTIVE SESSION  •  JAVA 21  •  LOCAL-FIRST");
        system.setForeground(Theme.ACCENT_2);
        top.add(system, BorderLayout.WEST);

        content.setBackground(Theme.BG);
        root.add(sidebar, BorderLayout.WEST);
        root.add(top, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void addNav(JPanel sidebar, String label, String page) {
        JButton b = Theme.button(label, false);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> showPage(page));
        sidebar.add(b);
        sidebar.add(Box.createVerticalStrut(8));
    }

    public void showPage(String page) {
        stopActivePractice();
        content.removeAll();
        switch (page) {
            case HOME -> content.add(new HomeView(this, engine, repository), BorderLayout.CENTER);
            case LEARN -> content.add(new LearnView(this, engine), BorderLayout.CENTER);
            case PRACTICE -> content.add(new PracticeView(this, engine), BorderLayout.CENTER);
            case PROGRESS -> content.add(new ProgressView(this, engine, repository), BorderLayout.CENTER);
            case MISTAKES -> content.add(new MistakesView(this, engine, repository), BorderLayout.CENTER);
            case RESEARCH -> content.add(new ResearchView(this, engine, repository), BorderLayout.CENTER);
            default -> content.add(new HomeView(this, engine, repository), BorderLayout.CENTER);
        }
        content.revalidate();
        content.repaint();
        updateProfileLabels();
    }

    public void openPractice(String skill) {
        stopActivePractice();
        content.removeAll();
        content.add(new PracticeView(this, engine, skill), BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    public void refresh() {
        showPage(HOME);
    }

    public void editProfile() {
        JTextField name = new JTextField(engine.learner().getName());
        JTextField goal = new JTextField(engine.learner().getGoal());
        JPanel p = new JPanel(new GridLayout(0,1,6,6));
        p.add(new JLabel("Name"));
        p.add(name);
        p.add(new JLabel("Learning goal"));
        p.add(goal);
        int result = JOptionPane.showConfirmDialog(this, p, "Learner profile",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            // The name intentionally stays stable because it is the persistence key.
            engine.learner().setGoal(goal.getText().trim());
            repository.saveProfile(engine.learner());
            updateProfileLabels();
        }
    }

    private void updateProfileLabels() {
        if (engine.learner() == null) return;
        profileName.setText(engine.learner().getName());
        profileGoal.setText(engine.learner().getGoal());
    }

    private void stopActivePractice() {
        if (content.getComponentCount() > 0 && content.getComponent(0) instanceof PracticeView pv) {
            pv.stopTicker();
        }
    }

    public TutorRepository repository() { return repository; }
    public TutorEngine engine() { return engine; }
}