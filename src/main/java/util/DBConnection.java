package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton connection to XAMPP MySQL (phpMyAdmin).
 * Database: rehla — tables: personne, preference, reservation, etc.
 * Change user/password below if your XAMPP MySQL uses different credentials.
 */
public class DBConnection {
    // Match your phpMyAdmin: server 127.0.0.1, database "rehla"
    private static final String URL = "jdbc:mysql://localhost:3306/rehla?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection conn;
    private static DBConnection instance;

    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConn() {
        if (conn == null) {
            throw new IllegalStateException(
                "[DB] Not connected. Check: 1) XAMPP MySQL is STARTED  2) Database 'rehla' exists  3) User '" + USER + "' / password in DBConnection.java"
            );
        }
        return conn;
    }

    private DBConnection() {
        try {
            this.conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DB] Connected to MySQL — database: rehla");
        } catch (SQLException e) {
            this.conn = null;
            System.err.println("[DB] Connection failed: " + e.getMessage());
            e.printStackTrace();
            System.err.println("[DB] Check: 1) XAMPP MySQL is STARTED  2) Database 'rehla' exists  3) User '" + USER + "' / password correct.");
        }
    }
}