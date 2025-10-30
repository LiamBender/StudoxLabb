package model;

import java.sql.*;
import java.util.Optional;

public class CourseDAO {
    public Optional<Course> findByCode(String code) throws SQLException {
        String sql = "SELECT id, code, name, hp FROM courses WHERE code = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Course(
                        rs.getInt("id"), rs.getString("code"),
                        rs.getString("name"), rs.getDouble("hp")));
                }
                return Optional.empty();
            }
        }
    }
}