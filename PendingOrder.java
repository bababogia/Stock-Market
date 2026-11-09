import java.time.LocalDateTime;

/**
 * FEATURE 2 — Limit Orders & Stop Losses
 *
 * Represents a single conditional order that has been placed but not yet
 * executed.  The {@link OrderMonitorService} polls the market price
 * periodically and fires this order when the trigger condition is met.
 *
 * Order types supported:
 *   LIMIT_BUY   — buy N shares if price DROPS to or below triggerPrice
 *   LIMIT_SELL  — sell N shares if price RISES to or above triggerPrice
 *   STOP_LOSS   — sell N shares if price DROPS to or below triggerPrice
 *
 * The semantics of LIMIT_SELL vs STOP_LOSS differ only in direction
 * of the user's intent (take-profit vs cut-loss), but the trigger
 * condition for STOP_LOSS is the same direction as LIMIT_BUY.  Both are
 * included as named constants so the GUI can label them correctly.
 */
public class PendingOrder {

    public enum OrderType {
        LIMIT_BUY,   // fire when price <= triggerPrice
        LIMIT_SELL,  // fire when price >= triggerPrice
        STOP_LOSS    // fire when price <= triggerPrice  (cut losses)
    }

    public enum OrderStatus {
        PENDING,
        EXECUTED,
        CANCELLED
    }

    private final String      symbol;
    private final int         quantity;
    private final double      triggerPrice;
    private final OrderType   type;
    private       OrderStatus status;
    private final LocalDateTime createdAt;
    private       LocalDateTime executedAt;

    public PendingOrder(String symbol, int quantity, double triggerPrice, OrderType type) {
        this.symbol       = symbol.toUpperCase();
        this.quantity     = quantity;
        this.triggerPrice = triggerPrice;
        this.type         = type;
        this.status       = OrderStatus.PENDING;
        this.createdAt    = LocalDateTime.now();
    }

    // ── Trigger Logic ─────────────────────────────────────────────────────

    /**
     * Returns true if the given live price satisfies this order's condition.
     * Called by {@link OrderMonitorService} on every price-check cycle.
     */
    public boolean isTriggerMet(double currentPrice) {
        if (status != OrderStatus.PENDING) return false;

        return switch (type) {
            case LIMIT_BUY  -> currentPrice <= triggerPrice;
            case LIMIT_SELL -> currentPrice >= triggerPrice;
            case STOP_LOSS  -> currentPrice <= triggerPrice;
        };
    }

    // ── State Transitions ─────────────────────────────────────────────────

    public void markExecuted() {
        this.status     = OrderStatus.EXECUTED;
        this.executedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public String      getSymbol()       { return symbol; }
    public int         getQuantity()     { return quantity; }
    public double      getTriggerPrice() { return triggerPrice; }
    public OrderType   getType()         { return type; }
    public OrderStatus getStatus()       { return status; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public boolean     isPending()       { return status == OrderStatus.PENDING; }

    @Override
    public String toString() {
        return String.format("[%s] %s x%d @ trigger=%.2f | status=%s",
                symbol, type, quantity, triggerPrice, status);
    }
}
