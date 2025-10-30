package model;

public class User {
    private int id;
    private String username;
    private String role; // "STUDENT" eller "TEACHER"
    private Integer studentId; // null om TEACHER
    private Integer teacherId; // null om STUDENT

    public User(int id, String username, String role, Integer studentId, Integer teacherId) {
        this.id = id; this.username = username; this.role = role;
        this.studentId = studentId; this.teacherId = teacherId;
    }
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Integer getStudentId() { return studentId; }
    public Integer getTeacherId() { return teacherId; }
    public boolean isStudent() { return "STUDENT".equalsIgnoreCase(role); }
    public boolean isTeacher() { return "TEACHER".equalsIgnoreCase(role); }
}