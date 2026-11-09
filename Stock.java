/*
 * Represents a single stock with basic buy/sell operations.
 *
 * Changes from original:
 *   - Removed the unused internal `stocks` list (Stock.stocks was never read).
 *   - Made `name` and `price` final — they are set once in the constructor
 *     and never reassigned, so final is both more correct and cleaner.
 */
public class Stock {

    private final String name;
    private final double price;
    private int quantity;

    Stock(String name, double price, int quantity) {
        this.name     = name;
        this.price    = price;
        this.quantity = quantity;
    }

    // Getters
    public String getSymbol()   { return name; }
    public double getPrice()    { return price; }
    public int    getQuantity() { return quantity; }

    // Buy stock (increase held quantity)
    public void buy(int amount) {
        if (amount > 0) {
            this.quantity += amount;
        }
    }

    // Sell stock (decrease held quantity if sufficient shares available)
    public boolean sell(int amount) {
        if (amount > 0 && amount <= quantity) {
            this.quantity -= amount;
            return true;
        }
        return false;
    }
}
