package gui;

import controller.MainController;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StudentWindow extends JFrame {
    private final MainController controller;

    // Studentkontroller
    private final JTextField tfPnr = new JTextField(14);
    private final JTextField tfCourse = new JTextField(10);
    private final JButton btnJoin = new JButton("Gå med i kurs");
    private final JButton btnLeave = new JButton("Gå ur kurs");
    private final JButton btnList = new JButton("Visa mina kurser");
    private final JButton btnSchedule = new JButton("Visa schema (2025)");
    
 // === Sök via personnummer (elev/lärare) ===
    private final JTextField tfLookupPnr = new JTextField(14);
    private final JButton btnLookup = new JButton("Sök via personnummer");
    private final DefaultListModel<String> lookupModel = new DefaultListModel<>();
    private final JList<String> lstLookup = new JList<>(lookupModel);


    // Visning
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> lstView = new JList<>(listModel);
    private final JTextArea taLog = new JTextArea(5, 48);

    // NYTT: Min totalsumma
    private final JLabel lblMyTotal = new JLabel("Mina totala poäng: 0.0");

    public StudentWindow(MainController controller) {
        super("Studox – Student");
        this.controller = controller;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        var content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        String who = controller.getCurrentNameAndPnr().orElse(controller.getCurrentUsername());
        JLabel lbl = new JLabel("Inloggad (Elev): " + who);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        content.add(lbl, BorderLayout.NORTH);

        // Center
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(buildStudentPanel(), BorderLayout.NORTH);

        var listScroll = new JScrollPane(lstView);
        listScroll.setPreferredSize(new Dimension(560, 220));
        center.add(listScroll, BorderLayout.CENTER);

        // NYTT: totalsumma under listan
        lblMyTotal.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        center.add(lblMyTotal, BorderLayout.SOUTH);

        content.add(center, BorderLayout.CENTER);

        // Logg
        taLog.setEditable(false);
        content.add(new JScrollPane(taLog), BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);

        // Actions
        btnJoin.addActionListener(e -> {
            String msg = controller.join(getLockedStudentPnr(), tfCourse.getText().trim());
            taLog.append(msg + "\n");
            doListForStudent();
            updateMyTotalPoints();      // NYTT
        });
        btnLeave.addActionListener(e -> {
            String msg = controller.leave(getLockedStudentPnr(), tfCourse.getText().trim());
            taLog.append(msg + "\n");
            doListForStudent();
            updateMyTotalPoints();      // NYTT
        });
        btnList.addActionListener(e -> {
            doListForStudent();
            updateMyTotalPoints();      // NYTT
        });
        btnSchedule.addActionListener(e -> {
            showStudentSchedule2025();
            updateMyTotalPoints();      // NYTT
        });

        // Förifyll & lås PNR
        tfPnr.setText(getLockedStudentPnr());
        tfPnr.setEditable(false);

        // Start: visa mina kurser + totalsumma
        doListForStudent();
        updateMyTotalPoints();          // NYTT
        
        
     // Panel: Sök via personnummer
        var pnlLookup = new JPanel(new BorderLayout(8,8));
        var north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        north.add(new JLabel("Personnummer:"));
        north.add(tfLookupPnr);
        north.add(btnLookup);
        pnlLookup.add(north, BorderLayout.NORTH);

        lstLookup.setVisibleRowCount(8);
        pnlLookup.add(new JScrollPane(lstLookup), BorderLayout.CENTER);
        pnlLookup.setBorder(BorderFactory.createTitledBorder("Sök: elev eller lärare"));

        /* Lägg pnlLookup i din befintliga container, t.ex.
           main.add(pnlLookup, BorderLayout.SOUTH);
           eller om du har Grid/Box, placera den där det passar logiskt.
        */
        
        btnLookup.addActionListener(e -> {
            String pnr = tfLookupPnr.getText().trim();
            lookupModel.clear();
            controller.listCoursesByPersonnummer(pnr).forEach(lookupModel::addElement);
        });


    }

    private JComponent buildStudentPanel() {
        var panel = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gc.gridx = 0; gc.gridy = row; panel.add(new JLabel("Mitt PNR:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1; panel.add(tfPnr, gc);

        gc.gridx = 0; gc.gridy = row; gc.fill = 0; gc.weightx = 0; panel.add(new JLabel("Kurskod:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1; panel.add(tfCourse, gc);

        var buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(btnJoin);
        buttons.add(btnLeave);
        buttons.add(btnList);
        buttons.add(btnSchedule);

        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2; gc.fill = 0;
        panel.add(buttons, gc);

        return panel;
    }

    private void doListForStudent() {
        listModel.clear();
        try {
            java.util.List<String> rows = controller.studentListMyCoursesWithResult();
            rows.forEach(listModel::addElement);
            taLog.append("Listade kurser (med poäng + resultat).\n");
        } catch (Exception ex) {
            taLog.append("Fel vid listning: " + ex.getMessage() + "\n");
        }
    }

    private void showStudentSchedule2025() {
        listModel.clear();
        var schema = controller.getSchedule(getLockedStudentPnr());
        schema.forEach(listModel::addElement);
        taLog.append(schema.isEmpty() ? "Inget schema (2025) hittades.\n" : "Schema (2025) hämtat.\n");
    }

    private String getLockedStudentPnr() {
        return controller.getCurrentStudentPnr().orElse(tfPnr.getText().trim());
    }

    // NYTT: uppdatera etiketten med mina totala poäng (1 decimal)
    private void updateMyTotalPoints() {
        double total = controller.getMyTotalPoints();
        lblMyTotal.setText(String.format("Mina totala poäng: %.1f", total));
    }
}