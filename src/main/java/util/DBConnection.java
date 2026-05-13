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


   /* private String url = "jdbc:mysql://localhost:3306/rehla";*/

    private Connection conn;
    private static DBConnection instance;

    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }

    private DBConnection() {
        try {
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connection established");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


    }

}