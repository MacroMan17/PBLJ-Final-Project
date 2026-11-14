package com.sqli.sandbox.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Centralized UI design system providing consistent color tokens, typography,
 * borders, and component styling.
 */
public final class UITheme {

    // Background Colors
    public static final Color BG_DARK = new Color(15, 17, 26);
    public static final Color BG_CARD = new Color(24, 26, 42);
    public static final Color BG_INPUT = new Color(32, 35, 54);
    public static final Color BG_CODE = new Color(10, 11, 20);

    // Accent Colors
    public static final Color PRIMARY_BLUE = new Color(88, 166, 255);
    public static final Color SUCCESS_GREEN = new Color(63, 185, 80);
    public static final Color WARNING_ORANGE = new Color(240, 136, 62);
    public static final Color DANGER_RED = new Color(248, 81, 73);
    public static final Color PURPLE_ACCENT = new Color(163, 113, 247);

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(240, 246, 252);
    public static final Color TEXT_SECONDARY = new Color(139, 148, 158);
    public static final Color TEXT_MUTED = new Color(110, 118, 129);

    // Border Colors
    public static final Color BORDER_SUBTLE = new Color(48, 54, 76);
    public static final Color BORDER_ACTIVE = new Color(88, 166, 255);

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_CODE = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONT_CODE_BOLD = new Font("Monospaced", Font.BOLD, 12);

    private UITheme() {}

    public static JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return btn;
    }

    public static void styleTextField(JTextField field) {
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(Color.WHITE);
        field.setFont(FONT_CODE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    public static TitledBorder createTitledBorder(String title, Color accentColor) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE),
            " " + title + " ",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            FONT_HEADER,
            accentColor
        );
    }
}
