package com.shop;

import com.shop.config.AppConfig;
import com.shop.controller.LoginController;
import com.shop.util.DatabaseInitializer;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX Application lifecycle coordinator.
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting {} v{}...", AppConfig.APP_NAME, AppConfig.APP_VERSION);

        // 1. Initialize SQLite database schema and seed data
        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            logger.error("Failed to initialize database: {}", e.getMessage(), e);
        }

        // 2. Launch User Authentication Screen
        LoginController loginController = new LoginController(primaryStage);
        loginController.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
