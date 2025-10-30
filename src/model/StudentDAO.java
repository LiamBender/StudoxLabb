package model;

import java.sql.*;
import java.util.Optional;

public class StudentDAO {
    public Optional<Student> findByPnr(String pnr) throws SQLException {
        String sql = "SELECT id, personnummer, name FROM students WHERE personnummer = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Student(
                        rs.getInt("id"), rs.getString("personnummer"), rs.getString("name")));
                }
                return Optional.empty();
            }
        }
    }

    public Student createIfMissing(String pnr, String name) throws SQLException {
        // Skapa om inte finns
        var existing = findByPnr(pnr);
        if (existing.isPresent()) return existing.get();

        String sql = "INSERT INTO students(personnummer, name) VALUES(?, ?) RETURNING id";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pnr);
            ps.setString(2, (name==null || name.isBlank()) ? pnr : name);
            try (ResultSet rs = ps.executeQuery()) {
                Student s = new Student(pnr, (name==null || name.isBlank()) ? pnr : name);
                if (rs.next()) s.setId(rs.getInt(1));
                return s;
            }
        }
    }
}