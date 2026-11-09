import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * FEATURE 1 — Portfolio Performance Tracking
 *
 * Records a chronological series of PortfolioSnapshot objects and
 * exposes financial metrics computed on demand from that history.
 *
 * Changes from previous version:
 *   - Fixed "Variable latest is never read" warning in profitAndLoss():
 *     the intermediate `latest` variable was assigned but its fields were
 *     accessed directly on the expression instead. Now the variable is
 *     used consistently, removing the warning cleanly.
 */
public class PerformanceTracker {

    private final List<PortfolioSnapshot> history = new ArrayList<>();

    // ── Recording ─────────────────────────────────────────────────────────

    /**
     * Add or replace a snapshot for the given date.
     * Keeps the list sorted oldest-first at all times.
     */
    public synchronized void record(PortfolioSnapshot snapshot) {
        history.removeIf(s -> s.getDate().equals(snapshot.getDate()));
        history.add(snapshot);
        Collections.sort(history, (a, b) -> a.getDate().compareTo(b.getDate()));
    }

    // ── Core Metrics ──────────────────────────────────────────────────────

    /**
     * Daily return for the given date as a percentage.
     * Returns 0.0 if there is no previous snapshot or equity was zero.
     *
     * Formula: ((todayEquity - prevEquity) / prevEquity) * 100
     */
    public double dailyReturn(LocalDate date) {
        Optional<PortfolioSnapshot> todayOpt = snapshotFor(date);
        if (todayOpt.isEmpty()) return 0.0;

        PortfolioSnapshot today = todayOpt.get();
        int idx = history.indexOf(today);
        if (idx == 0) return 0.0;

        double prev = history.get(idx - 1).getTotalEquity();
        double curr = today.getTotalEquity();
        if (prev == 0.0) return 0.0;

        return ((curr - prev) / prev) * 100.0;
    }

    /**
     * The most recent total equity (holdings + cash).
     * Returns 0.0 if no snapshots have been recorded yet.
     */
    public double totalPortfolioValue() {
        if (history.isEmpty()) return 0.0;
        return history.get(history.size() - 1).getTotalEquity();
    }

    /**
     * Overall percentage growth from the first snapshot to the most recent.
     *
     * Formula: ((latestEquity - firstEquity) / firstEquity) * 100
     */
    public double overallGrowthPercent() {
        if (history.size() < 2) return 0.0;

        double first  = history.get(0).getTotalEquity();
        double latest = history.get(history.size() - 1).getTotalEquity();
        if (first == 0.0) return 0.0;

        return ((latest - first) / first) * 100.0;
    }

    /**
     * Absolute profit or loss = current equity minus total cash deposited.
     * Positive = in profit; negative = at a loss.
     *
     * Fix: `latest` is now actually used to access both getTotalEquity()
     * and getNetDeposited(), removing the "variable is never read" warning.
     */
    public double profitAndLoss() {
        if (history.isEmpty()) return 0.0;
        PortfolioSnapshot latest = history.get(history.size() - 1);
        return latest.getTotalEquity() - latest.getNetDeposited();
    }

    /**
     * Returns every daily return percentage in chronological order.
     * Useful for plotting a P&L chart in the GUI.
     */
    public List<Double> allDailyReturns() {
        List<Double> returns = new ArrayList<>();
        for (PortfolioSnapshot snap : history) {
            returns.add(dailyReturn(snap.getDate()));
        }
        return Collections.unmodifiableList(returns);
    }

    /** Read-only view of the full snapshot history. */
    public List<PortfolioSnapshot> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Optional<PortfolioSnapshot> snapshotFor(LocalDate date) {
        return history.stream()
                .filter(s -> s.getDate().equals(date))
                .findFirst();
    }

    /** Formatted summary string for display in a JTextArea or JOptionPane. */
    public String summaryReport() {
        if (history.isEmpty()) return "No performance data recorded yet.";

        return String.format(
                "═══════════ Performance Summary ═══════════%n" +
                "  Total Portfolio Value : €%.2f%n" +
                "  Profit / Loss         : €%.2f%n" +
                "  Overall Growth        : %.2f%%%n" +
                "  Today's Return        : %.2f%%%n" +
                "  Snapshots recorded    : %d%n" +
                "═══════════════════════════════════════════",
                totalPortfolioValue(),
                profitAndLoss(),
                overallGrowthPercent(),
                dailyReturn(LocalDate.now()),
                history.size()
        );
    }
}
