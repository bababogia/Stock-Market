import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;

public class LoginWindow extends JFrame {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private HashMap<String, String> credentials;

    public LoginWindow() {
        setTitle("Login");
        setSize(440, 390);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(AppTheme.BACKGROUND);
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(26, 28, 26, 28)));

        JLabel title = new JLabel("Welcome back");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.NAVY);
        JLabel subtitle = new JLabel("Sign in to continue to your trading desk.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(AppTheme.MUTED);
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);

        usernameField = new JTextField();
        AppTheme.styleField(usernameField);
        passwordField = new JPasswordField();
        AppTheme.styleField(passwordField);
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);
        addField(fields, "Username", usernameField);
        addField(fields, "Password", passwordField);

        JButton loginButton = new JButton("Sign in");
        AppTheme.styleButton(loginButton, AppTheme.ACCENT);
        JButton registerButton = new JButton("Create account");
        AppTheme.styleButton(registerButton, AppTheme.SURFACE);
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.add(loginButton);
        actions.add(registerButton);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(AppTheme.BACKGROUND);
        outer.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        outer.add(card);
        add(outer);

        loadCredentials();

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> {
            dispose();
            RegisterWindow registerWindow = new RegisterWindow();
            registerWindow.setVisible(true);
        });

    }

    private void addField(JPanel fields, String text, JTextField field) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(AppTheme.TEXT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setAlignmentX(LEFT_ALIGNMENT);
        fields.add(label);
        fields.add(Box.createVerticalStrut(6));
        fields.add(field);
        fields.add(Box.createVerticalStrut(14));
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
            dispose();
            Portfolio portfolio = new Portfolio();
            StockMarketGUI frame = new StockMarketGUI(portfolio);
            frame.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials!");
        }
    }
}
