import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class RegisterWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public RegisterWindow() {
        setTitle("Register");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel userLabel = new JLabel("New Username:");
        userLabel.setBounds(50, 40, 120, 30);
        add(userLabel);

        usernameField = new JTextField();
        usernameField.setBounds(180, 40, 150, 30);
        add(usernameField);

        JLabel passLabel = new JLabel("New Password:");
        passLabel.setBounds(50, 80, 120, 30);
        add(passLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(180, 80, 150, 30);
        add(passwordField);

        JButton registerButton = new JButton("Register");
        registerButton.setBounds(130, 140, 120, 30);
        add(registerButton);

        registerButton.addActionListener(e -> register());

        setVisible(true);
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
            new LoginWindow();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving user.");
        }
    }
}
