package gui;

import controller.MainController;

import javax.swing.*;
import java.awt.*;

public class TeacherWindow extends JFrame {
    private final MainController controller;

    // Lärare-kontroller
    private final JTextField tfStudentPnr = new JTextField(14);
    private final JTextField tfCourse = new JTextField(10);

    private final JButton btnAdd = new JButton("Lägg till åt elev");
    private final JButton btnRemove = new JButton("Ta bort åt elev");
    private final JButton btnSchedule = new JButton("Visa mitt schema (2025)");
    private final JButton btnViewStudentCourses = new JButton("Visa elevens kurser");

    private final JButton btnMarkPassed = new JButton("Avklarad");
    private final JButton btnMarkFailed = new JButton("Misslyckad");
    private final JButton btnClearResult = new JButton("Rensa");

    private final JLabel lblTotal = new JLabel("Totala poäng: 0");

    // Visning
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> lstView = new JList<>(listModel);
    private final JTextArea taLog = new JTextArea(5, 48);

    public TeacherWindow(MainController controller) {
        super("Studox – Lärare");
        this.controller = controller;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        var content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        String who = controller.getCurrentNameAndPnr().orElse(controller.getCurrentUsername());
        JLabel lbl = new JLabel("Inloggad (Lärare): " + who);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        content.add(lbl, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(buildTeacherPanel(), BorderLayout.NORTH);

        var listScroll = new JScrollPane(lstView);
        listScroll.setPreferredSize(new Dimension(560, 220));
        center.add(listScroll, BorderLayout.CENTER);

        content.add(center, BorderLayout.CENTER);

        taLog.setEditable(false);
        content.add(new JScrollPane(taLog), BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);

        // Actions
        btnAdd.addActionListener(e -> {
            String pnr = tfStudentPnr.getText().trim();
            String code = tfCourse.getText().trim();
            if (pnr.isEmpty() || code.isEmpty()) { msg("Ange elev-PNR och kurskod."); return; }
            msg(controller.teacherAddCourseToStudent(pnr, code));
            refreshFor(pnr);
        });

        btnRemove.addActionListener(e -> {
            String pnr = tfStudentPnr.getText().trim();
            String code = tfCourse.getText().trim();
            if (pnr.isEmpty() || code.isEmpty()) { msg("Ange elev-PNR och kurskod."); return; }
            msg(controller.teacherRemoveCourseFromStudent(pnr, code));
            refreshFor(pnr);
        });

        btnSchedule.addActionListener(e -> {
            listModel.clear();
            for (var s : controller.teacherSchedule2025()) listModel.addElement(s);
            msg("Hämtade lärarschema (2025).");
        });

        btnViewStudentCourses.addActionListener(e -> {
            String pnr = tfStudentPnr.getText().trim();
            if (pnr.isEmpty()) { msg("Ange elevens personnummer."); return; }
            listStudentCoursesWithResult(pnr);
            updateTotal(pnr);
        });

        btnMarkPassed.addActionListener(e -> setResultForSelected("PASSED"));
        btnMarkFailed.addActionListener(e -> setResultForSelected("FAILED"));
        btnClearResult.addActionListener(e -> setResultForSelected(""));
    }

    private JComponent buildTeacherPanel() {
        var panel = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;

        gc.gridx = 0; gc.gridy = row; panel.add(new JLabel("Elev-PNR:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1; panel.add(tfStudentPnr, gc);

        gc.gridx = 0; gc.gridy = row; gc.fill = 0; gc.weightx = 0; panel.add(new JLabel("Kurskod:"), gc);
        gc.gridx = 1; gc.gridy = row++; gc.fill = GridBagConstraints.HORIZONTAL; panel.add(tfCourse, gc);

        var buttons1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons1.add(btnAdd);
        buttons1.add(btnRemove);
        buttons1.add(btnSchedule);
        buttons1.add(btnViewStudentCourses);
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2; panel.add(buttons1, gc);

        var buttons2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons2.add(btnMarkPassed);
        buttons2.add(btnMarkFailed);
        buttons2.add(btnClearResult);
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2; panel.add(buttons2, gc);

        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2; panel.add(lblTotal, gc);

        return panel;
    }

    private void listStudentCoursesWithResult(String pnr) {
        listModel.clear();
        for (var s : controller.teacherListStudentCoursesWithResult(pnr)) {
            listModel.addElement(s);
        }
        msg("Visar kurser för elev " + pnr + " (med resultat + poäng).");
    }

    private void updateTotal(String pnr) {
        double total = controller.getStudentTotalPointsByPnr(pnr);
        lblTotal.setText(String.format("Totala poäng: %.1f", total));
    }

    private void refreshFor(String pnr) {
        if (pnr == null || pnr.isBlank()) return;
        listStudentCoursesWithResult(pnr);
        updateTotal(pnr);
    }

    private void setResultForSelected(String result) {
        String pnr = tfStudentPnr.getText().trim();
        if (pnr.isEmpty()) { msg("Ange elevens personnummer."); return; }
        String code = getSelectedCourseCode();
        if (code.isEmpty()) { msg("Markera en kursrad eller skriv Kurskod."); return; }
        msg(controller.teacherSetCourseResult(pnr, code, result));
        refreshFor(pnr);
    }

    /** Försök plocka kurskod ur markerad rad (format "CODE – Name (Xp) [RESULT]"), fallback tfCourse. */
    private String getSelectedCourseCode() {
        String sel = lstView.getSelectedValue();
        if (sel != null) {
            int i = sel.indexOf(" – ");
            if (i > 0) return sel.substring(0, i).trim();
            String[] parts = sel.split("\\s+");
            if (parts.length > 0) return parts[0].trim();
        }
        return tfCourse.getText().trim();
    }

    private void msg(String m) { taLog.append(m + "\n"); }
}