import java.util.ArrayList;
import java.util.List;

/*
This class represents a stock with basic operations for buying and selling shares.
Each stock has the following attributes:
1. `name`: The name or symbol of the stock.
2. `price`: The current price of the stock per share.
3. `quantity`: The number of shares available in the stock.

The class includes the following methods:
- A constructor to initialize the stock with a name, price, and quantity.
- Getter methods to retrieve the stock's name, price, and quantity.
- A method `buy` that allows buying more shares of the stock (increases quantity).
- A method `sell` that allows selling shares of the stock (decreases quantity if enough shares are available).
*/

public class Stock {

	private String name;
	private double price;
	private int quantity;
	private List<Stock> stocks;
	
	Stock(String name, double price, int quantity){
		this.name = name;
		this.price = price;
		this.quantity = quantity;
		this.stocks = new ArrayList<>();
	}
	
	 // Getters
    public String getSymbol() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    // Buy Stock
    public void buy(int amount) {
        if (amount > 0) {
            this.quantity += amount;
        }
    }

    // Sell Stock
    public boolean sell(int amount) {
        if (amount > 0 && amount <= quantity) {
            this.quantity -= amount;
            return true; 
        }
        return false; 
    }
	
	
}
