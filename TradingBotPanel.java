import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * FEATURE 3 — Trading Bot & Backtesting (GUI Panel)
 *
 * Provides a UI for:
 *   1. Running a quick backtest on synthetic data (no historical CSV required)
 *      with configurable short/long SMA windows and starting capital.
 *   2. Stepping a live paper-trading bot through manual price updates.
 *
 * The backtest is run on a Swing Worker thread so the GUI stays responsive
 * during computation.
 */
public class TradingBotPanel extends JPanel {

    private final JTextField symbolField;
    private final JTextField shortPeriodField;
    private final JTextField longPeriodField;
    private final JTextField capitalField;
    private final JTextField numDaysField;
    private final JTextArea  resultArea;
    private final JLabel     signalLabel;      // live bot signal

    // Live-bot state
    private MovingAverageCrossoverBot liveBot = null;

    public TradingBotPanel() {
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        setBackground(AppTheme.BACKGROUND);

        // ── Top: parameter inputs ─────────────────────────────────────────
        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        inputs.setOpaque(false);

        symbolField      = field("AAPL", 5);
        shortPeriodField = field("10",   3);
        longPeriodField  = field("50",   3);
        capitalField     = field("10000",6);
        numDaysField     = field("200",  4);

        inputs.add(label("Symbol:"));      inputs.add(symbolField);
        inputs.add(label("Fast SMA:"));    inputs.add(shortPeriodField);
        inputs.add(label("Slow SMA:"));    inputs.add(longPeriodField);
        inputs.add(label("Capital €:"));   inputs.add(capitalField);
        inputs.add(label("Days:"));        inputs.add(numDaysField);

        // ── Middle: results ───────────────────────────────────────────────
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        AppTheme.styleTextArea(resultArea);
        resultArea.setText("Configure parameters above, then click 'Run Backtest'.");
        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));

        // ── Bottom: buttons + live-bot signal ─────────────────────────────
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bottom.setOpaque(false);

        JButton backtestBtn = new JButton("Run backtest");
        AppTheme.styleButton(backtestBtn, AppTheme.ACCENT);
        backtestBtn.addActionListener(e -> runBacktest());

        JButton liveBotBtn = new JButton("Initialize paper bot");
        AppTheme.styleButton(liveBotBtn, AppTheme.SUCCESS);
        liveBotBtn.addActionListener(e -> initLiveBot());

        JButton feedPriceBtn = new JButton("Feed price");
        AppTheme.styleButton(feedPriceBtn, AppTheme.SURFACE);
        feedPriceBtn.addActionListener(e -> feedPriceToLiveBot());

        signalLabel = new JLabel("Bot signal: N/A");
        signalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        signalLabel.setForeground(AppTheme.NAVY);

        bottom.add(backtestBtn);
        bottom.add(liveBotBtn);
        bottom.add(feedPriceBtn);
        bottom.add(signalLabel);

        // ── Compose ───────────────────────────────────────────────────────
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(AppTheme.SURFACE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        JPanel inputContent = new JPanel(new BorderLayout(0, 10));
        inputContent.setOpaque(false);
        JLabel heading = new JLabel("Strategy lab");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(AppTheme.NAVY);
        JLabel subtitle = new JLabel("Test a moving-average strategy or step through a paper-trading signal.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.MUTED);
        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(heading, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);
        inputContent.add(titleBlock, BorderLayout.NORTH);
        inputContent.add(inputs, BorderLayout.CENTER);
        top.add(inputContent, BorderLayout.CENTER);

        add(top,    BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ── Action Handlers ───────────────────────────────────────────────────

    private void runBacktest() {
        int   shortP, longP, days;
        double capital;

        try {
            shortP  = Integer.parseInt(shortPeriodField.getText().trim());
            longP   = Integer.parseInt(longPeriodField.getText().trim());
            days    = Integer.parseInt(numDaysField.getText().trim());
            capital = Double.parseDouble(capitalField.getText().trim());
        } catch (NumberFormatException ex) {
            error("All numeric fields must be valid numbers.");
            return;
        }

        if (shortP >= longP) {
            error("Fast SMA period must be less than Slow SMA period.");
            return;
        }
        if (days < longP + 1) {
            error("Number of days must exceed the Slow SMA period + 1.");
            return;
        }

        String symbol = symbolField.getText().trim().toUpperCase();
        resultArea.setText("Running backtest on " + days + " simulated bars…");

        // Run on a background thread so the GUI stays responsive
        new SwingWorker<MovingAverageCrossoverBot.BacktestResult, Void>() {
            @Override
            protected MovingAverageCrossoverBot.BacktestResult doInBackground() {
                MovingAverageCrossoverBot bot =
                        new MovingAverageCrossoverBot(symbol, shortP, longP);
                List<PriceBar> data = MovingAverageCrossoverBot.generateSampleData(days);
                return bot.backtest(data, capital);
            }

            @Override
            protected void done() {
                try {
                    MovingAverageCrossoverBot.BacktestResult r = get();
                    resultArea.setText(r.summaryReport());
                    resultArea.setCaretPosition(0);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    resultArea.setText("Backtest interrupted.");
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    resultArea.setText("Backtest error: "
                            + (cause == null ? ex.getMessage() : cause.getMessage()));
                }
            }
        }.execute();
    }

    private void initLiveBot() {
        int shortP, longP;
        try {
            shortP = Integer.parseInt(shortPeriodField.getText().trim());
            longP  = Integer.parseInt(longPeriodField.getText().trim());
        } catch (NumberFormatException ex) {
            error("Fast/Slow SMA values must be integers.");
            return;
        }
        if (shortP >= longP) { error("Fast SMA must be < Slow SMA."); return; }

        String symbol = symbolField.getText().trim().toUpperCase();
        liveBot = new MovingAverageCrossoverBot(symbol, shortP, longP);
        signalLabel.setText("Bot initialized for " + symbol + " | Signal: HOLD");
        signalLabel.setForeground(Color.DARK_GRAY);
        resultArea.setText("""
                Live bot initialized.
                Click 'Feed Price' to simulate a new price bar.
                The bot will emit BUY / SELL / HOLD signals.""");
    }

    private void feedPriceToLiveBot() {
        if (liveBot == null) {
            error("Initialize the live bot first.");
            return;
        }
        String input = JOptionPane.showInputDialog(this,
                "Enter the current close price for " + liveBot.getSymbol() + ":",
                "Feed Price", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;

        double price;
        try {
            price = Double.parseDouble(input.trim());
        } catch (NumberFormatException ex) {
            error("Price must be a valid decimal number.");
            return;
        }

        PriceBar bar    = new PriceBar(LocalDate.now(), price);
        String   signal = liveBot.onNewBar(bar);

        Color color = switch (signal) {
            case "BUY"  -> new Color(0, 128, 0);
            case "SELL" -> Color.RED;
            default     -> Color.DARK_GRAY;
        };

        signalLabel.setText("Bot signal: " + signal + " @ €" + String.format("%.2f", price));
        signalLabel.setForeground(color);
        resultArea.append(String.format("%n[%s] price=%.2f → %s", LocalDate.now(), price, signal));
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private JTextField field(String defaultVal, int cols) {
        JTextField f = new JTextField(defaultVal, cols);
        AppTheme.styleField(f);
        f.setPreferredSize(new Dimension(Math.max(66, cols * 14), 38));
        return f;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(AppTheme.TEXT);
        return l;
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Error", JOptionPane.ERROR_MESSAGE);
    }
}
