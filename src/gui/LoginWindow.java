package gui;

import controller.MainController;

import javax.swing.*;
import java.awt.*;

public class LoginWindow extends JFrame {
    private final JTextField tfUser = new JTextField(14);
    private final JPasswordField pfPass = new JPasswordField(14);
    private final JButton btnLogin = new JButton("Logga in");
    private final JTextArea taLog = new JTextArea(3, 32);

    private final MainController controller = new MainController();

    public LoginWindow() {
        super("Studox – Inloggning");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var p = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gc.gridx=0; gc.gridy=row; p.add(new JLabel("Användarnamn:"), gc);
        gc.gridx=1; gc.gridy=row++; gc.fill=GridBagConstraints.HORIZONTAL; gc.weightx=1; p.add(tfUser, gc);

        gc.gridx=0; gc.gridy=row; gc.fill=0; gc.weightx=0; p.add(new JLabel("Lösenord:"), gc);
        gc.gridx=1; gc.gridy=row++; gc.fill=GridBagConstraints.HORIZONTAL; p.add(pfPass, gc);

        gc.gridx=1; gc.gridy=row++; gc.fill=0; gc.weightx=0;
        p.add(btnLogin, gc);

        taLog.setEditable(false);
        var logScroll = new JScrollPane(taLog);
        gc.gridx=0; gc.gridy=row; gc.gridwidth=2; gc.fill=GridBagConstraints.HORIZONTAL;
        p.add(logScroll, gc);

        setContentPane(p);
        pack();
        setLocationRelativeTo(null);

        btnLogin.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(btnLogin);
    }

    private void doLogin() {
        var u = tfUser.getText().trim();
        var p = new String(pfPass.getPassword());

        boolean ok = controller.login(u, p);
        if (!ok) {
            taLog.append("Fel användarnamn eller lösenord.\n");
            return;
        }
        taLog.append("Inloggad som: " + controller.getCurrentUsername()
                + (controller.isTeacher()? " (Lärare)\n" : " (Elev)\n"));

        var main = new MainWindow(controller);
        main.setVisible(true);
        dispose();
    }
}