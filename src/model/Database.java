package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    // Byt till din databas/credentials
    private static final String URL  = "jdbc:postgresql://localhost:5432/Studox";
    private static final String USER = "postgres";
    private static final String PASS = "";

    static {
        try { Class.forName("org.postgresql.Driver"); }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Saknar PostgreSQL JDBC-driver på classpath.", e);
        }
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}