import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



/*
This class represents a Portfolio for managing stock transactions.
It provides functionality for:
1. Buying stocks: The `buyStock` method creates a new transaction for buying stocks and adds it to the portfolio.
2. Saving transactions: The `saveTransactionToFile` method saves each stock transaction to a file (portfolio.txt).
3. Searching and selling stocks: The `searchAndSellStock` method searches for a stock symbol in the portfolio and sells the stock by updating the file. If there are enough stocks to sell, the file is updated, and the sold stocks are removed from the portfolio.
4. Each transaction consists of the stock symbol, quantity, and price per share. These are stored as records in the portfolio file.
5. Error handling: The program handles exceptions that may occur during reading or writing to the portfolio file and provides error messages when things go wrong.
6. The file `portfolio.txt` stores all stock transactions in a simple text format, with each transaction on a new line.

The class uses an ArrayList to hold the transactions in memory, and changes are persisted to the `portfolio.txt` file after each operation.
*/


public class Portfolio {
    private List<Transaction> transactions;
    private File portfolioFile;

    public Portfolio() {
        this.transactions = new ArrayList<>();
        this.portfolioFile = new File("portfolio.txt");
    }

    // Αγορά μετοχής και προσθήκη στο χαρτοφυλάκιο
    public void buyStock(Stock stock, int quantity, double pricePerShare) {
        Transaction transaction = new Transaction(stock,quantity, pricePerShare);
        transactions.add(transaction);
        saveTransactionToFile(transaction);
        
    }
    


    // Save to portfolio.txt
    private void saveTransactionToFile(Transaction transaction) {
        try (FileWriter writer = new FileWriter(portfolioFile, true);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(transaction.toString());
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
        }
    }
    
    public double searchAndSellStock(String stockSymbol, int quantityToSell, double sellingPrice) {
        List<String> updatedTransactions = new ArrayList<>();
        
        int remainingQuantity = quantityToSell;
        double totalPurchaseCost = 0.0;
        boolean stockFound = false;

        try (BufferedReader br = new BufferedReader(new FileReader(portfolioFile))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3) continue;

                String symbol = parts[0];
                int quantity = Integer.parseInt(parts[1]);
                double purchasePrice = Double.parseDouble(parts[2]);

                if (symbol.equals(stockSymbol) && remainingQuantity > 0) {
                    stockFound = true;

                    if (quantity <= remainingQuantity) {
                        remainingQuantity -= quantity;
                        totalPurchaseCost += quantity * purchasePrice;
                        // Μην προσθέτεις ξανά στο updatedTransactions — τις πουλάμε όλες
                    } else {
                        int remainingInFile = quantity - remainingQuantity;
                        totalPurchaseCost += remainingQuantity * purchasePrice;
                        updatedTransactions.add(symbol + "," + remainingInFile + "," + purchasePrice);
                        remainingQuantity = 0;
                    }
                } else {
                    updatedTransactions.add(line); // Δεν αφορά αυτή τη μετοχή
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading portfolio file: " + e.getMessage());
            return -1.0;
        }

        if (!stockFound || remainingQuantity > 0) {

            return -1.0;
        }

        // Ενημέρωση του αρχείου με τις νέες καταστάσεις
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(portfolioFile))) {
            for (String updatedLine : updatedTransactions) {
                bw.write(updatedLine);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error updating portfolio file: " + e.getMessage());
            return -1.0;
        }

        // Υπολογισμός κέρδους/ζημίας
        double totalSellingRevenue = quantityToSell * sellingPrice;
        double profit = totalSellingRevenue - totalPurchaseCost;


        return profit;
    }
}


//        // Υπολογισμός Κέρδους ή Ζημίας
//        if (quantityToSell > 0) {
//            double totalSellingPrice = quantityToSell * sellingPrice;
//            double profitOrLoss = totalSellingPrice - totalPurchaseCost;
//            System.out.println("Sold " + quantityToSell + " shares of " + stockSymbol);
//            System.out.println("Total Purchase Cost: $" + totalPurchaseCost);
//            System.out.println("Total Selling Price: $" + totalSellingPrice);
//            System.out.println("Profit/Loss: $" + profitOrLoss);
//        } else {
//            System.out.println("No stocks were sold.");
//        }
   


