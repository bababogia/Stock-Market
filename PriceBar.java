import java.time.LocalDate;

/**
 * FEATURE 3 — Trading Bot & Backtesting
 *
 * A single OHLCV (Open / High / Low / Close / Volume) candle for one
 * trading day.  The backtester and the moving-average calculator work
 * entirely on lists of {@code PriceBar} objects, so the same logic works
 * whether you feed it live data or historical CSV data.
 */
public class PriceBar {

    private final LocalDate date;
    private final double    open;
    private final double    high;
    private final double    low;
    private final double    close;
    private final long      volume;

    public PriceBar(LocalDate date, double open, double high,
                    double low, double close, long volume) {
        this.date   = date;
        this.open   = open;
        this.high   = high;
        this.low    = low;
        this.close  = close;
        this.volume = volume;
    }

    /** Convenience constructor when only close prices are available. */
    public PriceBar(LocalDate date, double close) {
        this(date, close, close, close, close, 0L);
    }

    public LocalDate getDate()  { return date; }
    public double    getOpen()  { return open; }
    public double    getHigh()  { return high; }
    public double    getLow()   { return low; }
    public double    getClose() { return close; }
    public long      getVolume(){ return volume; }

    @Override
    public String toString() {
        return String.format("[%s] O=%.2f H=%.2f L=%.2f C=%.2f V=%d",
                date, open, high, low, close, volume);
    }
}
