package model;

import java.sql.*;
import java.util.Optional;

public class UserDAO {
    public Optional<User> authenticate(String username, String password) throws SQLException {
        String sql = """
            SELECT id, username, role, student_id, teacher_id
            FROM users
            WHERE username = ? AND password = ?
        """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password); // DEMO: klartext. Byt till hash i skarpt läge.
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        (Integer) rs.getObject("student_id"),
                        (Integer) rs.getObject("teacher_id")
                    ));
                }
                return Optional.empty();
            }
        }
        
    }
    public boolean createUser(String first, String last, String pnr, String password, String role) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            int personId;
            // Skapa student eller lärare
            if ("STUDENT".equalsIgnoreCase(role)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO students (namn, personnummer) VALUES (?, ?) RETURNING id")) {
                    ps.setString(1, first + " " + last);
                    ps.setString(2, pnr);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        personId = rs.getInt("id");
                    }
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO teachers (namn, personnummer) VALUES (?, ?) RETURNING id")) {
                    ps.setString(1, first + " " + last);
                    ps.setString(2, pnr);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        personId = rs.getInt("id");
                    }
                }
            }

            // Skapa user (username = pnr)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password, role, student_id, teacher_id) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, pnr);
                ps.setString(2, password);                 // TODO: byt till hash
                ps.setString(3, role);
                if ("STUDENT".equalsIgnoreCase(role)) {
                    ps.setInt(4, personId);
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setNull(4, Types.INTEGER);
                    ps.setInt(5, personId);
                }
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            // om conn finns kan man rollbacka; i try-with-resources stängs den ändå
            throw e;
        }
    }
}