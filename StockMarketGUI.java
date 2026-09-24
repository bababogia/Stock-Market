import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.json.JSONObject;

/*
This class represents the GUI for the Stock Market application.
It allows the user to perform the following actions:
1. Enter a stock symbol and quantity of shares to buy.
2. Perform a buy or sell operation for a stock.
3. View and manage the portfolio stored in a text file.
4. Display information about the application's functionality.

Key Components:
- Stock Code Input Field: Where the user enters the stock symbol.
- Quantity Input Field: Where the user enters the quantity of shares to buy or sell.
- Buy Button: Executes a buy operation for the entered stock symbol and quantity.
- Sell Button: Executes a sell operation for the entered stock symbol and quantity.
- Portfolio Button: Opens the portfolio file where all stock transactions are stored.
- Info Button: Displays a window with application information.
- API Connection: Fetches live stock prices from Alpha Vantage API.
*/


public class StockMarketGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField stockCode;
	private JLabel lblQuantintyYouWant;
	private JTextField quantity;
	private JButton btnNewButton_1;
	private JButton btnNewButton_2;
	private JTextField txtSellPrice;
	
	
	public StockMarketGUI(Portfolio portfolio) {
		UserAccount userAccount = new UserAccount();

		//WINDOW MAKER
		setIconImage(Toolkit.getDefaultToolkit().getImage("C:\\Users\\Mario\\eclipse-workspace\\StockMarket\\app-logo.jpg"));
		setTitle("Stock Market");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 514, 679);
		contentPane = new JPanel();
		contentPane.setBackground(new Color(255, 255, 255));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Enter Stock Symbol");
		lblNewLabel.setBackground(new Color(255, 255, 255));
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 24));
		lblNewLabel.setBounds(10, 102, 215, 40);
		contentPane.add(lblNewLabel);
		
		stockCode = new JTextField();
		stockCode.setFont(new Font("Tahoma", Font.PLAIN, 24));
		stockCode.setBounds(235, 105, 246, 46);
		contentPane.add(stockCode);
		stockCode.setColumns(10);
		
		lblQuantintyYouWant = new JLabel("Quantity");
		lblQuantintyYouWant.setFont(new Font("Tahoma", Font.PLAIN, 24));
		lblQuantintyYouWant.setBounds(128, 172, 97, 40);
		contentPane.add(lblQuantintyYouWant);
		
		quantity = new JTextField();
		quantity.setFont(new Font("Tahoma", Font.PLAIN, 24));
		quantity.setColumns(10);
		quantity.setBounds(235, 175, 246, 46);
		contentPane.add(quantity);
		
		JLabel lblSellPrice = new JLabel("Sell Price");
		lblSellPrice.setFont(new Font("Tahoma", Font.PLAIN, 24));
		lblSellPrice.setBounds(128, 328, 97, 40);
		contentPane.add(lblSellPrice);
		
		txtSellPrice = new JTextField();
		txtSellPrice.setFont(new Font("Tahoma", Font.PLAIN, 24));
		txtSellPrice.setColumns(10);
		txtSellPrice.setBounds(235, 331, 246, 46);
		contentPane.add(txtSellPrice);
	
		//Money
		JTextArea textArea = new JTextArea();
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 24));
		textArea.setEditable(false);
		textArea.setBounds(80, 453, 182, 35);
		contentPane.add(textArea);

		
		
		//BUY SECTION
		JButton btnNewButton = new JButton("Buy");
		btnNewButton.setForeground(Color.WHITE);
		btnNewButton.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        String symbol = stockCode.getText();
		        int stockQuantity;

		        String input = quantity.getText();
		        try {
		            stockQuantity = Integer.parseInt(input);
		            if (stockQuantity <= 0) {
		                JOptionPane.showMessageDialog(null, "Quantity must be over 0.", "Error", JOptionPane.ERROR_MESSAGE);
		                return;
		            }
		        } catch (NumberFormatException e1) {
		            JOptionPane.showMessageDialog(null, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
		            return;
		        }

		        double pricePerShare = 0.0;

		        try {
		            pricePerShare = getStockPrice(symbol);
		        } catch (Exception e1) {
		            e1.printStackTrace();
		        }

		        if (pricePerShare != -1) {
		            double totalCost = pricePerShare * stockQuantity;

		            if (userAccount.canAfford(totalCost)) {
		                userAccount.deduct(totalCost);

		                Stock stock = new Stock(symbol, pricePerShare, stockQuantity);
		                stock.buy(stockQuantity);
		                portfolio.buyStock(stock, stockQuantity, pricePerShare);

		                // Update the textArea with the new balance
		                textArea.setText(String.format("%.2f", userAccount.getBalance()));

		                JOptionPane.showMessageDialog(null, "Purchase completed successfully!\nBalance: €" + String.format("%.2f", userAccount.getBalance()));
		            } else {
		                JOptionPane.showMessageDialog(null, "Insufficient funds.\nBalance: €" + userAccount.getBalance(), "Insufficient Balance", JOptionPane.WARNING_MESSAGE);
		            }
		        }
		    }
		});



		
		btnNewButton.setBackground(new Color(30, 144, 255));
		btnNewButton.setFont(new Font("Arial", Font.PLAIN, 20));
		btnNewButton.setBounds(347, 252, 134, 40);
		contentPane.add(btnNewButton);

		
		
		//SELL SECTION
		JButton btnSell = new JButton("Sell");
		btnSell.setForeground(Color.WHITE);
		btnSell.setFont(new Font("Arial", Font.PLAIN, 20));
		btnSell.setBackground(new Color(30, 144, 255));
		btnSell.setBounds(347, 418, 134, 40);
		
		
		contentPane.add(btnSell);
		
		
		btnSell.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        String stockSymbol = stockCode.getText();
		        int stockQuantity;

		        try {
		            stockQuantity = Integer.parseInt(quantity.getText());
		        } catch (NumberFormatException ex) {
		            JOptionPane.showMessageDialog(null, "Invalid quantity! Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
		            return;
		        }

		        double sellingPrice;
		        
		        try {
		            sellingPrice = Double.parseDouble(txtSellPrice.getText());
		        } catch (NumberFormatException ex) {
		            JOptionPane.showMessageDialog(null, "Invalid price format!", "Error", JOptionPane.ERROR_MESSAGE);
		            return;
		        }

		        // Execute the sale and get the profit/loss
		        double profit = portfolio.searchAndSellStock(stockSymbol, stockQuantity, sellingPrice);

		        if (profit != -1) {
		            // Calculate total amount from the sale (quantity * price)
		            double saleAmount = stockQuantity * sellingPrice;
		            
		            // Update the wallet (add the sale amount)
		            userAccount.loadMoney(saleAmount);  // Assuming `loadMoney` adds to the balance
		            
		            // Show success message with profit/loss
		            JOptionPane.showMessageDialog(null,
		                String.format("Stock sold successfully!\nAmount added to wallet: %.2f€\nProfit/Loss: %.2f€", 
		                    saleAmount, profit),
		                "Success",
		                JOptionPane.INFORMATION_MESSAGE);
		            
		            // Update the displayed balance (if you have a textArea for the wallet)
		            textArea.setText(String.format("%.2f", userAccount.getBalance()));
		        } else {
		            JOptionPane.showMessageDialog(null,
		                "Stock sale failed! Please check if you have enough shares.",
		                "Error",
		                JOptionPane.ERROR_MESSAGE);
		        }
		    }
		});


		//PORTFOLIO SECTION
		btnNewButton_1 = new JButton("Portfolio");
		btnNewButton_1.setForeground(new Color(255, 255, 255));
		btnNewButton_1.setBackground(new Color(112, 128, 144));
		btnNewButton_1.setFont(new Font("Arial", Font.PLAIN, 18));
		
		
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    try {
		            File file = new File("portfolio.txt");
		            if (file.exists()) {
		                Desktop.getDesktop().open(file);
		            } else {
		                JOptionPane.showMessageDialog(null, "portfolio.txt not found.", "Error", JOptionPane.ERROR_MESSAGE);
		            }
		        } catch (IOException ex) {
		            JOptionPane.showMessageDialog(null, "Error opening file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		        }
			}
		});
		btnNewButton_1.setBounds(175, 592, 134, 41);
		contentPane.add(btnNewButton_1);
		
		btnNewButton_2 = new JButton("Info");
		btnNewButton_2.setForeground(new Color(255, 255, 255));
		btnNewButton_2.setBackground(new Color(112, 128, 144));
		btnNewButton_2.setFont(new Font("Arial", Font.PLAIN, 18));
		btnNewButton_2.setBounds(347, 591, 134, 40);
		contentPane.add(btnNewButton_2);
		
	
		
		btnNewButton_2.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        JFrame infoFrame = new JFrame("Informations");
		        infoFrame.setSize(400, 300);
		        infoFrame.setLocationRelativeTo(null); // Κέντρο της οθόνης
		        infoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Μόνο αυτό το παράθυρο κλείνει

		        JTextArea textArea = new JTextArea();
		        textArea.setText("• Adding new stock purchases by specifying the stock code, quantity\r\n"
		        		+"\n"
		        		+"• Sell stocks by specifying the stock code,quantity and sell price"
		        		+"\n"
		        		+"\n"
		        		+"• Load money by specifying the amount you want to add"
		        		+"\n"
		        		+"\n"
		        		+"• All portfolio data is stored and managed through a file named Portfolio.txt.\r\n"
		        		+"\n"
		        		
		        		+ "• Each line in the Portfolio.txt follows the structure:\r\n"
		        		
		        		+ "CODE|QUANTITY|PRICE PER UNIT\r\n");
		        textArea.setWrapStyleWord(true);
		        textArea.setLineWrap(true);
		        textArea.setEditable(false);
		        
		        infoFrame.getContentPane().add(new JScrollPane(textArea)); // αν έχεις πολλά κείμενα, scroll αυτόματα
		        infoFrame.setVisible(true);
		    }
		});
		
		
		JButton btnNewButton_3 = new JButton("Check Stock Price");
		btnNewButton_3.setForeground(new Color(255, 255, 255));
		btnNewButton_3.setBackground(new Color(112, 128, 144));
		btnNewButton_3.setFont(new Font("Arial", Font.PLAIN, 12));
		btnNewButton_3.setBounds(10, 592, 134, 42);
		contentPane.add(btnNewButton_3);
		

		
		JLabel lblWallet = new JLabel("Wallet:");
		lblWallet.setFont(new Font("Tahoma", Font.PLAIN, 22));
		lblWallet.setBackground(Color.WHITE);
		lblWallet.setBounds(10, 451, 78, 40);
		contentPane.add(lblWallet);
		
		
		
		JButton btnNewButton_3_1 = new JButton("Deposit");
		btnNewButton_3_1.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        String input = JOptionPane.showInputDialog("Enter amount to load:");
		        
		        // Check if user pressed Cancel or closed the dialog
		        if (input == null) {
		            return; 
		        }
		        
		        try {
		            double amount = Double.parseDouble(input);
		            if (amount <= 0) {
		                JOptionPane.showMessageDialog(null, "Amount must be greater than 0.", "Error", JOptionPane.ERROR_MESSAGE);
		            } else {
		                userAccount.loadMoney(amount);

		                textArea.setText(String.format("%.2f", userAccount.getBalance()));
		            }
		        } catch (NumberFormatException ex) {
		            JOptionPane.showMessageDialog(null, "Invalid number entered.", "Error", JOptionPane.ERROR_MESSAGE);
		        }
		    }
		});


		
		btnNewButton_3_1.setForeground(new Color(255, 255, 255));
		btnNewButton_3_1.setFont(new Font("Arial", Font.PLAIN, 20));
		btnNewButton_3_1.setBackground(new Color(30, 144, 255));
		btnNewButton_3_1.setBounds(10, 511, 137, 40);
		contentPane.add(btnNewButton_3_1);
		
		JLabel lblStockMarket = new JLabel("STOCK MARKET");
		lblStockMarket.setForeground(new Color(0, 102, 153));
		lblStockMarket.setFont(new Font("Dubai", Font.PLAIN, 32));
		lblStockMarket.setBackground(Color.WHITE);
		lblStockMarket.setBounds(146, 11, 240, 61);
		contentPane.add(lblStockMarket);
		
	
		

		
		btnNewButton_3.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String stockSymbol = stockCode.getText();
				
				double pricePerShare = 0.0; //Placeholder

				try {
					pricePerShare = getStockPrice(stockSymbol);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				if(pricePerShare != -1) {
					 JOptionPane.showMessageDialog(null, "The price of "+stockSymbol+" is at: "+pricePerShare);
			   }
			}
		});
	

	}



public static double getStockPrice(String symbol) throws Exception {
	
    String apiKey = "FCLLB1H0VO49DDCG";
    String urlString = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol="
            + symbol + "&apikey=" + apiKey;
    try {
    URI uri = URI.create(urlString);
    URL url = uri.toURL();

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    

    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String inputLine;
    StringBuilder response = new StringBuilder();
    while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
    }
    in.close();
 
    JSONObject obj = new JSONObject(response.toString());
   
    
    JSONObject globalQuote = obj.getJSONObject("Global Quote");
    String priceStr = globalQuote.getString("05. price");
    
    double price = Double.parseDouble(priceStr);
    return price;
    
    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, "Error, please enter a valid symbol ", "Invalid Symbol", JOptionPane.ERROR_MESSAGE);
        return -1; 
    }
  }
}
