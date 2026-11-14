package com.sqli.sandbox.ui;

import com.sqli.sandbox.config.DatabaseConfig;
import com.sqli.sandbox.database.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Top banner panel displaying live database reachability status,
 * configuration diagnostics, and offline fallback controls.
 */
public class ConnectionStatusPanel extends JPanel {

    private final JLabel statusBadge;
    private final JLabel detailsLabel;
    private final JButton testBtn;
    private final JCheckBox inMemoryToggle;
    private DatabaseManager dbManager;
    private final Consumer<DatabaseManager> onManagerChanged;

    public ConnectionStatusPanel(DatabaseManager initialManager, Consumer<DatabaseManager> onManagerChanged) {
        this.dbManager = initialManager;
        this.onManagerChanged = onManagerChanged;

        setLayout(new BorderLayout(10, 0));
        setBackground(new Color(10, 12, 20));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_SUBTLE),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        // Left: Title and subtitle
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        leftPanel.setOpaque(false);

        JLabel title = new JLabel("⚡ SQL Injection Sandbox & Visualizer");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.PRIMARY_BLUE);

        detailsLabel = new JLabel("Target: " + dbManager.getConfig().getHost() + ":" + dbManager.getConfig().getPort() + "/" + dbManager.getConfig().getDatabase());
        detailsLabel.setFont(UITheme.FONT_CODE);
        detailsLabel.setForeground(UITheme.TEXT_SECONDARY);

        leftPanel.add(title);
        leftPanel.add(detailsLabel);
        add(leftPanel, BorderLayout.WEST);

        // Right: Status indicator, Test button, and In-Memory switch
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        rightPanel.setOpaque(false);

        statusBadge = new JLabel("  CONNECTING...  ");
        statusBadge.setFont(UITheme.FONT_CODE_BOLD);
        statusBadge.setOpaque(true);
        statusBadge.setBackground(UITheme.WARNING_ORANGE);
        statusBadge.setForeground(Color.BLACK);
        statusBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        testBtn = UITheme.createStyledButton("Test Connection", new Color(38, 79, 142));
        testBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        testBtn.addActionListener(e -> checkConnectionAsync());

        inMemoryToggle = new JCheckBox("In-Memory Lab (Offline Mode)");
        inMemoryToggle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        inMemoryToggle.setForeground(UITheme.TEXT_PRIMARY);
        inMemoryToggle.setOpaque(false);
        inMemoryToggle.setFocusPainted(false);
        inMemoryToggle.addItemListener(e -> {
            boolean useH2 = inMemoryToggle.isSelected();
            switchDatabaseMode(useH2);
        });

        rightPanel.add(inMemoryToggle);
        rightPanel.add(statusBadge);
        rightPanel.add(testBtn);

        add(rightPanel, BorderLayout.EAST);

        // Trigger initial non-blocking connection test
        checkConnectionAsync();
    }

    public void checkConnectionAsync() {
        testBtn.setEnabled(false);
        statusBadge.setText("  TESTING...  ");
        statusBadge.setBackground(UITheme.WARNING_ORANGE);
        statusBadge.setForeground(Color.BLACK);

        SwingWorker<DatabaseManager.ConnectionStatus, Void> worker = new SwingWorker<>() {
            @Override
            protected DatabaseManager.ConnectionStatus doInBackground() {
                return dbManager.testConnection();
            }

            @Override
            protected void done() {
                testBtn.setEnabled(true);
                try {
                    DatabaseManager.ConnectionStatus status = get();
                    if (status.isConnected()) {
                        statusBadge.setText("  ● CONNECTED  ");
                        statusBadge.setBackground(UITheme.SUCCESS_GREEN);
                        statusBadge.setForeground(Color.BLACK);
                        detailsLabel.setText("Active: " + dbManager.getConfig().getHost() + ":" + dbManager.getConfig().getPort() + "/" + dbManager.getConfig().getDatabase() + " (" + status.getResponseTimeMs() + "ms)");
                        statusBadge.setToolTipText(status.getDetails());
                    } else {
                        statusBadge.setText("  ● OFFLINE  ");
                        statusBadge.setBackground(UITheme.DANGER_RED);
                        statusBadge.setForeground(Color.WHITE);
                        detailsLabel.setText(status.getMessage());
                        statusBadge.setToolTipText("Error: " + status.getDetails());
                    }
                } catch (Exception ex) {
                    statusBadge.setText("  ● ERROR  ");
                    statusBadge.setBackground(UITheme.DANGER_RED);
                    statusBadge.setForeground(Color.WHITE);
                    detailsLabel.setText("Connection check failed: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void switchDatabaseMode(boolean inMemory) {
        if (inMemory) {
            DatabaseConfig h2Config = DatabaseConfig.createInMemoryH2Config("sqli_lab_demo");
            dbManager = new DatabaseManager(h2Config);
            try {
                new com.sqli.sandbox.database.SchemaInitializer(dbManager).initializeSchema();
            } catch (Exception ignored) {}
            detailsLabel.setText("Active: In-Memory H2 Simulation Lab (Offline Mode)");
        } else {
            dbManager = new DatabaseManager(DatabaseConfig.load());
            detailsLabel.setText("Target: " + dbManager.getConfig().getHost() + ":" + dbManager.getConfig().getPort() + "/" + dbManager.getConfig().getDatabase());
        }
        if (onManagerChanged != null) {
            onManagerChanged.accept(dbManager);
        }
        checkConnectionAsync();
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }
}
