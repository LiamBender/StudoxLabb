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
}