import javax.swing.*;
import java.awt.*;
import java.io.*;

public class RegisterWindow extends JFrame {
    private final JTextField usernameField;
    private final JPasswordField passwordField;

    public RegisterWindow() {
        setTitle("Register");
        setSize(440, 390);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(AppTheme.BACKGROUND);
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(AppTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(26, 28, 26, 28)));

        JLabel title = new JLabel("Create your account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppTheme.NAVY);
        JLabel subtitle = new JLabel("Register to access your trading desk.");
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

        JButton registerButton = new JButton("Create account");
        AppTheme.styleButton(registerButton, AppTheme.ACCENT);
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(registerButton, BorderLayout.CENTER);

        card.add(heading, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(AppTheme.BACKGROUND);
        outer.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        outer.add(card);
        add(outer);

        registerButton.addActionListener(e -> register());
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

    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("users.txt", true))) {
            bw.write(username + "," + password);
            bw.newLine();
            JOptionPane.showMessageDialog(this, "Registration successful!");
            dispose();
            LoginWindow loginWindow = new LoginWindow();
            loginWindow.setVisible(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving user.");
        }
    }
}
