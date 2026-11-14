package com.sqli.sandbox.ui;

import com.sqli.sandbox.model.LoginResult;
import com.sqli.sandbox.model.SecurityMode;
import com.sqli.sandbox.model.User;
import com.sqli.sandbox.service.AuthenticationService;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Interactive login panel for testing individual credentials and injection payloads.
 * Executes database operations off the EDT using SwingWorker to prevent UI freezing.
 */
public class LoginFormPanel extends JPanel {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JToggleButton modeToggle;
    private final JPanel warningBanner;
    private final JButton loginBtn;
    private final JButton clearBtn;
    private final JTextArea resultArea;

    private AuthenticationService authService;
    private final java.util.function.BiConsumer<LoginResult, String> onResultGenerated;

    public LoginFormPanel(AuthenticationService authService, java.util.function.BiConsumer<LoginResult, String> onResultGenerated) {
        this.authService = authService;
        this.onResultGenerated = onResultGenerated;

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_CARD);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // Form inputs panel
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);

        // 1. Warning banner (Visible only in VULNERABLE mode)
        warningBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        warningBanner.setBackground(new Color(60, 20, 20));
        warningBanner.setBorder(BorderFactory.createLineBorder(UITheme.DANGER_RED));
        JLabel warningLabel = new JLabel("⚠ WARNING: VULNERABLE MODE ACTIVE — String concatenation allows SQL structure to be altered!");
        warningLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        warningLabel.setForeground(new Color(255, 180, 180));
        warningBanner.add(warningLabel);
        formContainer.add(warningBanner);
        formContainer.add(Box.createVerticalStrut(8));

        // 2. Mode Selector
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modePanel.setOpaque(false);
        modeToggle = new JToggleButton("🔓  VULNERABLE MODE (Statement Concatenation)");
        modeToggle.setFont(UITheme.FONT_CODE_BOLD);
        modeToggle.setBackground(new Color(180, 40, 40));
        modeToggle.setForeground(Color.WHITE);
        modeToggle.setFocusPainted(false);
        modeToggle.setBorderPainted(false);
        modeToggle.setOpaque(true);
        modeToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        modeToggle.addItemListener(e -> updateModeState());
        modePanel.add(modeToggle);
        formContainer.add(modePanel);
        formContainer.add(Box.createVerticalStrut(8));

        // 3. Grid for Inputs
        JPanel inputGrid = new JPanel(new GridBagLayout());
        inputGrid.setOpaque(false);
        inputGrid.setBorder(UITheme.createTitledBorder("CREDENTIAL INPUT", UITheme.PRIMARY_BLUE));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        usernameField = new JTextField(24);
        passwordField = new JPasswordField(24);
        UITheme.styleTextField(usernameField);
        UITheme.styleTextField(passwordField);

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        JLabel userLbl = new JLabel("Username / Payload:");
        userLbl.setFont(UITheme.FONT_BOLD);
        userLbl.setForeground(UITheme.TEXT_SECONDARY);
        inputGrid.add(userLbl, g);

        g.gridx = 1; g.gridy = 0; g.weightx = 1;
        inputGrid.add(usernameField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(UITheme.FONT_BOLD);
        passLbl.setForeground(UITheme.TEXT_SECONDARY);
        inputGrid.add(passLbl, g);

        g.gridx = 1; g.gridy = 1; g.weightx = 1;
        inputGrid.add(passwordField, g);

        // Buttons
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionRow.setOpaque(false);

        clearBtn = UITheme.createStyledButton("Clear", new Color(55, 60, 80));
        clearBtn.addActionListener(e -> clearForm());

        loginBtn = UITheme.createStyledButton("Execute Authentication", new Color(38, 90, 185));
        loginBtn.addActionListener(e -> executeLoginAsync());

        actionRow.add(clearBtn);
        actionRow.add(loginBtn);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        inputGrid.add(actionRow, g);

        // Quick payload chips
        JPanel chipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        chipsPanel.setOpaque(false);
        JLabel chipTitle = new JLabel("Quick Fill:");
        chipTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        chipTitle.setForeground(UITheme.TEXT_MUTED);
        chipsPanel.add(chipTitle);

        addQuickChip(chipsPanel, "Valid (Alice)", "alice", "Alice#Pass99");
        addQuickChip(chipsPanel, "Valid (Admin)", "admin", "Admin@123");
        addQuickChip(chipsPanel, "Bypass (' OR '1'='1' -- )", "' OR '1'='1' -- ", "anything");
        addQuickChip(chipsPanel, "Admin Bypass (lab_admin' -- )", "lab_admin' -- ", "dummy");
        addQuickChip(chipsPanel, "Syntax Error (admin')", "admin'", "test");

        g.gridy = 3;
        inputGrid.add(chipsPanel, g);

        formContainer.add(inputGrid);
        add(formContainer, BorderLayout.NORTH);

        // 4. Output / Result display
        JPanel resultContainer = new JPanel(new BorderLayout());
        resultContainer.setOpaque(false);
        resultContainer.setBorder(UITheme.createTitledBorder("AUTHENTICATION RESULT & LEAKED ROWS", UITheme.WARNING_ORANGE));

        resultArea = new JTextArea(8, 40);
        resultArea.setEditable(false);
        resultArea.setFont(UITheme.FONT_CODE);
        resultArea.setBackground(UITheme.BG_CODE);
        resultArea.setForeground(new Color(230, 235, 120));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setMargin(new Insets(8, 10, 8, 10));

        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_SUBTLE));
        resultContainer.add(resultScroll, BorderLayout.CENTER);

        add(resultContainer, BorderLayout.CENTER);

        updateModeState();
    }

    private void addQuickChip(JPanel panel, String label, String user, String pass) {
        JButton chip = new JButton(label);
        chip.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        chip.setBackground(new Color(36, 40, 60));
        chip.setForeground(UITheme.TEXT_PRIMARY);
        chip.setFocusPainted(false);
        chip.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addActionListener(e -> {
            usernameField.setText(user);
            passwordField.setText(pass);
        });
        panel.add(chip);
    }

    private void updateModeState() {
        boolean isSecure = modeToggle.isSelected();
        if (isSecure) {
            modeToggle.setText("🔒  SECURE MODE (PreparedStatement + BCrypt)");
            modeToggle.setBackground(new Color(25, 130, 60));
            warningBanner.setVisible(false);
        } else {
            modeToggle.setText("🔓  VULNERABLE MODE (Statement Concatenation)");
            modeToggle.setBackground(new Color(180, 40, 40));
            warningBanner.setVisible(true);
        }
        revalidate();
        repaint();
    }

    private void executeLoginAsync() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword()).trim();
        boolean secure = modeToggle.isSelected();
        SecurityMode mode = secure ? SecurityMode.SECURE : SecurityMode.VULNERABLE;

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Executing Query...");
        resultArea.setText("Executing query in background worker thread...\n");

        SwingWorker<LoginResult, Void> worker = new SwingWorker<>() {
            @Override
            protected LoginResult doInBackground() {
                return authService.login(username, password, mode);
            }

            @Override
            protected void done() {
                loginBtn.setEnabled(true);
                loginBtn.setText("Execute Authentication");
                try {
                    LoginResult result = get();
                    displayResult(result, username);
                    if (onResultGenerated != null) {
                        onResultGenerated.accept(result, username);
                    }
                } catch (Exception ex) {
                    resultArea.setText("EXECUTION ERROR: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void displayResult(LoginResult r, String inputUsername) {
        StringBuilder sb = new StringBuilder();
        sb.append("======================================================================\n");
        sb.append(String.format("STATUS         : %s%n", r.getMessage()));
        sb.append(String.format("MODE           : %s%n", r.getMode().getDisplayName()));
        sb.append(String.format("EXECUTION TIME : %d ms%n", r.getExecutionTimeMs()));
        sb.append("======================================================================\n\n");

        if (r.hasUsers()) {
            sb.append(String.format("RETURNED RECORDS (%d row(s)):%n", r.getUsers().size()));
            sb.append("----------------------------------------------------------------------\n");
            sb.append(String.format(" %-5s | %-16s | %-12s | %s%n", "ID", "USERNAME", "ROLE", "CREDENTIAL STATUS"));
            sb.append("----------------------------------------------------------------------\n");
            for (User u : r.getUsers()) {
                sb.append(String.format(" %-5d | %-16s | %-12s | %s%n",
                    u.getId(), u.getUsername(), u.getRole(), "[REDACTED - NEVER EXPOSED]"));
            }
            sb.append("----------------------------------------------------------------------\n");
            if (r.getUsers().size() > 1) {
                sb.append("⚠ ALERT: Multiple rows leaked due to SQL Injection condition evaluating to TRUE!\n");
            }
        } else {
            if (r.isSyntaxError()) {
                sb.append("✘ QUERY FAILED DUE TO SQL SYNTAX ERROR.\n");
                sb.append("Injected characters violated the SQL parser's grammar rules.\n");
            } else if (!r.isSuccess()) {
                sb.append("✘ NO MATCHING USER ROWS RETURNED.\n");
                sb.append("Authentication safely failed closed.\n");
            }
        }

        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
    }

    public void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        resultArea.setText("");
    }

    public void setAuthenticationService(AuthenticationService authService) {
        this.authService = authService;
    }

    public void setInputs(String username, String password, boolean secureMode) {
        usernameField.setText(username);
        passwordField.setText(password);
        modeToggle.setSelected(secureMode);
        updateModeState();
    }
}
