import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

final class AppTheme {

    static final Color BACKGROUND = new Color(243, 246, 251);
    static final Color SURFACE = Color.WHITE;
    static final Color NAVY = new Color(23, 38, 59);
    static final Color TEXT = new Color(39, 52, 70);
    static final Color MUTED = new Color(108, 123, 143);
    static final Color ACCENT = new Color(39, 105, 210);
    static final Color SUCCESS = new Color(22, 139, 102);
    static final Color BORDER = new Color(222, 229, 238);
    static final Color INPUT = new Color(249, 251, 253);

    private AppTheme() {}

    static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            throw new IllegalStateException("Unable to initialize the system look and feel.", ex);
        }
        UIManager.put("TabbedPane.selected", SURFACE);
        UIManager.put("TabbedPane.contentAreaColor", BACKGROUND);
        UIManager.put("TabbedPane.focus", BORDER);
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.PLAIN, 13));
    }

    static void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setBackground(INPUT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    static void styleButton(JButton button, Color background) {
        Color normal = background;
        Color hover = background.equals(SURFACE) ? INPUT : background.brighter();
        Color pressed = background.darker();

        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(normal.equals(ACCENT) || normal.equals(SUCCESS)
                ? Color.WHITE : TEXT);
        button.setBackground(normal);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normal.equals(ACCENT) || normal.equals(SUCCESS)
                        ? normal : BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        ButtonModel model = button.getModel();
        model.addChangeListener(event -> {
            if (button.isEnabled() && model.isPressed() && model.isArmed()) {
                button.setBackground(pressed);
            } else if (button.isEnabled() && model.isRollover()) {
                button.setBackground(hover);
            } else {
                button.setBackground(normal);
            }
        });
    }

    static void styleTextArea(JTextArea area) {
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setForeground(TEXT);
        area.setBackground(SURFACE);
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }
}
