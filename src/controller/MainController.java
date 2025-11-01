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

    // ====== Lärare: sätt resultat för en elevs kurs ======
    public String teacherSetCourseResult(String elevPnr, String kurskod, String result) {
        if (!isTeacher()) return "Endast lärare får sätta resultat.";
        try (Connection con = getConn()) {
            Integer sid = findStudentIdByPnr(con, elevPnr);
            if (sid == null) return "Elev finns inte.";

            Integer cid = findCourseIdByCode(con, kurskod);
            if (cid == null) return "Kurs finns inte.";

            String sql = "UPDATE course_enrollments " +
                         "SET result = ? " +
                         "WHERE student_id = ? AND course_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (result == null || result.isBlank()) {
                    ps.setNull(1, Types.VARCHAR);
                } else {
                    ps.setString(1, result.trim().toUpperCase()); // PASSED/FAILED
                }
                ps.setInt(2, sid);
                ps.setInt(3, cid);
                int rows = ps.executeUpdate();
                if (rows == 0) return "Hittade ingen matchande kurs för eleven.";
            }

            return "Resultat uppdaterat: " + (result == null || result.isBlank() ? "(raderat)" : result);
        } catch (SQLException ex) {
            return "Fel vid uppdatering av resultat: " + ex.getMessage();
        }
    }

    // ====== Lärare: lista en elevs kurser med resultat ======
    public List<String> teacherListStudentCoursesWithResult(String elevPnr) {
        String sql = """
            SELECT c.code,
                   c.name,
                   COALESCE((
                       SELECT k.hogskolepoang
                       FROM kurstillfallen k
                       WHERE k.kurskod = c.code
                       ORDER BY k.artal DESC
                       LIMIT 1
                   ), 0) AS points,
                   e.result
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
                while (rs.next()) {
                    String code   = rs.getString("code");
                    String name   = rs.getString("name");
                    double pts    = rs.getDouble("points");
                    String result = rs.getString("result"); // kan vara null

                    String pointsStr = (Math.floor(pts) == pts)
                            ? String.format("%.0f", pts)
                            : String.format("%.1f", pts);

                    out.add("%s – %s (%s p)%s".formatted(
                            code, name, pointsStr,
                            (result == null ? "" : " [" + result + "]")
                    ));
                }
                return out;
            }
        } catch (SQLException ex) {
            return List.of("Fel vid hämtning: " + ex.getMessage());
        }
    }

    // ====== Poängsummering (lärare kan se per pnr) ======
    public double getStudentTotalPointsByPnr(String pnr) {
        String sql = """
            SELECT COALESCE(SUM(
                COALESCE((
                    SELECT k.hogskolepoang
                    FROM kurstillfallen k
                    WHERE k.kurskod = c.code
                    ORDER BY k.artal DESC
                    LIMIT 1
                ), 0)
            ), 0) AS total_points
            FROM course_enrollments e
            JOIN students s ON s.id = e.student_id
            JOIN courses  c ON c.id = e.course_id
            WHERE s.personnummer = ?
              AND e.result = 'PASSED'
            """;
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total_points") : 0.0;
            }
        } catch (SQLException ex) {
            System.err.println("Fel vid summering av poäng: " + ex.getMessage());
            return 0.0;
        }
    }

    // ====== Poängsummering (student ser egna) ======
    public double getMyTotalPoints() {
        if (!isStudent() || currentStudentId == null) return 0.0;
        String sql = """
            SELECT COALESCE(SUM(
                COALESCE((
                    SELECT k.hogskolepoang
                    FROM kurstillfallen k
                    WHERE k.kurskod = c.code
                    ORDER BY k.artal DESC
                    LIMIT 1
                ), 0)
            ), 0) AS total_points
            FROM course_enrollments e
            JOIN courses c ON c.id = e.course_id
            WHERE e.student_id = ?
              AND e.result = 'PASSED'
            """;
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentStudentId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total_points") : 0.0;
            }
        } catch (SQLException ex) {
            return 0.0;
        }
    }
 // ===== Student: lista mina kurser med poäng + resultat =====
    public java.util.List<String> studentListMyCoursesWithResult() {
        if (!isStudent() || currentStudentId == null) {
            return java.util.List.of("Du måste vara inloggad som student.");
        }
        String sql = """
            SELECT c.code,
                   c.name,
                   COALESCE((
                       SELECT k.hogskolepoang
                       FROM kurstillfallen k
                       WHERE k.kurskod = c.code
                       ORDER BY k.artal DESC
                       LIMIT 1
                   ), 0) AS points,
                   e.result
            FROM course_enrollments e
            JOIN courses c ON c.id = e.course_id
            WHERE e.student_id = ?
            ORDER BY c.code
            """;
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentStudentId);
            try (var rs = ps.executeQuery()) {
                java.util.List<String> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    String code   = rs.getString("code");
                    String name   = rs.getString("name");
                    double pts    = rs.getDouble("points");
                    String result = rs.getString("result"); // kan vara null

                    String pointsStr = (Math.floor(pts) == pts)
                            ? String.format("%.0f", pts)
                            : String.format("%.1f", pts);

                    out.add("%s – %s (%s p)%s".formatted(
                            code, name, pointsStr,
                            (result == null ? "" : " [" + result + "]")
                    ));
                }
                return out;
            }
        } catch (SQLException ex) {
            return java.util.List.of("Fel vid hämtning: " + ex.getMessage());
        }
    }

    // ===== Registrering (fixad kolumnnamn) =====
    public boolean registerUser(String first, String last, String pnr, String password, String roleStr) {
        String r = roleStr == null ? "" : roleStr.trim().toUpperCase();
        if (!"STUDENT".equals(r) && !"TEACHER".equals(r)) return false;

        try (Connection con = getConn()) {
            con.setAutoCommit(false);
            int personId;

            // 1) Skapa person i students/teachers och hämta id
            if ("STUDENT".equals(r)) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO students (name, personnummer) VALUES (?, ?) RETURNING id")) {
                    ps.setString(1, first + " " + last);
                    ps.setString(2, pnr);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { con.rollback(); return false; }
                        personId = rs.getInt("id");
                    }
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO teachers (name, personnummer) VALUES (?, ?) RETURNING id")) {
                    ps.setString(1, first + " " + last);
                    ps.setString(2, pnr);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { con.rollback(); return false; }
                        personId = rs.getInt("id");
                    }
                }
            }

            // 2) Skapa användare (username = personnummer)
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (username, password, role, student_id, teacher_id) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, pnr);
                ps.setString(2, password); // TODO: byt till hash (BCrypt) senare
                ps.setString(3, r);
                if ("STUDENT".equals(r)) {
                    ps.setInt(4, personId);
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setNull(4, Types.INTEGER);
                    ps.setInt(5, personId);
                }
                ps.executeUpdate();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // t.ex. unikhetsfel på personnummer/username
        }
    }

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
    public String  getCurrentUsername() { return (username != null) ? username : ""; }

    // ===== Hjälpare: slå upp id:n =====
    private Integer findStudentIdByPnr(Connection con, String pnr) throws SQLException {
        String sql = "SELECT id FROM students WHERE personnummer = ?";
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private Integer findCourseIdByCode(Connection con, String code) throws SQLException {
        String sql = "SELECT id FROM courses WHERE code = ?";
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private boolean hasKurstillfalle2025(Connection con, String kurskod) throws SQLException {
        String sql = "SELECT 1 FROM kurstillfallen WHERE kurskod = ? AND artal = 2025 LIMIT 1";
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, kurskod);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
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

            String sql = "INSERT INTO course_enrollments(student_id, course_id, status) " +
                         "VALUES(?, ?, 'REGISTERED') ON CONFLICT DO NOTHING";
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
            JOIN courses c        ON c.id = e.course_id
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
                    out.add("%s v%s (%s)".formatted(
                        rs.getString("kurskod"),
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
            String sql = "INSERT INTO course_enrollments(student_id, course_id, status) " +
                         "VALUES(?, ?, 'REGISTERED') ON CONFLICT DO NOTHING";
            try (var ps = con.prepareStatement(sql)) { ps.setInt(1, sid); ps.setInt(2, cid); ps.executeUpdate(); }
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
        if (currentTeacherId == null) return List.of();
        String sql = """
            SELECT DISTINCT k.kurskod, k.kursnamn, k.vecka
            FROM course_teachers ct
            JOIN courses c        ON c.id = ct.course_id
            JOIN kurstillfallen k ON k.kurskod = c.code
            WHERE ct.teacher_id = ? AND k.artal = 2025
            ORDER BY k.kurskod, k.vecka
            """;
        try (var con = getConn(); var ps = con.prepareStatement(sql)) {
            ps.setInt(1, currentTeacherId);
            try (var rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add("%s v%s (%s)".formatted(
                        rs.getString("kurskod"),
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
    
    
    
    
 // ====== Sökning via personnummer (elev eller lärare) ======

    /** Returnerar "ELEV" om PNR finns i students, "LARARE" om i teachers, annars tom. */
    private String resolveRoleByPnr(Connection con, String pnr) throws SQLException {
        try (var ps = con.prepareStatement("SELECT 1 FROM students WHERE personnummer = ?")) {
            ps.setString(1, pnr);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return "ELEV";
            }
        }
        try (var ps = con.prepareStatement("SELECT 1 FROM teachers WHERE personnummer = ?")) {
            ps.setString(1, pnr);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return "LARARE";
            }
        }
        return "";
    }

    /** Hjälpmetod: formaterar rad "CODE – Name (X p)" ev. med resultat i [BRACKETS]. */
    private String formatCourseRow(String code, String name, Double pointsOrNull, String resultOrNull) {
        String pts = pointsOrNull == null ? null :
                (Math.floor(pointsOrNull) == pointsOrNull ? String.format("%.0f", pointsOrNull) : String.format("%.1f", pointsOrNull));
        String tail = "";
        if (pts != null) tail += " (" + pts + " p)";
        if (resultOrNull != null) tail += " [" + resultOrNull + "]";
        return code + " – " + name + tail;
    }

    /** Kurser för elev-PNR (hämtar ev. poäng från senaste kurstillfälle + elevens resultat). */
    private List<String> listCoursesForStudentPnr(Connection con, String pnr) throws SQLException {
        String sql = """
            SELECT c.code, c.name,
                   COALESCE((
                       SELECT k.hogskolepoang
                       FROM kurstillfallen k
                       WHERE k.kurskod = c.code
                       ORDER BY k.artal DESC
                       LIMIT 1
                   ), 0) AS points,
                   e.result
            FROM course_enrollments e
            JOIN students s ON s.id = e.student_id
            JOIN courses  c ON c.id = e.course_id
            WHERE s.personnummer = ?
            ORDER BY c.code
            """;
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (var rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(formatCourseRow(
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getDouble("points"),
                            rs.getString("result")));
                }
                return out;
            }
        }
    }

    /** Kurser för lärar-PNR (kurser läraren undervisar i). */
    private List<String> listCoursesForTeacherPnr(Connection con, String pnr) throws SQLException {
        String sql = """
            SELECT c.code, c.name,
                   COALESCE((
                       SELECT k.hogskolepoang
                       FROM kurstillfallen k
                       WHERE k.kurskod = c.code
                       ORDER BY k.artal DESC
                       LIMIT 1
                   ), 0) AS points
            FROM course_teachers ct
            JOIN teachers t ON t.id = ct.teacher_id
            JOIN courses  c ON c.id = ct.course_id
            WHERE t.personnummer = ?
            ORDER BY c.code
            """;
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (var rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(formatCourseRow(
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getDouble("points"),
                            null));
                }
                return out;
            }
        }
    }

    /**
     * Publik metod för GUI: skriv in ett personnummer (elev eller lärare)
     * och få tillbaka en lista med rubrik + kurser.
     */
    public List<String> listCoursesByPersonnummer(String pnr) {
        if (pnr == null || pnr.isBlank()) return List.of("Ange personnummer.");
        try (var con = getConn()) {
            String who = resolveRoleByPnr(con, pnr);
            if (who.isEmpty()) {
                return List.of("Hittade ingen elev eller lärare med personnummer: " + pnr);
            }
            List<String> rows = "ELEV".equals(who)
                    ? listCoursesForStudentPnr(con, pnr)
                    : listCoursesForTeacherPnr(con, pnr);

            String header = "ELEV".equals(who) ? "Kurser för elev " : "Kurser för lärare ";
            List<String> out = new ArrayList<>();
            out.add(header + pnr + ":");
            out.addAll(rows.isEmpty() ? List.of("(inga)") : rows);
            return out;
        } catch (SQLException ex) {
            return List.of("Fel vid sökning: " + ex.getMessage());
        }
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
 // Hämta nuvarande inloggades personnummer (oavsett roll)
    public java.util.Optional<String> getCurrentPersonnummer() {
        if (isStudent() && currentStudentId != null) {
            String sql = "SELECT personnummer FROM students WHERE id = ?";
            try (var con = getConn(); var ps = con.prepareStatement(sql)) {
                ps.setInt(1, currentStudentId);
                try (var rs = ps.executeQuery()) { if (rs.next()) return java.util.Optional.of(rs.getString(1)); }
            } catch (Exception ignored) {}
        } else if (isTeacher() && currentTeacherId != null) {
            String sql = "SELECT personnummer FROM teachers WHERE id = ?";
            try (var con = getConn(); var ps = con.prepareStatement(sql)) {
                ps.setInt(1, currentTeacherId);
                try (var rs = ps.executeQuery()) { if (rs.next()) return java.util.Optional.of(rs.getString(1)); }
            } catch (Exception ignored) {}
        }
        return java.util.Optional.empty();
    }

    // Hämta nuvarande inloggades namn (från students/teachers)
    public java.util.Optional<String> getCurrentName() {
        if (isStudent() && currentStudentId != null) {
            String sql = "SELECT name FROM students WHERE id = ?";
            try (var con = getConn(); var ps = con.prepareStatement(sql)) {
                ps.setInt(1, currentStudentId);
                try (var rs = ps.executeQuery()) { if (rs.next()) return java.util.Optional.of(rs.getString(1)); }
            } catch (Exception ignored) {}
        } else if (isTeacher() && currentTeacherId != null) {
            String sql = "SELECT name FROM teachers WHERE id = ?";
            try (var con = getConn(); var ps = con.prepareStatement(sql)) {
                ps.setInt(1, currentTeacherId);
                try (var rs = ps.executeQuery()) { if (rs.next()) return java.util.Optional.of(rs.getString(1)); }
            } catch (Exception ignored) {}
        }
        return java.util.Optional.empty();
    }

    // Praktisk display-sträng: "Namn – PNR"
    public java.util.Optional<String> getCurrentNameAndPnr() {
        var name = getCurrentName();
        var pnr  = getCurrentPersonnummer();
        if (name.isPresent() && pnr.isPresent()) {
            return java.util.Optional.of(name.get() + " – " + pnr.get());
        }
        // fallback om något saknas
        if (name.isPresent()) return name;
        if (pnr.isPresent())  return pnr;
        return java.util.Optional.empty();
    }
}