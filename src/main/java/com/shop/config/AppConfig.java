package com.shop.config;

/**
 * Global application constants and configuration settings.
 */
public final class AppConfig {

    private AppConfig() {
        // Prevent instantiation
    }

    // Application Metadata
    public static final String APP_NAME = "Shop Management System";
    public static final String APP_VERSION = "1.0.0";

    // Database Connection URL
    public static final String DB_FILE_NAME = "shop_management.db";
    public static final String DB_URL = "jdbc:sqlite:" + DB_FILE_NAME;

    // Security Settings
    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    public static final String DEFAULT_STAFF_USERNAME = "cashier";
    public static final String DEFAULT_STAFF_PASSWORD = "Cashier@123";

    // Default Business Settings
    public static final String DEFAULT_CURRENCY = "$";
    public static final double DEFAULT_TAX_RATE = 5.0; // 5%

    // UI Theme Settings
    public static final String STYLESHEET_PATH = "/com/shop/styles.css";
}
