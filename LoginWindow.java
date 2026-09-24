import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private HashMap<String, String> credentials;

    public LoginWindow() {
        setTitle("Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(50, 40, 100, 30);
        add(userLabel);

        usernameField = new JTextField();
        usernameField.setBounds(150, 40, 180, 30);
        add(usernameField);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(50, 80, 100, 30);
        add(passLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(150, 80, 180, 30);
        add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(50, 130, 120, 30);
        add(loginButton);

        JButton registerButton = new JButton("Register");
        registerButton.setBounds(210, 130, 120, 30);
        add(registerButton);

        loadCredentials();

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> {
            dispose();
            new RegisterWindow(); 
        });

        setVisible(true);
    }

    private void loadCredentials() {
        credentials = new HashMap<>();
        File file = new File("users.txt");
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    credentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading users.");
        }
    }

    private void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (credentials.containsKey(username) && credentials.get(username).equals(password)) {
            JOptionPane.showMessageDialog(this, "Login successful!");
            dispose(); // Κλείνουμε το login παράθυρο
        	Portfolio portfolio = new Portfolio();
    		StockMarketGUI frame = new StockMarketGUI(portfolio);
    		frame.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials!");
        }
    }
}
