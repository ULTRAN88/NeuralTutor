package com.neuraltutor;

import com.neuraltutor.content.ContentCatalog;
import com.neuraltutor.engine.TutorEngine;
import com.neuraltutor.model.LearnerProfile;
import com.neuraltutor.persistence.TutorRepository;
import com.neuraltutor.ui.NeuralTutorFrame;
import com.neuraltutor.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.*;

public final class Main {
    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> {
            TutorRepository repo = new TutorRepository();
            String[] profile = authenticate(repo);
            if (profile == null) System.exit(0);
            String name = profile[0];
            String goal = profile[1];

            LearnerProfile learner = repo.loadProfile(name.trim())
                    .orElse(new LearnerProfile(name.trim(), goal.trim()));
            learner.setGoal(goal.trim());

            TutorEngine engine = new TutorEngine(ContentCatalog.questions(), repo);
            NeuralTutorFrame frame = new NeuralTutorFrame(repo, engine, learner);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        });
    }

    private static String[] authenticate(TutorRepository repo) {
        JDialog dialog = new JDialog((Frame) null, "NeuralTutor", true);
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(Theme.PANEL);
        root.setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel eyebrow = Theme.muted("NEURAL TUTOR");
        eyebrow.setForeground(Theme.ACCENT_2);
        heading.add(eyebrow);
        heading.add(Box.createVerticalStrut(8));
        JLabel title = Theme.title("Welcome to your learning space", 25);
        heading.add(title);
        heading.add(Box.createVerticalStrut(7));
        JLabel description = Theme.muted("Sign in to continue, or create a new learner account.");
        heading.add(description);
        root.add(heading, BorderLayout.NORTH);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setOpaque(false);
        JButton loginTab = Theme.button("Log in", true);
        JButton registerTab = Theme.button("Register", false);
        tabs.add(loginTab);
        tabs.add(registerTab);

        JTextField loginName = styledField("Learner");
        JPasswordField loginPassword = styledPassword();
        JTextField registerName = styledField("");
        JTextField registerGoal = styledField("Master Java");
        JPasswordField registerPassword = styledPassword();
        JPasswordField registerConfirm = styledPassword();

        JPanel loginFields = fields(fieldLabel("NAME"), loginName,
            fieldLabel("PASSWORD"), loginPassword);
        JPanel registerFields = fields(fieldLabel("NAME"), registerName,
            fieldLabel("LEARNING GOAL"), registerGoal,
            fieldLabel("PASSWORD"), registerPassword,
            fieldLabel("CONFIRM PASSWORD"), registerConfirm);
        JPanel forms = new JPanel(new CardLayout());
        forms.setOpaque(false);
        forms.add(loginFields, "login");
        forms.add(registerFields, "register");
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(tabs, BorderLayout.NORTH);
        center.add(forms, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JLabel error = Theme.muted(" ");
        error.setForeground(Theme.DANGER);
        JButton action = Theme.button("Log in  →", true);
        JButton cancel = Theme.button("Cancel", false);
        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.add(error, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(action);
        actions.add(buttons, BorderLayout.EAST);
        root.add(actions, BorderLayout.SOUTH);

        String[] result = new String[2];
        boolean[] registering = {false};
        CardLayout cardLayout = (CardLayout) forms.getLayout();
        loginTab.addActionListener(e -> {
            registering[0] = false;
            cardLayout.show(forms, "login");
            loginTab.setBackground(Theme.ACCENT);
            registerTab.setBackground(Theme.PANEL_2);
            action.setText("Log in  →");
            error.setText(" ");
            dialog.pack();
            dialog.setSize(new Dimension(640, 470));
        });
        registerTab.addActionListener(e -> {
            registering[0] = true;
            cardLayout.show(forms, "register");
            loginTab.setBackground(Theme.PANEL_2);
            registerTab.setBackground(Theme.ACCENT);
            action.setText("Create account  →");
            error.setText(" ");
            dialog.pack();
            dialog.setSize(new Dimension(640, 700));
        });
        action.addActionListener(e -> {
            if (registering[0]) {
                String name = registerName.getText().trim();
                String password = new String(registerPassword.getPassword());
                if (name.isEmpty() || password.isBlank()) {
                    error.setText("Name and password are required.");
                    return;
                }
                if (!password.equals(new String(registerConfirm.getPassword()))) {
                    error.setText("Passwords do not match.");
                    return;
                }
                String goal = registerGoal.getText().trim().isEmpty() ? "Master Java" : registerGoal.getText().trim();
                if (!repo.register(name, password, goal)) {
                    error.setText("That learner name is already registered.");
                    return;
                }
                result[0] = name;
                result[1] = goal;
            } else {
                String name = loginName.getText().trim();
                if (!repo.authenticate(name, new String(loginPassword.getPassword()))) {
                    error.setText("Incorrect learner name or password.");
                    return;
                }
                result[0] = name;
                result[1] = repo.loadProfile(name).map(p -> p.getGoal()).orElse("Master Java");
            }
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());
        dialog.setContentPane(root);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setSize(new Dimension(640, 470));
        dialog.setLocationRelativeTo(null);
        loginName.requestFocusInWindow();
        dialog.setVisible(true);
        return result[0] == null ? null : result;
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = Theme.muted(text);
        label.setForeground(Theme.ACCENT_2);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        return label;
    }

    private static JTextField styledField(String value) {
        JTextField field = new JTextField(value);
        field.setUI(new BasicTextFieldUI());
        field.setFont(new Font("SansSerif", Font.PLAIN, 18));
        field.setForeground(new Color(239, 243, 248));
        field.setBackground(Theme.PANEL_2);
        field.setCaretColor(new Color(239, 243, 248));
        field.setOpaque(true);
        field.setColumns(24);
        field.setHorizontalAlignment(SwingConstants.LEFT);
        field.setPreferredSize(new Dimension(540, 58));
        field.setMinimumSize(new Dimension(540, 58));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        field.setSelectionColor(Theme.ACCENT);
        field.setSelectedTextColor(new Color(239, 243, 248));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(10, 12, 10, 12)));
        return field;
    }

    private static JPasswordField styledPassword() {
        JPasswordField field = new JPasswordField();
        field.setUI(new BasicTextFieldUI());
        field.setFont(new Font("SansSerif", Font.PLAIN, 18));
        field.setForeground(new Color(239, 243, 248));
        field.setBackground(Theme.PANEL_2);
        field.setCaretColor(new Color(239, 243, 248));
        field.setOpaque(true);
        field.setColumns(24);
        field.setHorizontalAlignment(SwingConstants.LEFT);
        field.setPreferredSize(new Dimension(540, 58));
        field.setMinimumSize(new Dimension(540, 58));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        field.setSelectionColor(Theme.ACCENT);
        field.setSelectedTextColor(new Color(239, 243, 248));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(10, 12, 10, 12)));
        return field;
    }

    private static JPanel fields(Component... components) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (Component component : components) {
            if (component instanceof JComponent jComponent) {
                jComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            panel.add(component);
            panel.add(Box.createVerticalStrut(8));
        }
        return panel;
    }
}