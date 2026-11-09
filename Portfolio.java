import java.io.*;
import java.util.*;

/**
 * Manages the user's stock holdings, persisted in portfolio.txt.
 *
 * Changes over the original:
 *   • estimateHoldingsValue(Map<String,Double>) — sums the market value of all
 *     open positions using a caller-supplied price map.  Used by
 *     PerformancePanel when taking a P&L snapshot.
 *   • No other logic was altered; the file format (symbol,qty,price) is
 *     kept identical so existing portfolio.txt files remain readable.
 */
public class Portfolio {

    private final List<Transaction> transactions;
    private final File portfolioFile;

    public Portfolio() {
        this.transactions  = new ArrayList<>();
        this.portfolioFile = new File("portfolio.txt");
    }

    // ── Buy ───────────────────────────────────────────────────────────────

    public void buyStock(Stock stock, int quantity, double pricePerShare) {
        Transaction tx = new Transaction(stock, quantity, pricePerShare);
        transactions.add(tx);
        saveTransactionToFile(tx);
    }

    // ── Sell ──────────────────────────────────────────────────────────────

    /** Returns realized profit/loss, or NaN if the sale cannot be completed. */
    public double searchAndSellStock(String stockSymbol, int quantityToSell, double sellingPrice) {
        if (quantityToSell <= 0 || !Double.isFinite(sellingPrice) || sellingPrice <= 0) {
            return Double.NaN;
        }

        List<String> updated   = new ArrayList<>();
        int     remaining      = quantityToSell;
        double  totalCost      = 0.0;
        boolean found          = false;

        try (BufferedReader br = new BufferedReader(new FileReader(portfolioFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3) continue;

                String symbol   = parts[0];
                int    qty      = Integer.parseInt(parts[1]);
                double buyPrice = Double.parseDouble(parts[2]);

                if (symbol.equals(stockSymbol) && remaining > 0) {
                    found = true;
                    if (qty <= remaining) {
                        remaining -= qty;
                        totalCost += qty * buyPrice;
                    } else {
                        totalCost += remaining * buyPrice;
                        updated.add(symbol + "," + (qty - remaining) + "," + buyPrice);
                        remaining = 0;
                    }
                } else {
                    updated.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading portfolio: " + e.getMessage());
            return Double.NaN;
        }

        if (!found || remaining > 0) return Double.NaN;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(portfolioFile))) {
            for (String l : updated) { bw.write(l); bw.newLine(); }
        } catch (IOException e) {
            System.err.println("Error writing portfolio: " + e.getMessage());
            return Double.NaN;
        }

        return quantityToSell * sellingPrice - totalCost;
    }

    // ── Feature 1 helper: estimate current market value of all holdings ───

    /**
     * Reads portfolio.txt and sums (quantity × currentPrice) for each
     * position using prices from the supplied map.  If a symbol is not in
     * the map it uses the original buy price as a fallback.
     *
     * @param currentPrices  map of symbol → latest market price
     * @return total market value of all open positions
     */
    public double estimateHoldingsValue(Map<String, Double> currentPrices) {
        double total = 0.0;
        if (!portfolioFile.exists()) return 0.0;

        try (BufferedReader br = new BufferedReader(new FileReader(portfolioFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                String symbol   = parts[0];
                int    qty      = Integer.parseInt(parts[1]);
                double buyPrice = Double.parseDouble(parts[2]);
                double mktPrice = currentPrices.getOrDefault(symbol, buyPrice);
                total += qty * mktPrice;
            }
        } catch (IOException e) {
            System.err.println("Error estimating holdings: " + e.getMessage());
        }
        return total;
    }

    // ── Persistence ───────────────────────────────────────────────────────

    private void saveTransactionToFile(Transaction transaction) {
        try (FileWriter fw = new FileWriter(portfolioFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(transaction.toString());
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
        }
    }
}
