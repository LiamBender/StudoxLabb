package gui;

import controller.MainController;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    public MainWindow(MainController controller) {
        super("Studox");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Minimal splash/label ifall fönstret syns en blinkning
        var lbl = new JLabel("Öppnar vy ...", SwingConstants.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        lbl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        setContentPane(lbl);
        setSize(320, 120);
        setLocationRelativeTo(null);

        // Öppna rätt fönster direkt
        SwingUtilities.invokeLater(() -> {
            JFrame next = controller.isTeacher()
                    ? new TeacherWindow(controller)
                    : new StudentWindow(controller);
            next.setVisible(true);
            dispose(); // stäng routern
        });
    }
}