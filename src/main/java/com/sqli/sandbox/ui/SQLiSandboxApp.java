package com.sqli.sandbox.ui;

import com.sqli.sandbox.config.DatabaseConfig;
import com.sqli.sandbox.database.DatabaseManager;
import com.sqli.sandbox.service.AuthenticationService;
import com.sqli.sandbox.service.TestCaseRunnerService;

import javax.swing.*;
import java.awt.*;

/**
 * Main application entry point for the SQL Injection Sandbox & Visualizer.
 * Assembles the interactive login workbench, query visualizer, automated test suite,
 * and attack cheat sheet into a cohesive educational platform.
 */
public class SQLiSandboxApp extends JFrame {

    private DatabaseManager dbManager;
    private AuthenticationService authService;
    private TestCaseRunnerService runnerService;

    private ConnectionStatusPanel statusPanel;
    private LoginFormPanel loginPanel;
    private TestRunnerPanel testRunnerPanel;
    private QueryVisualizerPanel visualizerPanel;
    private CheatSheetPanel cheatSheetPanel;
    private JTabbedPane tabbedPane;

    public SQLiSandboxApp() {
        super("SQL Injection Sandbox & Visualizer — Educational Security Lab");
        initServices();
        initUI();
    }

    private void initServices() {
        DatabaseConfig config = DatabaseConfig.load();
        this.dbManager = new DatabaseManager(config);
        this.authService = new AuthenticationService(dbManager);
        this.runnerService = new TestCaseRunnerService(authService);
    }

    private void initUI() {
        setSize(940, 780);
        setMinimumSize(new Dimension(840, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // 1. Top connection status panel
        statusPanel = new ConnectionStatusPanel(dbManager, this::onDatabaseManagerChanged);
        add(statusPanel, BorderLayout.NORTH);

        // 2. Query Visualizer Panel (Lower Split)
        visualizerPanel = new QueryVisualizerPanel();

        // 3. Tabbed Workspace (Upper Split)
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(UITheme.BG_CARD);
        tabbedPane.setForeground(UITheme.TEXT_PRIMARY);
        tabbedPane.setFont(UITheme.FONT_HEADER);

        // Login Panel
        loginPanel = new LoginFormPanel(authService, (result, username) -> {
            visualizerPanel.displayResult(result, username);
        });

        // Test Runner Panel
        testRunnerPanel = new TestRunnerPanel(runnerService);

        // Cheat Sheet Panel
        cheatSheetPanel = new CheatSheetPanel((payload, pass) -> {
            loginPanel.setInputs(payload, pass, false); // Load into vulnerable mode for testing
            tabbedPane.setSelectedIndex(0);
        });

        tabbedPane.addTab("  Interactive Attack Workbench  ", loginPanel);
        tabbedPane.addTab("  Automated Security Test Suite  ", testRunnerPanel);
        tabbedPane.addTab("  Educational Cheat Sheet  ", cheatSheetPanel);

        // Vertical split between tabbed workspace and query visualizer
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, visualizerPanel);
        mainSplit.setDividerLocation(420);
        mainSplit.setDividerSize(5);
        mainSplit.setBorder(null);
        mainSplit.setOpaque(false);
        add(mainSplit, BorderLayout.CENTER);

        // 4. Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(10, 12, 20));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_SUBTLE));
        JLabel footerLabel = new JLabel("College CSE Capstone  |  OWASP Top 10 A03:2021 (Injection)  |  PreparedStatement + BCrypt");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(UITheme.TEXT_MUTED);
        footer.add(footerLabel);
        add(footer, BorderLayout.SOUTH);
    }

    private void onDatabaseManagerChanged(DatabaseManager newManager) {
        this.dbManager = newManager;
        this.authService = new AuthenticationService(newManager);
        this.runnerService = new TestCaseRunnerService(authService);

        loginPanel.setAuthenticationService(authService);
        testRunnerPanel.setRunnerService(runnerService);
        visualizerPanel.clear();
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("======================================================================");
            System.out.println("⚡ SQL Injection Sandbox & Visualizer (Headless CLI Mode)");
            System.out.println("======================================================================");
            System.out.println("No graphical display detected. Executing deterministic test battery...");

            DatabaseConfig config = DatabaseConfig.createInMemoryH2Config("headless_cli_demo");
            DatabaseManager dbMgr = new DatabaseManager(config);
            try {
                new com.sqli.sandbox.database.SchemaInitializer(dbMgr).initializeSchema();
            } catch (Exception e) {
                System.err.println("Schema initialization failed: " + e.getMessage());
            }

            AuthenticationService auth = new AuthenticationService(dbMgr);
            TestCaseRunnerService runner = new TestCaseRunnerService(auth);
            java.util.List<com.sqli.sandbox.model.TestCaseResult> results = runner.runAllTests();

            int passed = 0;
            for (com.sqli.sandbox.model.TestCaseResult r : results) {
                System.out.printf("Test #%2d [%-42s] [%s] -> %s%n",
                    r.getTestNumber(), r.getTestName(), r.getMode().name(), r.isPassed() ? "PASS" : "FAIL");
                if (r.isPassed()) passed++;
            }
            System.out.printf("%nExecution Complete: %d/%d tests passed (%.1f%%).%n",
                passed, results.size(), (double) passed / results.size() * 100.0);
            return;
        }

        // Apply System Look and Feel for native rendering
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            SQLiSandboxApp app = new SQLiSandboxApp();
            app.setVisible(true);
        });
    }
}
