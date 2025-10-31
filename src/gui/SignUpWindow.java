package gui;

import javax.swing.*;
import java.awt.*;
import controller.MainController;

public class SignUpWindow extends JFrame {
    private final MainController controller;

    public SignUpWindow(MainController controller) {
        super("Create Account");
        this.controller = controller;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(360, 320);
        setLocationRelativeTo(null);

        var panel = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        var tfFirst = new JTextField();
        var tfLast  = new JTextField();
        var tfPnr   = new JTextField();
        var pfPass  = new JPasswordField();
        var cbRole  = new JComboBox<>(new String[] {"STUDENT","TEACHER"});

        int row = 0;
        gc.gridx=0; gc.gridy=row; panel.add(new JLabel("Förnamn:"), gc);
        gc.gridx=1; panel.add(tfFirst, gc);

        gc.gridx=0; gc.gridy=++row; panel.add(new JLabel("Efternamn:"), gc);
        gc.gridx=1; panel.add(tfLast, gc);

        gc.gridx=0; gc.gridy=++row; panel.add(new JLabel("Personnummer:"), gc);
        gc.gridx=1; panel.add(tfPnr, gc);

        gc.gridx=0; gc.gridy=++row; panel.add(new JLabel("Lösenord:"), gc);
        gc.gridx=1; panel.add(pfPass, gc);

        gc.gridx=0; gc.gridy=++row; panel.add(new JLabel("Roll:"), gc);
        gc.gridx=1; panel.add(cbRole, gc);

        var btnCreate = new JButton("Skapa konto");
        gc.gridx=0; gc.gridy=++row; gc.gridwidth=2; gc.fill=GridBagConstraints.NONE;
        panel.add(btnCreate, gc);

        btnCreate.addActionListener(e -> {
            var ok = controller.registerUser(
                tfFirst.getText().trim(),
                tfLast.getText().trim(),
                tfPnr.getText().trim(),
                new String(pfPass.getPassword()),
                (String) cbRole.getSelectedItem()
            );
            JOptionPane.showMessageDialog(this, ok ? "Konto skapat!" : "Kunde inte skapa konto.");
            if (ok) dispose();
        });

        setContentPane(panel);
    }
}