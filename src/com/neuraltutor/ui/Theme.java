package com.neuraltutor.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public final class Theme {
    public static final Color BG = new Color(15, 18, 24);
    public static final Color PANEL = new Color(23, 28, 37);
    public static final Color PANEL_2 = new Color(29, 35, 46);
    public static final Color TEXT = new Color(239, 243, 248);
    public static final Color MUTED = new Color(151, 162, 178);
    public static final Color ACCENT = new Color(95, 125, 255);
    public static final Color ACCENT_2 = new Color(71, 218, 172);
    public static final Color WARNING = new Color(244, 179, 72);
    public static final Color DANGER = new Color(242, 102, 120);
    public static final Color BORDER = new Color(50, 59, 74);

    private Theme() {}

    public static void install() {
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", PANEL_2);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.border", new LineBorder(BORDER));
        UIManager.put("TextField.background", PANEL_2);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("ComboBox.background", PANEL_2);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
    }

    public static JLabel title(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(MUTED);
        return l;
    }

    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(primary ? ACCENT : BORDER),
                new EmptyBorder(11, 16, 11, 16)));
        b.setBackground(primary ? ACCENT : PANEL_2);
        b.setForeground(TEXT);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(18, 18, 18, 18)));
        return p;
    }

    public static JProgressBar progress(double value) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int)Math.round(value * 100));
        bar.setStringPainted(false);
        bar.setBorderPainted(false);
        bar.setBackground(PANEL_2);
        bar.setForeground(ACCENT_2);
        bar.setPreferredSize(new Dimension(220, 9));
        return bar;
    }

    public static String pct(double x) {
        return String.format(java.util.Locale.US, "%.0f%%", x * 100);
    }
}