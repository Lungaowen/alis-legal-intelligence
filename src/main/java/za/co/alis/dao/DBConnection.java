package za.co.alis.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = System.getenv().getOrDefault(
        "ALIS_DB_URL",
        "jdbc:postgresql://localhost:5432/ALIS_WEBAPPDB"
    );
    private static final String USER = System.getenv().getOrDefault("ALIS_DB_USER", "postgres");
    private static final String PASS = System.getenv().getOrDefault("ALIS_DB_PASSWORD", "");

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("PostgreSQL JDBC driver is not available: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
