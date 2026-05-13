package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private final String url = "jdbc:mysql://localhost:3306/rehla"
            + "?autoReconnect=true&useSSL=false&cachePrepStmts=false"
            + "&useServerPrepStmts=false";
    private final String user = "root";
    private final String password = "";
    private Connection conn;
    private static DBConnection instance;

    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Destroy the singleton entirely.
     * Next getInstance() call creates a brand-new connection with no cached state.
     */
    public static void resetInstance() {
        if (instance != null) {
            try {
                if (instance.conn != null && !instance.conn.isClosed()) {
                    instance.conn.close();
                }
            } catch (Exception ignored) {}
            instance = null;
        }
    }

    public Connection getConn() {
        try {
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                conn = openConnection();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Reconnect failed: " + e.getMessage());
        }
        return conn;
    }

    /**
     * Force a brand-new connection.
     * Call this before any refresh operation to guarantee fresh data from DB.
     */
    public void forceReconnect() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
            conn = openConnection();
            System.out.println("[DB] Force reconnected — fresh data ready.");
        } catch (SQLException e) {
            System.err.println("[DB] Force reconnect failed: " + e.getMessage());
        }
    }

    private DBConnection() {
        try {
            this.conn = openConnection();
            System.out.println("[DB] Connection established.");
        } catch (SQLException e) {
            System.err.println("[DB] Initial connection failed: " + e.getMessage());
        }
    }

    private Connection openConnection() throws SQLException {
        Connection c = DriverManager.getConnection(url, user, password);
        // READ_COMMITTED: every SELECT sees the latest committed data
        // (avoids MySQL REPEATABLE_READ snapshot that hides web-app changes)
        c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        c.setAutoCommit(true);
        return c;
    }
}