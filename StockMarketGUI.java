import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Main application window — integrates the trading tools in a tabbed layout:
 *
 *   Tab "Trading"     — the original Buy / Sell / Portfolio / Deposit UI
 *   Tab "Performance" — PerformancePanel (Feature 1)
 *   Tab "Orders"      — LimitOrderPanel   (Feature 2)
 *   Tab "Bot"         — TradingBotPanel   (Feature 3)
 *
 */
public class StockMarketGUI extends JFrame {

    private static final Pattern GLOBAL_QUOTE_PRICE = Pattern.compile(
            "\"Global Quote\"\\s*:\\s*\\{[^}]*\"05\\. price\"\\s*:\\s*\"([0-9]+(?:\\.[0-9]+)?)\"");

    // ── Fields (all final — assigned exactly once during construction) ────
    private final JTextField       stockCode;
    private final JTextField       quantity;
    private final JTextField       sellStockCode;
    private final JTextField       sellQuantity;
    private final JTextField       txtSellPrice;
    private final JTextArea        balanceArea;
    private final PerformancePanel performancePanel;
    private final OrderMonitorService orderMonitor;

    private final UserAccount        userAccount    = new UserAccount();
    private final PerformanceTracker tracker        = new PerformanceTracker();
    private       double             totalDeposited = 0.0;

    // ── Constructor ───────────────────────────────────────────────────────

    public StockMarketGUI(Portfolio portfolio) {

        setTitle("Stock Market");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(820, 650));
        setSize(980, 740);
        setLocationRelativeTo(null);
        setIconImage(Toolkit.getDefaultToolkit().getImage("app-logo.jpg"));

        // Start background order monitor (Feature 2)
        orderMonitor = new OrderMonitorService(portfolio, userAccount);
        orderMonitor.start(60);

