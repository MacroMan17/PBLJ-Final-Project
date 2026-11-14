package com.sqli.sandbox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.function.BiConsumer;

/**
 * Educational cheat sheet presenting real-world SQL injection vectors,
 * why they work in vulnerable string concatenation, and how PreparedStatement mitigates them.
 */
public class CheatSheetPanel extends JPanel {

    private final BiConsumer<String, String> onLoadPayload;

    public CheatSheetPanel(BiConsumer<String, String> onLoadPayload) {
        this.onLoadPayload = onLoadPayload;

        setLayout(new BorderLayout(8, 8));
        setBackground(UITheme.BG_CARD);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel heading = new JLabel("⚠ COMMON SQL INJECTION ATTACK VECTORS & REMEDIATION");
        heading.setFont(UITheme.FONT_HEADER);
        heading.setForeground(UITheme.WARNING_ORANGE);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        add(heading, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setOpaque(false);

        // Card 1: Classic Tautology Bypass
        cardsPanel.add(createAttackCard(
            "' OR '1'='1' -- ",
            "anything",
            "Classic Tautology Bypass with Comment",
            "Injects an always-true OR condition ('1'='1') and comments out the remainder of the query (-- ), completely bypassing password verification in vulnerable mode.",
            "PreparedStatement binds the string as literal username data; the database searches for a user whose literal username equals the payload."
        ));
        cardsPanel.add(Box.createVerticalStrut(8));

        // Card 2: Targeted Account Takeover
        cardsPanel.add(createAttackCard(
            "lab_admin' -- ",
            "dummy_pass",
            "Targeted Account Takeover (Comment Truncation)",
            "Forces authentication as the specified user ('lab_admin') while commenting out the password predicate entirely. Allows attackers to log in as admin without knowing the password.",
            "PreparedStatement pre-compiles the query with fixed placeholders; comments cannot alter the execution tree."
        ));
        cardsPanel.add(Box.createVerticalStrut(8));

        // Card 3: Numeric Tautology
        cardsPanel.add(createAttackCard(
            "' OR 1=1 -- ",
            "x",
            "Numeric Tautology Injection",
            "Similar to '1'='1', this injects numeric evaluation 1=1. Evaluates to true for all rows in vulnerable mode.",
            "PreparedStatement passes 1=1 inside string quotes; treated as character data."
        ));
        cardsPanel.add(Box.createVerticalStrut(8));

        // Card 4: Syntax Error Probe
        cardsPanel.add(createAttackCard(
            "admin'",
            "test",
            "Single Quote Syntax Error Probe (Reconnaissance)",
            "An unmatched single quote breaks the SQL string literal boundary, resulting in an unhandled SQLException. Attackers use this to identify vulnerable input fields.",
            "PreparedStatement automatically escapes special characters, eliminating syntax errors for names like O'Connor."
        ));
        cardsPanel.add(Box.createVerticalStrut(8));

        // Card 5: UNION Based Injection
        cardsPanel.add(createAttackCard(
            "' UNION SELECT 99, 'hacked_admin', 'SUPERUSER' -- ",
            "x",
            "UNION-Based Row Injection",
            "Appends a secondary result set into the query output, allowing attackers to forge arbitrary user records or exfiltrate hidden tables.",
            "PreparedStatement rejects UNION operations because parameter placeholders cannot represent SQL commands."
        ));

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.BG_CARD);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createAttackCard(String payload, String pass, String title, String attackDesc, String defenseDesc) {
        JPanel card = new JPanel(new BorderLayout(10, 6));
        card.setBackground(new Color(20, 22, 34));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 45, 50)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        // Top info
        JPanel topInfo = new JPanel(new GridLayout(3, 1, 1, 1));
        topInfo.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_HEADER);
        titleLbl.setForeground(new Color(255, 110, 110));

        JLabel payloadLbl = new JLabel("Payload: " + payload + "  |  Password: " + pass);
        payloadLbl.setFont(UITheme.FONT_CODE_BOLD);
        payloadLbl.setForeground(new Color(245, 230, 120));

        JLabel descLbl = new JLabel("<html><span style='color:#a0aab8;font-size:10px;'><b>Exploit:</b> " + attackDesc + "</span></html>");
        topInfo.add(titleLbl);
        topInfo.add(payloadLbl);
        topInfo.add(descLbl);

        card.add(topInfo, BorderLayout.CENTER);

        // Buttons on the right
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        btnPanel.setOpaque(false);

        JButton copyBtn = UITheme.createStyledButton("Copy", new Color(38, 70, 45));
        copyBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        copyBtn.setPreferredSize(new Dimension(85, 24));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(payload), null);
            copyBtn.setText("✔ Copied");
            Timer t = new Timer(1200, ev -> copyBtn.setText("Copy"));
            t.setRepeats(false);
            t.start();
        });

        JButton loadBtn = UITheme.createStyledButton("Load In Form", new Color(40, 75, 140));
        loadBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        loadBtn.setPreferredSize(new Dimension(85, 24));
        loadBtn.addActionListener(e -> {
            if (onLoadPayload != null) {
                onLoadPayload.accept(payload, pass);
            }
        });

        btnPanel.add(copyBtn);
        btnPanel.add(loadBtn);

        card.add(btnPanel, BorderLayout.EAST);
        return card;
    }
}
