package za.co.alis.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
   private static final String URL = "jdbc:postgresql://localhost:5432/ALIS_WEBAPPDB";
private static final String USER = "postgres";
private static final String PASS = "Lunga"; // if that's your password

    static {
        try { Class.forName("org.postgresql.Driver"); }
        catch (ClassNotFoundException e) { e.printStackTrace(); }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
