import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FEATURE 3 — Simple Trading Bot & Backtesting
 *
 * Implements a Moving Average Crossover strategy:
 *   • When the SHORT-period SMA crosses ABOVE the LONG-period SMA → BUY signal.
 *   • When the SHORT-period SMA crosses BELOW the LONG-period SMA → SELL signal.
 *
 * Changes from previous version:
 *   - Removed the `entryPrice` field from live-bot state; it was written but
 *     never read there (the backtest engine uses its own local avgBuyPrice).
 *     Entry price is now a local variable inside onNewBar() where it is needed.
 */
public class MovingAverageCrossoverBot {

    // ── Inner classes ─────────────────────────────────────────────────────

    /** A single trade recorded during backtesting. */
    public static class Trade {
        public final String    action;
        public final LocalDate date;
        public final double    price;
        public final int       shares;
        public final double    pnl;

        Trade(String action, LocalDate date, double price, int shares, double pnl) {
            this.action = action;
            this.date   = date;
            this.price  = price;
            this.shares = shares;
            this.pnl    = pnl;
        }

        @Override
        public String toString() {
            if ("BUY".equals(action)) {
                return String.format("[%s] BUY  %d shares @ €%.2f", date, shares, price);
            }
            return String.format("[%s] SELL %d shares @ €%.2f  P&L=€%.2f", date, shares, price, pnl);
        }
    }

    /** Aggregated results from a backtest run. */
    public static class BacktestResult {
        public final List<Trade> trades;
        public final double      startingCash;
        public final double      finalCash;
        public final int         totalTrades;
        public final int         winningTrades;
        public final double      totalPnL;
        public final double      winRate;

        BacktestResult(List<Trade> trades, double startingCash, double finalCash,
                       int totalTrades, int winningTrades, double totalPnL) {
            this.trades        = Collections.unmodifiableList(trades);
            this.startingCash  = startingCash;
            this.finalCash     = finalCash;
            this.totalTrades   = totalTrades;
            this.winningTrades = winningTrades;
            this.totalPnL      = totalPnL;
            int sellCount = (int) trades.stream().filter(t -> "SELL".equals(t.action)).count();
            this.winRate  = sellCount == 0 ? 0.0 : (winningTrades * 100.0 / sellCount);
        }

