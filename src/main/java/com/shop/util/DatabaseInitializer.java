package com.shop.util;

import com.shop.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Executes database setup and automatic seeding on first application launch.
 */
public final class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private DatabaseInitializer() {}

    /**
     * Initializes the database schema and seeds initial data.
     */
    public static void initialize() {
        logger.info("Initializing database schema...");
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            executeSqlScript(conn, "/schema.sql");
            seedDefaultUsers(conn);
            logger.info("Database initialization completed successfully.");
        } catch (Exception e) {
            logger.error("Error during database initialization: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Reads and executes SQL commands from a resource file.
     */
    private static void executeSqlScript(Connection conn, String scriptPath) throws Exception {
        InputStream is = DatabaseInitializer.class.getResourceAsStream(scriptPath);
        if (is == null) {
            logger.warn("SQL script not found at {}, relying on embedded schema.", scriptPath);
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and SQL comments
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = sb.toString().trim();
                    sb.setLength(0);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    } catch (Exception ex) {
                        logger.debug("SQL execution warning (may be duplicate seed): {}", ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Seeds initial Admin and Staff users with hashed BCrypt passwords if not present.
     */
    private static void seedDefaultUsers(Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password_hash, full_name, role, status, failed_attempts, must_change_password) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE', 0, ?)";

        try {
            // 1. Seed Admin
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, AppConfig.DEFAULT_ADMIN_USERNAME);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        String adminHash = PasswordUtil.hashPassword(AppConfig.DEFAULT_ADMIN_PASSWORD);
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, AppConfig.DEFAULT_ADMIN_USERNAME);
                            insertStmt.setString(2, adminHash);
                            insertStmt.setString(3, "Store Administrator");
                            insertStmt.setString(4, "ADMIN");
                            insertStmt.setInt(5, 1); // Must change password on first login
                            insertStmt.executeUpdate();
                            logger.info("Default administrator account created (Username: {})", AppConfig.DEFAULT_ADMIN_USERNAME);
                        }
                    }
                }
            }

            // 2. Seed Staff Cashier
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, AppConfig.DEFAULT_STAFF_USERNAME);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        String staffHash = PasswordUtil.hashPassword(AppConfig.DEFAULT_STAFF_PASSWORD);
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, AppConfig.DEFAULT_STAFF_USERNAME);
                            insertStmt.setString(2, staffHash);
                            insertStmt.setString(3, "Staff Cashier");
                            insertStmt.setString(4, "STAFF");
                            insertStmt.setInt(5, 0);
                            insertStmt.executeUpdate();
                            logger.info("Default staff cashier account created (Username: {})", AppConfig.DEFAULT_STAFF_USERNAME);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to seed default users: {}", e.getMessage(), e);
        }
    }
}
