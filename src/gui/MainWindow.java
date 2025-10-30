package gui;

import controller.MainController;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainWindow extends JFrame {
    // --- Elev (visas bara i elev-läge) ---
    private final JTextField tfPnr = new JTextField(14);
    private final JTextField tfCourse = new JTextField(10);
    private final JButton btnJoin = new JButton("Gå med i kurs");
    private final JButton btnLeave = new JButton("Gå ur kurs");
    private final JButton btnList = new JButton("Visa mina kurser");
    private final JButton btnSchedule = new JButton("Visa schema (2025)");

    // --- Lärare (visas bara i lärar-läge) ---
    private final JTextField tfStudentPnrForTeacher = new JTextField(14);
    private final JButton btnTeacherAdd = new JButton("Lägg till åt elev");
    private final JButton btnTeacherRemove = new JButton("Ta bort åt elev");
    private final JButton btnTeacherSchedule = new JButton("Visa lärarschema (2025)");
    private final JButton btnTeacherViewStudentCourses = new JButton("Visa elevens kurser");

    // --- Visning ---
    private final JTextArea taLog = new JTextArea(5, 48);
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> lstView = new JList<>(listModel);

    private final MainController controller;

    public MainWindow(MainController controller) {
        super("Studox – Huvudfönster");
        this.controller = controller;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        String username = controller.getCurrentUsername();
        String roleText = controller.isTeacher() ? "(Lärare)" : "(Elev)";
        JLabel lblRole = new JLabel("Inloggad som: " + username + " " + roleText);
        lblRole.setFont(lblRole.getFont().deriveFont(Font.BOLD));
        content.add(lblRole, BorderLayout.NORTH);

        // ===== Center: paneler beroende på roll =====
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(buildRolePanel(), BorderLayout.NORTH);

        var listScroll = new JScrollPane(lstView);
        listScroll.setPreferredSize(new Dimension(560, 180));
        center.add(listScroll, BorderLayout.CENTER);

        content.add(center, BorderLayout.CENTER);

        taLog.setEditable(false);
        content.add(new JScrollPane(taLog), BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);

        // ===== Actions =====
        btnJoin.addActionListener(e -> {
            String msg = controller.join(getLockedStudentPnr(), tfCourse.getText().trim());
            taLog.append(msg + "\n");
            doListForStudent();
        });

        btnLeave.addActionListener(e -> {
            String msg = controller.leave(getLockedStudentPnr(), tfCourse.getText().trim());
            taLog.append(msg + "\n");
            doListForStudent();
        });

        btnList.addActionListener(e -> doListForStudent());
        btnSchedule.addActionListener(e -> showStudentSchedule2025());

        btnTeacherAdd.addActionListener(e -> {
            String pnr = tfStudentPnrForTeacher.getText().trim();
            String code = tfCourse.getText().trim();
            taLog.append(controller.teacherAddCourseToStudent(pnr, code) + "\n");
        });

        btnTeacherRemove.addActionListener(e -> {
            String pnr = tfStudentPnrForTeacher.getText().trim();
            String code = tfCourse.getText().trim();
            taLog.append(controller.teacherRemoveCourseFromStudent(pnr, code) + "\n");
        });

        btnTeacherSchedule.addActionListener(e -> {
            listModel.clear();
            for (var s : controller.teacherSchedule2025()) listModel.addElement(s);
            taLog.append("Hämtade lärarschema (2025).\n");
        });

        btnTeacherViewStudentCourses.addActionListener(e -> {
            String pnr = tfStudentPnrForTeacher.getText().trim();
            listModel.clear();
            for (var s : controller.teacherListStudentCourses(pnr)) {
                listModel.addElement(s);
            }
            taLog.append("Visar kurser för elev " + pnr + ".\n");
        });
    }

    /** Bygger roll-specifik del av UI:t. */
    private JComponent buildRolePanel() {
        boolean student = controller.isStudent();
        var panel = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;

        if (student) {
            // Hämta inloggat PNR en gång och lås fältet
            tfPnr.setText(getLockedStudentPnr());
            tfPnr.setEditable(false);

            gc.gridx = 0; gc.gridy = row; panel.add(new JLabel("Mitt PNR:"), gc);
            gc.gridx = 1; gc.gridy = row++; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1; panel.add(tfPnr, gc);

            gc.gridx = 0; gc.gridy = row; gc.fill = 0; gc.weightx = 0; panel.add(new JLabel("Kurskod:"), gc);
            gc.gridx = 1; gc.gridy = row++; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1; panel.add(tfCourse, gc);

            var buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttons.add(btnJoin);
            buttons.add(btnLeave);
            buttons.add(btnList);
            buttons.add(btnSchedule);
            gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2; gc.fill = 0; panel.add(buttons, gc);
        } else { // LÄRARE
            gc.gridx = 0; gc.gridy = row; panel.add(new JLabel("Elev-PNR:"), gc);
            gc.gridx = 1; gc.gridy = row++; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1; panel.add(tfStudentPnrForTeacher, gc);

            gc.gridx = 0; gc.gridy = row; gc.fill = 0; gc.weightx = 0; panel.add(new JLabel("Kurskod:"), gc);
            gc.gridx = 1; gc.gridy = row++; gc.fill = GridBagConstraints.HORIZONTAL; panel.add(tfCourse, gc);

            // säkerställ att läraren kan skriva i kurskods-fältet
            tfCourse.setEnabled(true);
            tfCourse.setEditable(true);

            var buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttons.add(btnTeacherAdd);
            buttons.add(btnTeacherRemove);
            buttons.add(btnTeacherSchedule);
            buttons.add(btnTeacherViewStudentCourses);
            gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2; panel.add(buttons, gc);

            // DÖLJ/avaktivera elev-knappar helt i lärarläge
            setStudentControlsEnabled(false);
        }
        return panel;
    }

    private void setStudentControlsEnabled(boolean enabled) {
        tfPnr.setEnabled(enabled);
        // tfCourse lämnas alltid aktivt (används av båda roller)
        btnJoin.setEnabled(enabled);
        btnLeave.setEnabled(enabled);
        btnList.setEnabled(enabled);
        btnSchedule.setEnabled(enabled);
    }

    // ==== Student-funktioner ====
    private void doListForStudent() {
        listModel.clear();
        try {
            List<String> rows = controller.list(getLockedStudentPnr());
            rows.forEach(listModel::addElement);
            taLog.append("Listade kurser.\n");
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

    /** Hämtar PNR för inloggad student och ser till att elevflöden alltid använder det. */
    private String getLockedStudentPnr() {
        return controller.getCurrentStudentPnr().orElse(tfPnr.getText().trim());
    }
}