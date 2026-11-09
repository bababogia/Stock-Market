import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * FEATURE 2 — Limit Orders & Stop Losses (GUI Panel)
 *
 * Provides the user interface for placing and viewing conditional orders.
 * The panel communicates with {@link OrderMonitorService}, which runs on
 * a background thread and executes orders when their trigger price is hit.
 *
 * Layout:
 *   • Symbol / Quantity / Trigger Price input fields
 *   • Radio buttons: Limit Buy | Limit Sell | Stop Loss
 *   • "Place Order" button
 *   • "View Pending" button — opens a list of all pending orders
 *   • A status label updated by the monitor service callback
 */
public class LimitOrderPanel extends JPanel {

    private final OrderMonitorService monitor;

    private final JTextField symbolField;
    private final JTextField quantityField;
    private final JTextField triggerPriceField;
    private final JLabel     statusLabel;

    private final JRadioButton limitBuyBtn;
    private final JRadioButton limitSellBtn;
    private final JRadioButton stopLossBtn;

    public LimitOrderPanel(OrderMonitorService monitor) {
        this.monitor = monitor;

        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        setBackground(AppTheme.BACKGROUND);

        // ── Input row ─────────────────────────────────────────────────────
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        inputRow.setOpaque(false);

        symbolField       = styledField(5);
        quantityField     = styledField(5);
        triggerPriceField = styledField(7);

        inputRow.add(label("Symbol:"));      inputRow.add(symbolField);
        inputRow.add(label("Qty:"));         inputRow.add(quantityField);
        inputRow.add(label("Trigger €:"));   inputRow.add(triggerPriceField);

        // ── Order type radio group ─────────────────────────────────────────
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        typeRow.setOpaque(false);

        limitBuyBtn  = new JRadioButton("Limit Buy");
        limitSellBtn = new JRadioButton("Limit Sell");
        stopLossBtn  = new JRadioButton("Stop Loss");
        limitBuyBtn.setSelected(true);
        limitBuyBtn.setBackground(AppTheme.SURFACE);
        limitSellBtn.setBackground(AppTheme.SURFACE);
        stopLossBtn.setBackground(AppTheme.SURFACE);
        for (JRadioButton button : new JRadioButton[] {limitBuyBtn, limitSellBtn, stopLossBtn}) {
            button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            button.setFocusPainted(false);
        }

        ButtonGroup group = new ButtonGroup();
        group.add(limitBuyBtn);
        group.add(limitSellBtn);
        group.add(stopLossBtn);

        typeRow.add(limitBuyBtn);
        typeRow.add(limitSellBtn);
        typeRow.add(stopLossBtn);

        // ── Buttons ───────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        btnRow.setOpaque(false);

        JButton placeBtn = new JButton("Place Order");
        AppTheme.styleButton(placeBtn, AppTheme.ACCENT);
        placeBtn.addActionListener(e -> placeOrder());

        JButton viewBtn = new JButton("View Pending");
        AppTheme.styleButton(viewBtn, AppTheme.SURFACE);
        viewBtn.addActionListener(e -> viewPendingOrders());

        btnRow.add(placeBtn);
        btnRow.add(viewBtn);

        // ── Status label ──────────────────────────────────────────────────
        statusLabel = new JLabel("Waiting for order triggers...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(AppTheme.MUTED);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        // ── Wire up the monitor callback (fires on EDT) ───────────────────
        monitor.setOnOrderExecuted(msg -> {
            statusLabel.setText("<html>" + msg.replace("✔", "✔").replace("⚠", "⚠") + "</html>");
            statusLabel.setForeground(msg.startsWith("✔") ? new Color(0, 128, 0) : Color.RED);
            JOptionPane.showMessageDialog(this, msg, "Order Alert", JOptionPane.INFORMATION_MESSAGE);
        });

        // ── Compose ───────────────────────────────────────────────────────
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(AppTheme.SURFACE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        JLabel heading = new JLabel("Limit orders & stop losses");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(AppTheme.NAVY);
        JLabel subtitle = new JLabel("Set a trigger and the monitor will watch the market for you.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 0, 14, 0));
        top.add(heading);
        top.add(subtitle);
        top.add(inputRow);
        top.add(typeRow);
        top.add(btnRow);

        add(top, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    // ── Action Handlers ───────────────────────────────────────────────────

    private void placeOrder() {
        String symbol = symbolField.getText().trim().toUpperCase();
        if (symbol.isEmpty()) {
            error("Please enter a stock symbol.");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            error("Quantity must be a positive integer.");
            return;
        }

        double trigger;
        try {
            trigger = Double.parseDouble(triggerPriceField.getText().trim());
            if (trigger <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            error("Trigger price must be a positive number.");
            return;
        }

        PendingOrder.OrderType type;
        if (limitBuyBtn.isSelected())  type = PendingOrder.OrderType.LIMIT_BUY;
        else if (limitSellBtn.isSelected()) type = PendingOrder.OrderType.LIMIT_SELL;
        else                            type = PendingOrder.OrderType.STOP_LOSS;

        PendingOrder order = new PendingOrder(symbol, qty, trigger, type);
        monitor.addOrder(order);

        statusLabel.setForeground(new Color(0, 128, 0));
        statusLabel.setText("Order placed: " + order);
        clearInputs();

        JOptionPane.showMessageDialog(this,
                "Order placed successfully!\n" + order,
                "Limit Order", JOptionPane.INFORMATION_MESSAGE);
    }

    private void viewPendingOrders() {
        List<PendingOrder> pending = monitor.getPendingOrders();
        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No pending orders.",
                    "Pending Orders", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder("Pending Orders:\n\n");
        for (PendingOrder o : pending) {
            sb.append("  ").append(o).append("\n");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Pending Orders (" + pending.size() + ")",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private JTextField styledField(int cols) {
        JTextField f = new JTextField(cols);
        AppTheme.styleField(f);
        f.setPreferredSize(new Dimension(Math.max(100, cols * 18), 38));
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

    private void clearInputs() {
        symbolField.setText("");
        quantityField.setText("");
        triggerPriceField.setText("");
    }
}
