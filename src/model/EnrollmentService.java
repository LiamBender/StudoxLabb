package model;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class EnrollmentService {
    private final StudentDAO studentDAO = new StudentDAO();
    private final CourseDAO courseDAO   = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    // ---- NY hjälpfunktion: finns kurskod i kurstillfallen för visst år? ----
    private boolean courseOfferedInYear(String courseCode, int year) throws SQLException {
        String sql = "SELECT 1 FROM kurstillfallen WHERE kurskod = ? AND artal = ? LIMIT 1";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---- Registrera på kurs, endast om år = 2025 är möjligt ----
    public String joinCourse(String pnr, String courseCode) {
        try {
            // skapa student om saknas (för demo)
            Student s = studentDAO.createIfMissing(pnr, null);

            Optional<Course> cOpt = courseDAO.findByCode(courseCode);
            if (cOpt.isEmpty()) return "Kurs hittas inte: " + courseCode;

            // *** Begränsning: endast kurser som ges 2025 ***
            if (!courseOfferedInYear(courseCode, 2025)) {
                return "Kursen " + courseCode + " kan inte väljas – inget kurstillfälle år 2025.";
            }

            boolean ok = enrollmentDAO.joinCourse(s.getId(), cOpt.get().getId());
            return ok ? "Registrerad på kursen." : "Redan registrerad.";
        } catch (SQLException e) {
            return "Fel vid registrering: " + e.getMessage();
        }
    }

    public String leaveCourse(String pnr, String courseCode) {
        try {
            var sOpt = studentDAO.findByPnr(pnr);
            if (sOpt.isEmpty()) return "Student finns inte.";
            var cOpt = courseDAO.findByCode(courseCode);
            if (cOpt.isEmpty()) return "Kurs finns inte.";

            boolean ok = enrollmentDAO.leaveCourse(sOpt.get().getId(), cOpt.get().getId());
            return ok ? "Avregistrerad." : "Kunde inte avregistrera (ej registrerad eller redan passerad).";
        } catch (SQLException e) {
            return "Fel vid avregistrering: " + e.getMessage();
        }
    }

    public List<String> listCourses(String pnr) throws SQLException {
        var sOpt = studentDAO.findByPnr(pnr);
        if (sOpt.isEmpty()) return List.of();
        return enrollmentDAO.listCoursesForStudent(sOpt.get().getId());
    }
}