        public String summaryReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════ Backtest Report ═══════════\n");
            sb.append(String.format("  Starting capital  : €%.2f%n", startingCash));
            sb.append(String.format("  Final equity      : €%.2f%n", finalCash));
            sb.append(String.format("  Total P&L         : €%.2f%n", totalPnL));
            sb.append(String.format("  Return on capital : %.2f%%%n",
                    startingCash == 0 ? 0 : (totalPnL / startingCash) * 100));
            sb.append(String.format("  Trades executed   : %d%n", totalTrades));
            sb.append(String.format("  Winning trades    : %d  (%.1f%% win rate)%n",
                    winningTrades, winRate));
            sb.append("───────────────────────────────────────\n");
            sb.append("Trade log:\n");
            for (Trade t : trades) {
                sb.append("  ").append(t).append("\n");
            }
            sb.append("═══════════════════════════════════════");
            return sb.toString();
        }
    }

    // ── Bot State ─────────────────────────────────────────────────────────

    private final String       symbol;
    private final int          shortPeriod;
    private final int          longPeriod;
    private final List<Double> livePrices = new ArrayList<>();
    private       boolean      inPosition = false;

    // ── Constructor ───────────────────────────────────────────────────────

    public MovingAverageCrossoverBot(String symbol, int shortPeriod, int longPeriod) {
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("shortPeriod must be less than longPeriod");
        }
        this.symbol      = symbol.toUpperCase();
        this.shortPeriod = shortPeriod;
        this.longPeriod  = longPeriod;
    }

    // ── Live Paper Trading ────────────────────────────────────────────────

    /**
     * Feed the bot a new price bar during a live session.
     * Returns "BUY", "SELL", or "HOLD".
     */
    public String onNewBar(PriceBar bar) {
        livePrices.add(bar.getClose());

        if (livePrices.size() > longPeriod + 1) {
            livePrices.remove(0);
        }

        if (livePrices.size() < longPeriod + 1) return "HOLD";

        double shortSMA     = computeSMA(livePrices, livePrices.size() - shortPeriod, livePrices.size());
        double longSMA      = computeSMA(livePrices, livePrices.size() - longPeriod,  livePrices.size());
        double prevShortSMA = computeSMA(livePrices, livePrices.size() - shortPeriod - 1, livePrices.size() - 1);
        double prevLongSMA  = computeSMA(livePrices, livePrices.size() - longPeriod  - 1, livePrices.size() - 1);

        boolean bullishCross = prevShortSMA <= prevLongSMA && shortSMA > longSMA;
        boolean bearishCross = prevShortSMA >= prevLongSMA && shortSMA < longSMA;

        if (bullishCross && !inPosition) {
            inPosition = true;
            return "BUY";
        }
        if (bearishCross && inPosition) {
            inPosition = false;
            return "SELL";
        }
        return "HOLD";
    }

    // ── Backtesting Engine ────────────────────────────────────────────────

    public BacktestResult backtest(List<PriceBar> bars, double startingCash) {
        if (bars == null || bars.size() < longPeriod) {
            throw new IllegalArgumentException(
                    "Need at least " + longPeriod + " bars to backtest.");
        }

        List<Trade> tradeLog   = new ArrayList<>();
        double cash            = startingCash;
        int    sharesHeld      = 0;
        double avgBuyPrice     = 0.0;
        int    winningTrades   = 0;
        double totalPnL        = 0.0;

        for (int i = longPeriod; i < bars.size(); i++) {
            double shortSMA     = computeSMAFromBars(bars, i - shortPeriod + 1, i + 1);
            double longSMA      = computeSMAFromBars(bars, i - longPeriod  + 1, i + 1);
            double prevShortSMA = computeSMAFromBars(bars, i - shortPeriod, i);
            double prevLongSMA  = computeSMAFromBars(bars, i - longPeriod,  i);

            PriceBar  bar        = bars.get(i);
            double    closePrice = bar.getClose();
            LocalDate date       = bar.getDate();

            boolean bullishCross = prevShortSMA <= prevLongSMA && shortSMA > longSMA;
            boolean bearishCross = prevShortSMA >= prevLongSMA && shortSMA < longSMA;

            if (bullishCross && sharesHeld == 0 && cash > 0) {
                int shares = (int) (cash / closePrice);
                if (shares > 0) {
                    avgBuyPrice = closePrice;
                    cash       -= shares * closePrice;
                    sharesHeld  = shares;
                    tradeLog.add(new Trade("BUY", date, closePrice, shares, 0));
                }
            } else if (bearishCross && sharesHeld > 0) {
                double proceeds = sharesHeld * closePrice;
                double pnl      = (closePrice - avgBuyPrice) * sharesHeld;
                cash           += proceeds;
                totalPnL       += pnl;
                if (pnl > 0) winningTrades++;
                tradeLog.add(new Trade("SELL", date, closePrice, sharesHeld, pnl));
                sharesHeld = 0;
            }
        }

        // Mark to market if still holding at end of backtest window
        if (sharesHeld > 0) {
            double lastClose = bars.get(bars.size() - 1).getClose();
            double pnl       = (lastClose - avgBuyPrice) * sharesHeld;
            cash            += sharesHeld * lastClose;
            totalPnL        += pnl;
            if (pnl > 0) winningTrades++;
            tradeLog.add(new Trade("SELL (EOD)", bars.get(bars.size() - 1).getDate(),
                    lastClose, sharesHeld, pnl));
        }

        return new BacktestResult(tradeLog, startingCash, cash,
                tradeLog.size(), winningTrades, totalPnL);
    }

    // ── Indicator Helpers ─────────────────────────────────────────────────

    private static double computeSMA(List<Double> prices, int from, int to) {
        double sum = 0;
        for (int i = from; i < to; i++) sum += prices.get(i);
        return sum / (to - from);
    }

    private static double computeSMAFromBars(List<PriceBar> bars, int from, int to) {
        double sum = 0;
        for (int i = from; i < to; i++) sum += bars.get(i).getClose();
        return sum / (to - from);
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public String getSymbol()      { return symbol; }
    public int    getShortPeriod() { return shortPeriod; }
    public int    getLongPeriod()  { return longPeriod; }

    /** Generate synthetic price data for quick testing. */
    public static List<PriceBar> generateSampleData(int numDays) {
        List<PriceBar> bars = new ArrayList<>();
        double price = 100.0;
        LocalDate date = LocalDate.now().minusDays(numDays);
        java.util.Random rng = new java.util.Random(42);

        for (int i = 0; i < numDays; i++) {
            price *= (1.0 + (rng.nextDouble() - 0.5) * 0.04);
            price  = Math.max(price, 1.0);
            bars.add(new PriceBar(date.plusDays(i), price));
        }
        return bars;
    }
}
