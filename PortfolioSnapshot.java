import java.time.LocalDate;

/**
 * Represents the total portfolio value at a single point in time.
 * These snapshots are taken at the end of each trading session and
 * stored chronologically so we can calculate daily returns, cumulative
 * P&L and overall percentage growth across any date range.
 */
public class PortfolioSnapshot {

    private final LocalDate date;
    private final double totalValue;      // market value of all holdings
    private final double cashBalance;     // uninvested cash in the wallet
    private final double netDeposited;    // total cash the user has deposited lifetime

    public PortfolioSnapshot(LocalDate date,
                             double totalValue,
                             double cashBalance,
                             double netDeposited) {
        this.date        = date;
        this.totalValue  = totalValue;
        this.cashBalance = cashBalance;
        this.netDeposited = netDeposited;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public LocalDate getDate()         { return date; }
    public double getTotalValue()      { return totalValue; }
    public double getCashBalance()     { return cashBalance; }
    public double getNetDeposited()    { return netDeposited; }

    /**
     * Equity = market value of holdings + cash balance.
     * This is the number users think of as "how much is my portfolio worth".
     */
    public double getTotalEquity() {
        return totalValue + cashBalance;
    }

    @Override
    public String toString() {
        return String.format("[%s] equity=%.2f  holdings=%.2f  cash=%.2f  deposited=%.2f",
                date, getTotalEquity(), totalValue, cashBalance, netDeposited);
    }
}
