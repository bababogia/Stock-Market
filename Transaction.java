import java.time.LocalDateTime;

/*
This class represents a single stock transaction.
Each transaction contains the following attributes:
1. `stock`: Represents the stock involved in the transaction (an instance of the `Stock` class).
2. `type`: The type of transaction, either "BUY" or "SELL".
3. `quantity`: The number of shares involved in the transaction.
4. `pricePerShare`: The price of each share at the time of the transaction.
5. `timestamp`: The exact date and time when the transaction occurred, represented as a `LocalDateTime` object.

The class includes:
- A constructor to initialize the transaction with the stock, quantity, price per share, and timestamp.
- Getter methods to retrieve each attribute.
- A `toString` method that returns a string representation of the transaction in a format suitable for storing in a file (symbol, quantity, price per share).
*/


public class Transaction {
    private Stock stock;
    private String type; 
    private int quantity;
    private double pricePerShare; 
    private LocalDateTime timestamp; 

    public Transaction(Stock stock, int quantity, double pricePerShare) {
        this.stock = stock;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
        this.timestamp = LocalDateTime.now(); 
    }

    public Stock getStock() {
        return stock;
    }

    public String getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPricePerShare() {
        return pricePerShare;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return stock.getSymbol() + "," + quantity + "," + pricePerShare;
    }

}
