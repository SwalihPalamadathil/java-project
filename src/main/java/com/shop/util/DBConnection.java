package com.shop.util;

import com.shop.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database connection manager for SQLite.
 * Ensures foreign keys are enforced and connections are safely created.
 */
public class DBConnection {

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static DBConnection instance;
    private Connection connection;

    private DBConnection() {
        try {
            // Explicitly load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            initConnection();
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver not found: {}", e.getMessage(), e);
            throw new RuntimeException("SQLite JDBC driver missing from classpath", e);
        }
    }

    private void initConnection() {
        try {
            connection = DriverManager.getConnection(AppConfig.DB_URL);
            try (Statement stmt = connection.createStatement()) {
                // SQLite has foreign keys disabled by default; must enable explicitly
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            logger.info("Successfully established connection to SQLite database: {}", AppConfig.DB_URL);
        } catch (SQLException e) {
            logger.error("Failed to connect to database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Obtains the active database connection. If closed or invalid, reconnects.
     *
     * @return active java.sql.Connection
     * @throws SQLException on database error
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initConnection();
        }
        return connection;
    }

    /**
     * Creates a new separate Connection instance (useful for isolated background transactions).
     */
    public static Connection createNewConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(AppConfig.DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
