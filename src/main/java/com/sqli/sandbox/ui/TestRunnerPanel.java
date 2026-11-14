package com.sqli.sandbox.ui;

import com.sqli.sandbox.model.TestCaseResult;
import com.sqli.sandbox.service.TestCaseRunnerService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated deterministic test runner panel.
 * Runs the 10 core security scenarios asynchronously, renders results in a table,
 * provides metric summaries, and supports safe CSV export.
 */
public class TestRunnerPanel extends JPanel {

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JButton runAllBtn;
    private final JButton clearBtn;
    private final JButton exportBtn;
    private final JLabel summaryLabel;
    private final JProgressBar progressBar;

    private TestCaseRunnerService runnerService;
    private List<TestCaseResult> currentResults = new ArrayList<>();

    public TestRunnerPanel(TestCaseRunnerService runnerService) {
        this.runnerService = runnerService;

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_CARD);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // 1. Controls Toolbar at Top
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBtns.setOpaque(false);

        runAllBtn = UITheme.createStyledButton("▶ Run All 10 Tests", new Color(38, 120, 70));
        runAllBtn.addActionListener(e -> runTestsAsync());

        clearBtn = UITheme.createStyledButton("Reset Results", new Color(60, 65, 85));
        clearBtn.addActionListener(e -> clearResults());

        exportBtn = UITheme.createStyledButton("Export CSV", new Color(38, 90, 185));
        exportBtn.setEnabled(false);
        exportBtn.addActionListener(e -> exportCsv());

        leftBtns.add(runAllBtn);
        leftBtns.add(clearBtn);
        leftBtns.add(exportBtn);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(140, 20));

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        rightControls.setOpaque(false);
        rightControls.add(progressBar);

        toolbar.add(leftBtns, BorderLayout.WEST);
        toolbar.add(rightControls, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        // 2. Center Table
        String[] columns = {"#", "Test Scenario", "Mode", "Expected Outcome", "Actual Outcome", "Status", "Details / Explanation"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setBackground(UITheme.BG_CODE);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(UITheme.BORDER_SUBTLE);
        table.setRowHeight(26);
        table.setFont(UITheme.FONT_BODY);
        table.setSelectionBackground(new Color(40, 50, 80));
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(20, 24, 38));
        header.setForeground(UITheme.PRIMARY_BLUE);
        header.setFont(UITheme.FONT_HEADER);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(210);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setPreferredWidth(170);
        table.getColumnModel().getColumn(4).setPreferredWidth(170);
        table.getColumnModel().getColumn(5).setPreferredWidth(70);
        table.getColumnModel().getColumn(6).setPreferredWidth(260);

        // Custom renderer for Status column (PASS in green, FAIL in red)
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(UITheme.FONT_BOLD);
                if ("PASS".equals(value)) {
                    setForeground(UITheme.SUCCESS_GREEN);
                } else if ("FAIL".equals(value)) {
                    setForeground(UITheme.DANGER_RED);
                } else {
                    setForeground(UITheme.TEXT_MUTED);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_SUBTLE));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Summary Footer Bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 4, 2, 4));

        summaryLabel = new JLabel("Test Suite: Ready. Click 'Run All 10 Tests' to verify security assertions.");
        summaryLabel.setFont(UITheme.FONT_CODE_BOLD);
        summaryLabel.setForeground(UITheme.TEXT_SECONDARY);

        footer.add(summaryLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }

    private void runTestsAsync() {
        runAllBtn.setEnabled(false);
        clearBtn.setEnabled(false);
        exportBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        summaryLabel.setText("Executing 10 deterministic security test scenarios in background thread...");
        summaryLabel.setForeground(UITheme.WARNING_ORANGE);

        SwingWorker<List<TestCaseResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TestCaseResult> doInBackground() {
                return runnerService.runAllTests();
            }

            @Override
            protected void done() {
                runAllBtn.setEnabled(true);
                clearBtn.setEnabled(true);
                progressBar.setVisible(false);
                try {
                    currentResults = get();
                    populateTable(currentResults);
                    exportBtn.setEnabled(!currentResults.isEmpty());
                } catch (Exception e) {
                    summaryLabel.setText("Test execution error: " + e.getMessage());
                    summaryLabel.setForeground(UITheme.DANGER_RED);
                }
            }
        };
        worker.execute();
    }

    private void populateTable(List<TestCaseResult> results) {
        tableModel.setRowCount(0);
        int passed = 0;
        int failed = 0;

        for (TestCaseResult r : results) {
            if (r.isPassed()) passed++;
            else failed++;

            tableModel.addRow(new Object[]{
                r.getTestNumber(),
                r.getTestName(),
                r.getMode().name(),
                r.getExpectedOutcome(),
                r.getActualOutcome(),
                r.isPassed() ? "PASS" : "FAIL",
                r.getExplanation()
            });
        }

        double rate = (results.isEmpty()) ? 0.0 : ((double) passed / results.size()) * 100.0;
        String summaryText = String.format(
            "Total Tests: %d  |  Passed: %d  |  Failed: %d  |  Success Rate: %.1f%%",
            results.size(), passed, failed, rate
        );

        summaryLabel.setText(summaryText);
        summaryLabel.setForeground(failed == 0 ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);
    }

    private void clearResults() {
        tableModel.setRowCount(0);
        currentResults.clear();
        exportBtn.setEnabled(false);
        summaryLabel.setText("Results cleared. Click 'Run All 10 Tests' to execute.");
        summaryLabel.setForeground(UITheme.TEXT_SECONDARY);
    }

    private void exportCsv() {
        if (currentResults.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No test results to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("sqli_test_results.csv"));
        int choice = chooser.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            boolean success = runnerService.exportToCsv(currentResults, file);
            if (success) {
                JOptionPane.showMessageDialog(this, "Report exported safely to " + file.getName() + "\n(Credentials and hashes were strictly excluded).", "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to write CSV file.", "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setRunnerService(TestCaseRunnerService runnerService) {
        this.runnerService = runnerService;
    }
}
