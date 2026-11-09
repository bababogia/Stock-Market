import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * FEATURE 1 — Portfolio Performance Tracking (GUI Panel)
 *
 * A self-contained JPanel that:
 *   • Displays real-time portfolio metrics via a JTextArea.
 *   • Provides a "Take Snapshot" button that records today's equity into the
 *     PerformanceTracker and immediately refreshes the display.
 *   • Provides a "Full Report" button that opens a scrollable window showing
 *     the complete P&L history.
 *
 * To add this panel to StockMarketGUI, see the integration guide at the
 * bottom of this file.
 */
public class PerformancePanel extends JPanel {

    private final PerformanceTracker tracker;
    private final UserAccount        userAccount;

    // We need to know current market value of holdings.
    // This reference is set via setCurrentHoldingsValue() each time the GUI
    // refreshes prices, so the snapshot captures an accurate number.
    private double currentHoldingsValue = 0.0;
    private double netDeposited         = 0.0;

    private final JTextArea metricsArea;

    public PerformancePanel(PerformanceTracker tracker, UserAccount userAccount) {
        this.tracker     = tracker;
        this.userAccount = userAccount;

        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        setBackground(AppTheme.BACKGROUND);

        // ── Metrics display ───────────────────────────────────────────────
        JPanel reportCard = new JPanel(new BorderLayout(0, 12));
        reportCard.setBackground(AppTheme.SURFACE);
        reportCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        JLabel heading = new JLabel("Portfolio performance");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(AppTheme.NAVY);
        metricsArea = new JTextArea(6, 30);
        metricsArea.setEditable(false);
        AppTheme.styleTextArea(metricsArea);
        metricsArea.setText("No data yet. Click 'Take Snapshot' after trading.");
        JScrollPane scroll = new JScrollPane(metricsArea);
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        reportCard.add(heading, BorderLayout.NORTH);
        reportCard.add(scroll, BorderLayout.CENTER);
        add(reportCard, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        JButton snapshotBtn = new JButton("Take snapshot");
        AppTheme.styleButton(snapshotBtn, AppTheme.ACCENT);
        snapshotBtn.addActionListener(e -> takeSnapshot());

        JButton reportBtn = new JButton("Full report");
        AppTheme.styleButton(reportBtn, AppTheme.SURFACE);
        reportBtn.addActionListener(e -> showFullReport());

        btnPanel.add(snapshotBtn);
        btnPanel.add(reportBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Call this whenever the market value of open positions changes
     * (e.g., when the GUI fetches a fresh price).
     */
    public void setCurrentHoldingsValue(double value) {
        this.currentHoldingsValue = value;
    }

    /**
     * Call this every time the user deposits money, so the tracker can
     * compute accurate P&L relative to invested capital.
     */
    public void setNetDeposited(double total) {
        this.netDeposited = total;
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private void takeSnapshot() {
        PortfolioSnapshot snap = new PortfolioSnapshot(
                LocalDate.now(),
                currentHoldingsValue,
                userAccount.getBalance(),
                netDeposited
        );
        tracker.record(snap);
        refreshMetrics();
        JOptionPane.showMessageDialog(this,
                "Snapshot recorded for " + LocalDate.now(),
                "Performance Tracker", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshMetrics() {
        if (tracker.getHistory().isEmpty()) {
            metricsArea.setText("No data yet.");
            return;
        }
        metricsArea.setText(String.format(
                "Total Portfolio Value : €%.2f%n" +
                "Profit / Loss         : €%.2f%n" +
                "Overall Growth        : %.2f%%%n" +
                "Today's Daily Return  : %.2f%%%n" +
                "Snapshots recorded    : %d",
                tracker.totalPortfolioValue(),
                tracker.profitAndLoss(),
                tracker.overallGrowthPercent(),
                tracker.dailyReturn(LocalDate.now()),
                tracker.getHistory().size()
        ));
    }

    private void showFullReport() {
        JTextArea area = new JTextArea(tracker.summaryReport());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this,
                new JScrollPane(area),
                "Full Performance Report",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
