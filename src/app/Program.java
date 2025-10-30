package app;

import javax.swing.SwingUtilities;
import gui.LoginWindow;

public class Program {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }
}