package com.sqli.sandbox.ui;

import com.sqli.sandbox.model.LoginResult;
import com.sqli.sandbox.model.SecurityMode;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.List;

/**
 * Educational SQL Query Visualizer panel.
 * Clearly separates query templates from bound parameters, highlights injection points,
 * redacts credentials, and provides security explanations.
 */
public class QueryVisualizerPanel extends JPanel {

    private final JTextPane queryPane;
    private final JTextArea analysisArea;
    private final JLabel modeIndicator;

    public QueryVisualizerPanel() {
        setLayout(new BorderLayout(5, 5));
        setBackground(UITheme.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));

        // Top Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("SQL EXECUTION & QUERY VISUALIZER");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.PRIMARY_BLUE);

        modeIndicator = new JLabel("Status: Awaiting Query");
        modeIndicator.setFont(UITheme.FONT_CODE_BOLD);
        modeIndicator.setForeground(UITheme.TEXT_MUTED);

        top.add(title, BorderLayout.WEST);
        top.add(modeIndicator, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Center split: Query Pane (Top) and Analysis/Explanation Area (Bottom)
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerSize(4);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setOpaque(false);

        // Query text pane
        queryPane = new JTextPane();
        queryPane.setEditable(false);
        queryPane.setBackground(UITheme.BG_CODE);
        queryPane.setFont(UITheme.FONT_CODE);
        queryPane.setForeground(UITheme.TEXT_PRIMARY);
        queryPane.setMargin(new Insets(8, 10, 8, 10));

        JScrollPane queryScroll = new JScrollPane(queryPane);
        queryScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_SUBTLE));
        queryScroll.setPreferredSize(new Dimension(0, 110));

        // Analysis text area
        analysisArea = new JTextArea(4, 40);
        analysisArea.setEditable(false);
        analysisArea.setBackground(UITheme.BG_CARD);
        analysisArea.setFont(UITheme.FONT_CODE);
        analysisArea.setForeground(new Color(220, 225, 235));
        analysisArea.setLineWrap(true);
        analysisArea.setWrapStyleWord(true);
        analysisArea.setMargin(new Insets(6, 10, 6, 10));

        JScrollPane analysisScroll = new JScrollPane(analysisArea);
        analysisScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_SUBTLE),
            " ARCHITECTURAL & SECURITY ANALYSIS ",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 10),
            UITheme.TEXT_SECONDARY
        ));

        split.setTopComponent(queryScroll);
        split.setBottomComponent(analysisScroll);
        add(split, BorderLayout.CENTER);

        renderDefaultPlaceholder();
    }

    public void displayResult(LoginResult result, String inputUsername) {
        StyledDocument doc = queryPane.getStyledDocument();
        clearStyles(doc);

        Style styleBase = doc.addStyle("base", null);
        StyleConstants.setForeground(styleBase, new Color(170, 215, 245));
        StyleConstants.setFontFamily(styleBase, "Monospaced");
        StyleConstants.setFontSize(styleBase, 12);

        Style styleHighlight = doc.addStyle("highlight", null);
        StyleConstants.setForeground(styleHighlight, result.getMode() == SecurityMode.VULNERABLE ? UITheme.DANGER_RED : UITheme.SUCCESS_GREEN);
        StyleConstants.setFontFamily(styleHighlight, "Monospaced");
        StyleConstants.setFontSize(styleHighlight, 12);
        StyleConstants.setBold(styleHighlight, true);

        Style styleParams = doc.addStyle("params", null);
        StyleConstants.setForeground(styleParams, new Color(140, 230, 140));
        StyleConstants.setFontFamily(styleParams, "Monospaced");
        StyleConstants.setFontSize(styleParams, 11);

        try {
            doc.remove(0, doc.getLength());

            if (result.getMode() == SecurityMode.VULNERABLE) {
                modeIndicator.setText("Mode: VULNERABLE (Raw Statement Concatenation)");
                modeIndicator.setForeground(UITheme.DANGER_RED);

                String query = result.getExecutedQuery();
                doc.insertString(doc.getLength(), "-- [VULNERABLE DIRECT STRING CONCATENATION]\n", styleBase);

                // Highlight user input injection point
                if (inputUsername != null && !inputUsername.isEmpty() && query.contains(inputUsername)) {
                    int idx = query.indexOf(inputUsername);
                    doc.insertString(doc.getLength(), query.substring(0, idx), styleBase);
                    doc.insertString(doc.getLength(), inputUsername, styleHighlight);
                    doc.insertString(doc.getLength(), query.substring(idx + inputUsername.length()) + "\n", styleBase);
                } else {
                    doc.insertString(doc.getLength(), query + "\n", styleBase);
                }

                // Security Analysis Explanation
                StringBuilder sb = new StringBuilder();
                sb.append("• VULNERABILITY MECHANISM: Direct string interpolation into SQL statement.\n");
                sb.append("• PARSER IMPACT: The database engine compiles SQL AFTER user input is injected, allowing characters like ' and -- to reshape query logic.\n");
                if (result.isSyntaxError()) {
                    sb.append("• RESULT: Injection generated an invalid SQL command, throwing an unhandled SQLException.\n");
                } else if (result.isSuccess() && result.hasUsers()) {
                    sb.append("• RESULT: Exploit succeeded! Query returned ").append(result.getUsers().size()).append(" record(s) without valid credentials.\n");
                } else {
                    sb.append("• RESULT: Query executed but matched no rows.\n");
                }
                analysisArea.setText(sb.toString());

            } else {
                modeIndicator.setText("Mode: SECURE (Parameterized PreparedStatement)");
                modeIndicator.setForeground(UITheme.SUCCESS_GREEN);

                doc.insertString(doc.getLength(), "-- [SECURE PARAMETERIZED QUERY TEMPLATE]\n", styleBase);
                doc.insertString(doc.getLength(), result.getQueryTemplate() + "\n\n", styleHighlight);

                doc.insertString(doc.getLength(), "-- [SEPARATE PARAMETER BINDINGS]\n", styleBase);
                List<String> params = result.getBoundParameters();
                for (String p : params) {
                    doc.insertString(doc.getLength(), "  • " + p + "\n", styleParams);
                }

                // Security Analysis Explanation
                StringBuilder sb = new StringBuilder();
                sb.append("• DEFENSE MECHANISM: Parameterized PreparedStatement with placeholder binding (?)\n");
                sb.append("• PARSER IMPACT: The SQL template is pre-compiled by the database parser BEFORE parameters are bound.\n");
                sb.append("• BOUNDARY ENFORCEMENT: Parameter data is strictly treated as literal character values; injection syntax is neutralised.\n");
                sb.append("• CREDENTIAL SAFETY: Passwords verified in application memory using BCrypt; hashes are never exposed.\n");
                analysisArea.setText(sb.toString());
            }

        } catch (BadLocationException ignored) {
            queryPane.setText(result.getExecutedQuery());
        }

        queryPane.setCaretPosition(0);
        analysisArea.setCaretPosition(0);
    }

    public void clear() {
        renderDefaultPlaceholder();
    }

    private void renderDefaultPlaceholder() {
        queryPane.setText("-- Execute a login attempt or run test suite to visualize SQL statements here.");
        analysisArea.setText("Select Vulnerable or Secure mode and perform an action to inspect the underlying query architecture.");
        modeIndicator.setText("Status: Ready");
        modeIndicator.setForeground(UITheme.TEXT_MUTED);
    }

    private void clearStyles(StyledDocument doc) {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ignored) {}
    }
}
