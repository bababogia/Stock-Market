import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FEATURE 2 — Limit Orders & Stop Losses (Background Worker)
 *
 * A daemon-style scheduling service that runs on a background thread,
 * polls the live price of every stock that has a pending order, and
 * executes the order when its trigger condition is met — all without
 * blocking the Swing EDT.
 *
 * Architecture highlights:
 *   • Uses a single-thread {@link ScheduledExecutorService} with a daemon
 *     thread so it shuts down automatically when the JVM exits.
 *   • The pending-order list uses {@link CopyOnWriteArrayList} so the GUI
 *     thread can safely add/cancel orders while the monitor thread iterates.
 *   • Executed orders call back onto the Swing EDT via
 *     {@code SwingUtilities.invokeLater} so GUI components (JLabel, etc.)
 *     can be updated from the callback without race conditions.
 *   • Price fetching is delegated to a {@link PriceFetcher} interface so the
 *     live Alpha Vantage call can be swapped for a simulator during testing.
 *
 * Usage:
 * <pre>
 *   OrderMonitorService monitor = new OrderMonitorService(portfolio, userAccount);
 *   monitor.setOnOrderExecuted(msg -> statusLabel.setText(msg));
 *   monitor.start(30); // poll every 30 seconds
 *
 *   monitor.addOrder(new PendingOrder("AAPL", 10, 150.00, PendingOrder.OrderType.LIMIT_BUY));
 *   // ... later ...
 *   monitor.stop();
 * </pre>
 */
public class OrderMonitorService {

    private static final Logger LOG = Logger.getLogger(OrderMonitorService.class.getName());

    // ── Functional interface so the GUI can plug in its price-fetch logic ─

    @FunctionalInterface
    public interface PriceFetcher {
        /**
         * Return the current price for the given symbol, or -1 on error.
         */
        double fetchPrice(String symbol);
    }

    // ── State ─────────────────────────────────────────────────────────────

    private final CopyOnWriteArrayList<PendingOrder> pendingOrders =
            new CopyOnWriteArrayList<>();

    private final Portfolio    portfolio;
    private final UserAccount  userAccount;
    private       PriceFetcher priceFetcher;
    private       Consumer<String> onOrderExecuted; // GUI callback

    private ScheduledExecutorService scheduler;

    // ── Constructor ───────────────────────────────────────────────────────

    public OrderMonitorService(Portfolio portfolio, UserAccount userAccount) {
        this.portfolio    = portfolio;
        this.userAccount  = userAccount;
        this.priceFetcher = this::defaultFetch;  // uses StockMarketGUI.getStockPrice
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Start polling every {@code intervalSeconds} seconds.
     * Safe to call multiple times — a running scheduler is stopped first.
     */
    public synchronized void start(long intervalSeconds) {
        stop(); // stop any previous scheduler
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OrderMonitor-Thread");
            t.setDaemon(true); // JVM exits even if this thread is still running
            return t;
        });
        scheduler.scheduleAtFixedRate(
                this::checkAllOrders,
                0, intervalSeconds, TimeUnit.SECONDS
        );
        LOG.log(Level.INFO, "OrderMonitorService started (interval={0}s)", intervalSeconds);
    }

    /** Stop the background scheduler gracefully. */
    public synchronized void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            LOG.info("OrderMonitorService stopped.");
        }
    }

    // ── Order Management (thread-safe via CopyOnWriteArrayList) ───────────

    public void addOrder(PendingOrder order) {
        pendingOrders.add(order);
        LOG.log(Level.INFO, "Order added: {0}", order);
    }

    public void cancelOrder(PendingOrder order) {
        order.cancel();
        LOG.log(Level.INFO, "Order cancelled: {0}", order);
    }

    /** Returns an unmodifiable snapshot of all orders (any status). */
    public List<PendingOrder> getAllOrders() {
        return Collections.unmodifiableList(new ArrayList<>(pendingOrders));
    }

    /** Returns only orders that are still waiting to fire. */
    public List<PendingOrder> getPendingOrders() {
        List<PendingOrder> result = new ArrayList<>();
        for (PendingOrder o : pendingOrders) {
            if (o.isPending()) result.add(o);
        }
        return Collections.unmodifiableList(result);
    }

    // ── Callback / Injectors ──────────────────────────────────────────────

    /**
     * Set a callback that will be invoked on the Swing EDT when an order
     * is executed.  The string argument contains a human-readable message.
     */
    public void setOnOrderExecuted(Consumer<String> callback) {
        this.onOrderExecuted = callback;
    }

    /** Replace the default price fetcher (useful for simulated prices). */
    public void setPriceFetcher(PriceFetcher fetcher) {
        this.priceFetcher = fetcher;
    }

    // ── Core Monitor Logic (runs on background thread) ────────────────────

    private void checkAllOrders() {
        for (PendingOrder order : pendingOrders) {
            if (!order.isPending()) continue;

            try {
                double currentPrice = priceFetcher.fetchPrice(order.getSymbol());
                if (currentPrice < 0) continue; // fetch error — try again next cycle

                if (order.isTriggerMet(currentPrice)) {
                    executeOrder(order, currentPrice);
                }
            } catch (RuntimeException ex) {
                LOG.log(Level.WARNING, "Error checking order " + order, ex);
            }
        }
    }

    /**
     * Execute a triggered order.  All portfolio/account mutations happen on
     * the background thread; only the GUI callback is dispatched to the EDT.
     */
    private void executeOrder(PendingOrder order, double executionPrice) {
        String msg;

        switch (order.getType()) {
            case LIMIT_BUY -> {
                double totalCost = executionPrice * order.getQuantity();
                if (!userAccount.canAfford(totalCost)) {
                    msg = String.format(
                            "⚠ LIMIT BUY for %s FAILED — insufficient funds (need €%.2f).",
                            order.getSymbol(), totalCost);
                    break;
                }
                userAccount.deduct(totalCost);
                Stock stock = new Stock(order.getSymbol(), executionPrice, order.getQuantity());
                portfolio.buyStock(stock, order.getQuantity(), executionPrice);
                order.markExecuted();
                msg = String.format(
                        "✔ LIMIT BUY executed: %d × %s @ €%.2f  (total €%.2f)",
                        order.getQuantity(), order.getSymbol(), executionPrice, totalCost);
            }
            case LIMIT_SELL, STOP_LOSS -> {
                double profit = portfolio.searchAndSellStock(
                        order.getSymbol(), order.getQuantity(), executionPrice);
                if (Double.isNaN(profit)) {
                    msg = String.format(
                            "⚠ %s for %s FAILED — not enough shares in portfolio.",
                            order.getType(), order.getSymbol());
                    break;
                }
                userAccount.loadMoney(executionPrice * order.getQuantity());
                order.markExecuted();
                msg = String.format(
                        "✔ %s executed: %d × %s @ €%.2f  P&L: €%.2f",
                        order.getType(), order.getQuantity(),
                        order.getSymbol(), executionPrice, profit);
            }
            default -> msg = "Unknown order type.";
        }

        final String finalMsg = msg;
        LOG.info(finalMsg);
        if (onOrderExecuted != null) {
            SwingUtilities.invokeLater(() -> onOrderExecuted.accept(finalMsg));
        }
    }

    // ── Default Price Fetcher (delegates to StockMarketGUI) ───────────────

    private double defaultFetch(String symbol) {
        return StockMarketGUI.getStockPrice(symbol);
    }
}
