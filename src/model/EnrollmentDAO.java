package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO {

    public boolean joinCourse(int studentId, int courseId) throws SQLException {
        String sql = """
            INSERT INTO course_enrollments(student_id, course_id)
            VALUES (?, ?) ON CONFLICT (student_id, course_id) DO NOTHING
        """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean leaveCourse(int studentId, int courseId) throws SQLException {
        String sql = "DELETE FROM course_enrollments WHERE student_id=? AND course_id=? AND status='REGISTERED'";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<String> listCoursesForStudent(int studentId) throws SQLException {
        String sql = """
            SELECT c.code || ' - ' || c.name AS label
            FROM course_enrollments e
            JOIN courses c ON c.id = e.course_id
            WHERE e.student_id = ?
            ORDER BY c.code
        """;
        List<String> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString("label"));
            }
        }
        return out;
    }
}