package controller;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // ===== Inloggat tillstånd =====
    public enum Role { STUDENT, TEACHER }
    private Role role;
    private String username;
    private Integer currentStudentId; // om STUDENT
    private Integer currentTeacherId; // om TEACHER

    // ===== DB-anslutning =====
    private static final String URL  = "jdbc:postgresql://localhost:5432/Studox";
    private static final String USER = "postgres";
    private static final String PASS = "postgres"; // ändra vid behov

    private Connection getConn() throws SQLException { return DriverManager.getConnection(URL, USER, PASS); }

    // ===== Inloggning =====
    public boolean login(String user, String pass) {
        String sql = """
            SELECT u.username, u.role, u.student_id, u.teacher_id
            FROM users u
            WHERE u.username = ? AND u.password = ?
            """;
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                this.username = rs.getString("username");
                this.role = Role.valueOf(rs.getString("role"));
                this.currentStudentId = (Integer) rs.getObject("student_id");
                this.currentTeacherId = (Integer) rs.getObject("teacher_id");
                return true;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Login fel: " + ex.getMessage(), ex);
        }
    }

    public boolean isLoggedIn() { return role != null; }
    public boolean isStudent()   { return role == Role.STUDENT; }
    public boolean isTeacher()   { return role == Role.TEACHER; }
    public String  who()         { return username != null ? username : ""; }

    // ===== Hjälpare: slå upp id:n =====

    private Integer findStudentIdByPnr(Connection con, String pnr) throws SQLException {
        String sql = "SELECT id FROM students WHERE personnummer = ?";
        try (var ps = con.prepareStatement(sql)) { ps.setString(1, pnr); try (var rs = ps.executeQuery()) { return rs.next()? rs.getInt(1) : null; } }
    }

    private Integer findCourseIdByCode(Connection con, String code) throws SQLException {
        String sql = "SELECT id FROM courses WHERE code = ?";
        try (var ps = con.prepareStatement(sql)) { ps.setString(1, code); try (var rs = ps.executeQuery()) { return rs.next()? rs.getInt(1) : null; } }
    }

    private boolean hasKurstillfalle2025(Connection con, String kurskod) throws SQLException {
        String sql = "SELECT 1 FROM kurstillfallen WHERE kurskod = ? AND artal = 2025 LIMIT 1";
        try (var ps = con.prepareStatement(sql)) { ps.setString(1, kurskod); try (var rs = ps.executeQuery()) { return rs.next(); } }
    }

    // ===== Elev: gå med/ur/lista/schedule =====

    public String join(String pnr, String kurskod) {
        if (!isStudent()) return "Du måste vara inloggad som student för att gå med i kurs.";
        try (var con = getConn()) {
            Integer sid = findStudentIdByPnr(con, pnr);
            if (sid == null) return "Student med pnr finns inte.";
            if (!sid.equals(currentStudentId)) return "Du kan bara ändra dina egna kurser.";

            if (!hasKurstillfalle2025(con, kurskod))
                return "Inget kurstillfälle år 2025 för " + kurskod + ".";

            Integer cid = findCourseIdByCode(con, kurskod);
            if (cid == null) return "Kurskoden finns inte i courses.";

            String sql = "INSERT INTO course_enrollments(student_id, course_id, status) VALUES(?, ?, 'REGISTERED') ON CONFLICT DO NOTHING";
            try (var ps = con.prepareStatement(sql)) {
                ps.setInt(1, sid);
                ps.setInt(2, cid);
                int n = ps.executeUpdate();
                return (n == 0) ? "Redan registrerad på " + kurskod + "." : "Registrerad på " + kurskod + ".";
            }
        } catch (SQLException ex) {
            return "Fel vid anmälan: " + ex.getMessage();
        }
    }

    public String leave(String pnr, String kurskod) {
        if (!isStudent()) return "Du måste vara inloggad som student för att lämna kurs.";
        try (var con = getConn()) {
            Integer sid = findStudentIdByPnr(con, pnr);
            if (sid == null) return "Student med pnr finns inte.";
            if (!sid.equals(currentStudentId)) return "Du kan bara ändra dina egna kurser.";

            Integer cid = findCourseIdByCode(con, kurskod);
            if (cid == null) return "Kurskoden finns inte i courses.";

            String sql = "DELETE FROM course_enrollments WHERE student_id=? AND course_id=?";
            try (var ps = con.prepareStatement(sql)) {
                ps.setInt(1, sid); ps.setInt(2, cid);
                int n = ps.executeUpdate();
                return (n == 0) ? "Du är inte registrerad på " + kurskod + "." : "Avregistrerad från " + kurskod + ".";
            }
        } catch (SQLException ex) {
            return "Fel vid avanmälan: " + ex.getMessage();
        }
    }

    public List<String> list(String pnr) throws Exception {
        try (var con = getConn()) {
            Integer sid = findStudentIdByPnr(con, pnr);
            if (sid == null) throw new Exception("Student finns inte.");
            String sql = """
                SELECT c.code, c.name
                FROM course_enrollments e
                JOIN courses c ON c.id = e.course_id
                WHERE e.student_id = ?
                ORDER BY c.code
                """;
            try (var ps = con.prepareStatement(sql)) {
                ps.setInt(1, sid);
                try (var rs = ps.executeQuery()) {
                    List<String> out = new ArrayList<>();
                    while (rs.next()) out.add(rs.getString("code") + " – " + rs.getString("name"));
                    return out;
                }
            }
        }
    }

    public List<String> getSchedule(String pnr) {
        String sql = """
            SELECT k.kurskod, k.kursnamn, k.vecka, k.artal
            FROM course_enrollments e
            JOIN courses c       ON c.id = e.course_id
            JOIN kurstillfallen k ON k.kurskod = c.code
            WHERE e.student_id = (SELECT id FROM students WHERE personnummer = ?)
              AND k.artal = 2025
            ORDER BY k.kurskod, k.vecka
            """;
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (var rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add("%s v%s (%s)".formatted(rs.getString("kurskod"),
                                                    rs.getInt("vecka"),
                                                    rs.getString("kursnamn")));
                }
                return out;
            }
        } catch (SQLException ex) {
            List<String> err = new ArrayList<>();
            err.add("Fel vid hämtning: " + ex.getMessage());
            return err;
        }
    }

    // ===== Lärar-vyer =====

    public String teacherAddCourseToStudent(String elevPnr, String kurskod) {
        if (!isTeacher()) return "Endast lärare får göra detta.";
        try (var con = getConn()) {
            Integer sid = findStudentIdByPnr(con, elevPnr);
            if (sid == null) return "Elev finns inte.";
            if (!hasKurstillfalle2025(con, kurskod)) return "Inget 2025-kurstillfälle för " + kurskod + ".";
            Integer cid = findCourseIdByCode(con, kurskod);
            if (cid == null) return "Kurskoden finns inte.";
            String sql = "INSERT INTO course_enrollments(student_id, course_id, status) VALUES(?, ?, 'REGISTERED') ON CONFLICT DO NOTHING";
            try (var ps = con.prepareStatement(sql)) { ps.setInt(1, sid); ps.setInt(2, cid); int n=ps.executeUpdate(); }
            return "Lagt till " + kurskod + " åt eleven.";
        } catch (SQLException ex) {
            return "Fel (lägga till åt elev): " + ex.getMessage();
        }
    }

    public String teacherRemoveCourseFromStudent(String elevPnr, String kurskod) {
        if (!isTeacher()) return "Endast lärare får göra detta.";
        try (var con = getConn()) {
            Integer sid = findStudentIdByPnr(con, elevPnr);
            if (sid == null) return "Elev finns inte.";
            Integer cid = findCourseIdByCode(con, kurskod);
            if (cid == null) return "Kurskoden finns inte.";
            try (var ps = con.prepareStatement("DELETE FROM course_enrollments WHERE student_id=? AND course_id=?")) {
                ps.setInt(1, sid); ps.setInt(2, cid); ps.executeUpdate();
            }
            return "Tog bort " + kurskod + " åt eleven.";
        } catch (SQLException ex) {
            return "Fel (ta bort åt elev): " + ex.getMessage();
        }
    }

    public List<String> teacherSchedule2025() {
        // bygger på att course_teachers (course_id, teacher_id) innehåller kopplingar
        String sql = """
            SELECT DISTINCT k.kurskod, k.kursnamn, k.vecka
            FROM course_teachers ct
            JOIN courses c       ON c.id = ct.course_id
            JOIN kurstillfallen k ON k.kurskod = c.code
            WHERE ct.teacher_id = ? AND k.artal = 2025
            ORDER BY k.kurskod, k.vecka
            """;
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentTeacherId);
            try (var rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add("%s v%s (%s)".formatted(rs.getString("kurskod"),
                                                    rs.getInt("vecka"),
                                                    rs.getString("kursnamn")));
                }
                return out;
            }
        } catch (SQLException ex) {
            return List.of("Fel vid lärarschema: " + ex.getMessage());
        }
    }

    public List<String> teacherListStudentCourses(String elevPnr) {
        String sql = """
            SELECT c.code, c.name
            FROM course_enrollments e
            JOIN students s ON s.id = e.student_id
            JOIN courses  c ON c.id = e.course_id
            WHERE s.personnummer = ?
            ORDER BY c.code
            """;
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, elevPnr);
            try (var rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString("code")+" – "+rs.getString("name"));
                return out;
            }
        } catch (SQLException ex) {
            return List.of("Fel vid hämtning: " + ex.getMessage());
        }
    }
    public String getCurrentUsername() {
        return (username != null) ? username : "";
    }

    public java.util.Optional<String> getCurrentStudentPnr() {
        if (!isStudent() || currentStudentId == null) return java.util.Optional.empty();
        String sql = "SELECT personnummer FROM students WHERE id = ?";
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentStudentId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return java.util.Optional.of(rs.getString(1));
            }
        } catch (Exception ignored) {}
        return java.util.Optional.empty();
    }
}