        // Stop daemon cleanly on window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                orderMonitor.stop();
                dispose();
                System.exit(0);
            }
        });

        // Build trading tab — this assigns the input-field references
        TradingComponents tc = buildTradingComponents(portfolio);
        stockCode    = tc.stockCode;
        quantity     = tc.quantity;
        sellStockCode = tc.sellStockCode;
        sellQuantity = tc.sellQuantity;
        txtSellPrice = tc.txtSellPrice;
        balanceArea  = tc.balanceArea;

        // Build performance panel wrapper (Feature 1)
        performancePanel = new PerformancePanel(tracker, userAccount);

        // Compose tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.setBackground(AppTheme.BACKGROUND);
        tabs.addTab("Trading",     tc.panel);
        tabs.addTab("Performance", performancePanel);
        tabs.addTab("Orders",      new LimitOrderPanel(orderMonitor));
        tabs.addTab("Strategy Lab", new TradingBotPanel());

        getContentPane().add(tabs);
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Trading Tab — original UI
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Small holder so buildTradingComponents() can return both the panel
     * and the field references that the action handlers need.
     */
    private static class TradingComponents {
        JPanel     panel;
        JTextField stockCode;
        JTextField quantity;
        JTextField sellStockCode;
        JTextField sellQuantity;
        JTextField txtSellPrice;
        JTextArea  balanceArea;
    }

    private TradingComponents buildTradingComponents(Portfolio portfolio) {
        TradingComponents tc = new TradingComponents();
        JPanel panel = new JPanel(new BorderLayout(0, 22));
        panel.setBackground(AppTheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Welcome to your trading desk");
        title.setForeground(AppTheme.NAVY);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        JLabel subtitle = new JLabel("Manage your orders, cash and positions in one place.");
        subtitle.setForeground(AppTheme.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JPanel titleBlock = new JPanel(new BorderLayout(0, 5));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);
        heading.add(titleBlock, BorderLayout.WEST);
        panel.add(heading, BorderLayout.NORTH);

        JPanel tradeCards = new JPanel(new GridLayout(1, 2, 20, 0));
        tradeCards.setOpaque(false);
        JPanel buyCard = createTradeCard("Buy shares", "Enter a symbol and quantity to place a market buy.");
        JPanel sellCard = createTradeCard("Sell shares", "Enter the sale price and quantity for your position.");

        tc.stockCode = new JTextField();
        AppTheme.styleField(tc.stockCode);
        tc.quantity = new JTextField();
        AppTheme.styleField(tc.quantity);
        tc.txtSellPrice = new JTextField();
        AppTheme.styleField(tc.txtSellPrice);

        addField(buyCard, "Stock symbol", tc.stockCode);
        addField(buyCard, "Quantity", tc.quantity);
        JButton buyBtn = new JButton("Buy shares");
        AppTheme.styleButton(buyBtn, AppTheme.SUCCESS);
        buyBtn.addActionListener(e -> handleBuy(portfolio));
        buyBtn.setAlignmentX(LEFT_ALIGNMENT);
        buyCard.add(buyBtn);

        tc.sellStockCode = new JTextField();
        AppTheme.styleField(tc.sellStockCode);
        tc.sellQuantity = new JTextField();
        AppTheme.styleField(tc.sellQuantity);
        addField(sellCard, "Stock symbol", tc.sellStockCode);
        addField(sellCard, "Quantity", tc.sellQuantity);
        tc.txtSellPrice.setText("");
        addField(sellCard, "Sell price (€)", tc.txtSellPrice);
        JButton sellBtn = new JButton("Sell shares");
        AppTheme.styleButton(sellBtn, AppTheme.ACCENT);
        sellBtn.addActionListener(e -> handleSell(portfolio));
        sellBtn.setAlignmentX(LEFT_ALIGNMENT);
        sellCard.add(sellBtn);
        tradeCards.add(buyCard);
        tradeCards.add(sellCard);
        panel.add(tradeCards, BorderLayout.CENTER);

        JPanel accountBar = new JPanel(new BorderLayout(18, 0));
        accountBar.setBackground(AppTheme.SURFACE);
        accountBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        JPanel wallet = new JPanel(new BorderLayout(0, 3));
        wallet.setOpaque(false);
        JLabel walletLbl = new JLabel("AVAILABLE CASH");
        walletLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        walletLbl.setForeground(AppTheme.MUTED);
        tc.balanceArea = new JTextArea("€0.00");
        tc.balanceArea.setFont(new Font("Segoe UI", Font.BOLD, 23));
        tc.balanceArea.setForeground(AppTheme.NAVY);
        tc.balanceArea.setOpaque(false);
        tc.balanceArea.setEditable(false);
        wallet.add(walletLbl, BorderLayout.NORTH);
        wallet.add(tc.balanceArea, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 4, 9, 0));
        actions.setOpaque(false);
        JButton depositBtn = new JButton("Deposit");
        AppTheme.styleButton(depositBtn, AppTheme.ACCENT);
        depositBtn.addActionListener(e -> handleDeposit());
        JButton checkBtn = new JButton("Check Price");
        AppTheme.styleButton(checkBtn, AppTheme.SURFACE);
        checkBtn.addActionListener(e -> handleCheckPrice());
        JButton portBtn = new JButton("Portfolio");
        AppTheme.styleButton(portBtn, AppTheme.SURFACE);
        portBtn.addActionListener(e -> openPortfolioFile());
        JButton infoBtn = new JButton("Info");
        AppTheme.styleButton(infoBtn, AppTheme.SURFACE);
        infoBtn.addActionListener(e -> showInfo());
        actions.add(depositBtn);
        actions.add(checkBtn);
        actions.add(portBtn);
        actions.add(infoBtn);
        accountBar.add(wallet, BorderLayout.WEST);
        accountBar.add(actions, BorderLayout.CENTER);
        panel.add(accountBar, BorderLayout.SOUTH);

        tc.panel = panel;
        return tc;
    }

    private JPanel createTradeCard(String title, String description) {
        JPanel card = new JPanel();
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 19));
        heading.setForeground(AppTheme.NAVY);
        JLabel detail = new JLabel("<html><div style='width:290px'>" + description + "</div></html>");
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detail.setForeground(AppTheme.MUTED);
        detail.setBorder(BorderFactory.createEmptyBorder(5, 0, 18, 0));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        detail.setAlignmentX(LEFT_ALIGNMENT);
        card.add(heading);
        card.add(detail);
        return card;
    }

    private void addField(JPanel card, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(AppTheme.TEXT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        card.add(label);
        if (field == null) {
            field = new JTextField();
            AppTheme.styleField(field);
            if (labelText.equals("Stock symbol")) card.putClientProperty("symbolField", field);
            else card.putClientProperty("quantityField", field);
        }
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                field.getBorder(), BorderFactory.createEmptyBorder(3, 0, 12, 0)));
        card.add(field);
    }

    // ── Action Handlers ───────────────────────────────────────────────────

    private void handleBuy(Portfolio portfolio) {
        String symbol = stockCode.getText().trim();
        int qty;
        try {
            qty = Integer.parseInt(quantity.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null,
                    "Quantity must be a positive integer.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double price = getStockPrice(symbol);
        if (price < 0) return;

        double cost = price * qty;
        if (!userAccount.canAfford(cost)) {
            JOptionPane.showMessageDialog(null,
                    "Insufficient funds.\nBalance: €" + String.format("%.2f", userAccount.getBalance()),
                    "Insufficient Balance", JOptionPane.WARNING_MESSAGE);
            return;
        }

        userAccount.deduct(cost);
        Stock stock = new Stock(symbol, price, qty);
        portfolio.buyStock(stock, qty, price);
        refreshBalance();
        performancePanel.setCurrentHoldingsValue(cost);
        JOptionPane.showMessageDialog(null,
                String.format("Purchase completed!\nBalance: €%.2f", userAccount.getBalance()));
    }

    private void handleSell(Portfolio portfolio) {
        String symbol = sellStockCode.getText().trim();
        int qty;
        try {
            qty = Integer.parseInt(sellQuantity.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Quantity must be a positive integer!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double sellPrice;
        try {
            sellPrice = Double.parseDouble(txtSellPrice.getText().trim());
            if (!Double.isFinite(sellPrice) || sellPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Sell price must be a positive number!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double profit = portfolio.searchAndSellStock(symbol, qty, sellPrice);
        if (Double.isNaN(profit)) {
            JOptionPane.showMessageDialog(null,
                    "Sale failed — not enough shares.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        userAccount.loadMoney(qty * sellPrice);
        refreshBalance();
        JOptionPane.showMessageDialog(null,
                String.format("Sold successfully!\nProceeds: €%.2f\nP&L: €%.2f",
                        (double) qty * sellPrice, profit));
    }

    private void handleDeposit() {
        String input = JOptionPane.showInputDialog("Enter amount to deposit:");
        if (input == null) return;
        try {
            double amount = Double.parseDouble(input.trim());
            if (!Double.isFinite(amount) || amount <= 0) throw new NumberFormatException();
            userAccount.loadMoney(amount);
            totalDeposited += amount;
            performancePanel.setNetDeposited(totalDeposited);
            refreshBalance();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCheckPrice() {
        String symbol = stockCode.getText().trim();
        double price = getStockPrice(symbol);
        if (price >= 0)
            JOptionPane.showMessageDialog(null, symbol + " current price: €" + price);
    }

    private void openPortfolioFile() {
        try {
            File f = new File("portfolio.txt");
            if (f.exists()) Desktop.getDesktop().open(f);
            else JOptionPane.showMessageDialog(null,
                    "portfolio.txt not found.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Cannot open file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showInfo() {
        JTextArea area = new JTextArea("""
                • Buy stocks by entering symbol + quantity.
                • Sell stocks by entering symbol, quantity and sell price.
                • Deposit cash to fund your account.
                • Performance tab: view P&L snapshots.
                • Orders tab: set limit buy/sell and stop-loss triggers.
                • Strategy Lab: backtest the MA-crossover strategy.

                portfolio.txt format: symbol,quantity,pricePerShare""");
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(null,
                new JScrollPane(area), "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshBalance() {
        balanceArea.setText(String.format("%.2f", userAccount.getBalance()));
    }

    // ── Static price fetch — shared with OrderMonitorService ──────────────

    public static double getStockPrice(String symbol) {
        String apiKey = "FCLLB1H0VO49DDCG";
        String urlString = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol="
                + URLEncoder.encode(symbol.trim(), StandardCharsets.UTF_8) + "&apikey=" + apiKey;
        try {
            URI  uri  = URI.create(urlString);
            URL  url  = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);

            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                }

                Matcher matcher = GLOBAL_QUOTE_PRICE.matcher(sb);
                if (!matcher.find()) throw new IOException("Price was not present in the API response.");
                double price = Double.parseDouble(matcher.group(1));
                if (!Double.isFinite(price) || price <= 0) {
                    throw new IOException("API returned an invalid stock price.");
                }
                return price;
            } finally {
                conn.disconnect();
            }
        } catch (IOException | IllegalArgumentException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Unable to retrieve a price. Check the symbol and try again.",
                    "Price Lookup Failed", JOptionPane.ERROR_MESSAGE));
            return -1;
        }
    }
}
