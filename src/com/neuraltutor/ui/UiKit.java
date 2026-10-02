package com.neuraltutor.ui;

import javax.swing.*;
import java.awt.*;

public final class UiKit {
    private UiKit() {}

    public static JPanel page(String eyebrow, String title, String description) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(28, 34, 34, 34));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel e = Theme.muted(eyebrow.toUpperCase());
        e.setForeground(Theme.ACCENT_2);
        head.add(e);
        head.add(Box.createVerticalStrut(4));
        head.add(Theme.title(title, 30));
        head.add(Box.createVerticalStrut(7));
        head.add(Theme.muted(description));
        root.add(head, BorderLayout.NORTH);
        return root;
    }

    public static JLabel statValue(String value) {
        JLabel l = Theme.title(value, 29);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public static JPanel stack(Component... components) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (Component c : components) {
            p.add(c);
            p.add(Box.createVerticalStrut(10));
        }
        return p;
    }

    public static JPanel row(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        for (Component c : components) p.add(c);
        return p;
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getViewport().setBackground(Theme.BG);
        s.getVerticalScrollBar().setUnitIncrement(18);
        return s;
    }
}