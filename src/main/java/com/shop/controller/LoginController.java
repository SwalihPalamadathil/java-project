package com.shop.controller;

import com.shop.model.User;
import com.shop.service.AuthService;
import com.shop.util.AlertUtil;
import com.shop.view.MainLayout;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller and UI builder for the User Authentication screen.
 */
public class LoginController {

    private final AuthService authService = new AuthService();
    private final Stage stage;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button loginButton;
    private ProgressIndicator spinner;

    public LoginController(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b);");

        VBox card = new VBox(18);
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-padding: 35; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);");
        card.setAlignment(Pos.CENTER);

        // Header Title
        Label brandTitle = new Label("APEX RETAIL");
        brandTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        Label subTitle = new Label("Shop Management & POS System");
        subTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Form Fields
        VBox userBox = new VBox(5);
        Label userLbl = new Label("Username");
        userLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        usernameField = new TextField();
        usernameField.setPromptText("e.g. admin or cashier");
        userBox.getChildren().addAll(userLbl, usernameField);

        VBox passBox = new VBox(5);
        Label passLbl = new Label("Password");
        passLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passBox.getChildren().addAll(passLbl, passwordField);

        // Error message label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        // Actions
        spinner = new ProgressIndicator();
        spinner.setMaxSize(24, 24);
        spinner.setVisible(false);

        loginButton = new Button("Sign In");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        HBox buttonBox = new HBox(10, loginButton, spinner);
        buttonBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(loginButton, Priority.ALWAYS);

        Label defaultHint = new Label("Default Admin: admin / Admin@123\nDefault Staff: cashier / Cashier@123");
        defaultHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
        defaultHint.setAlignment(Pos.CENTER);

        // Event Listeners
        loginButton.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        card.getChildren().addAll(brandTitle, subTitle, userBox, passBox, errorLabel, buttonBox, defaultHint);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 900, 650);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }

        stage.setTitle("Shop Management System - Login");
        stage.setScene(scene);
        stage.show();
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.trim().isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginButton.setDisable(true);
        spinner.setVisible(true);
        errorLabel.setVisible(false);

        // Run authentication asynchronously to prevent UI freezing
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return authService.login(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            spinner.setVisible(false);
            User user = loginTask.getValue();

            if (user.isMustChangePassword()) {
                showChangePasswordPrompt(user);
            } else {
                navigateToMain(user);
            }
        });

        loginTask.setOnFailed(e -> {
            loginButton.setDisable(false);
            spinner.setVisible(false);
            Throwable ex = loginTask.getException();
            showError(ex != null ? ex.getMessage() : "Authentication failed.");
        });

        new Thread(loginTask).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void showChangePasswordPrompt(User user) {
        Stage promptStage = new Stage();
        promptStage.initModality(Modality.APPLICATION_MODAL);
        promptStage.setTitle("First Login - Change Default Password");

        VBox box = new VBox(15);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: #ffffff;");

        Label lbl = new Label("Please update your password from the default before proceeding.");
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        PasswordField newPass = new PasswordField();
        newPass.setPromptText("Enter new secure password");

        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirm new secure password");

        Button saveBtn = new Button("Update Password & Continue");
        saveBtn.getStyleClass().add("btn-primary");

        saveBtn.setOnAction(evt -> {
            String np = newPass.getText();
            String cp = confirmPass.getText();
            if (np.length() < 6) {
                AlertUtil.showError("Validation Error", "Password must be at least 6 characters.");
                return;
            }
            if (!np.equals(cp)) {
                AlertUtil.showError("Validation Error", "Passwords do not match.");
                return;
            }
            try {
                authService.changePassword(user.getId(), passwordField.getText(), np);
                AlertUtil.showInfo("Success", "Password updated successfully!");
                promptStage.close();
                navigateToMain(user);
            } catch (Exception ex) {
                AlertUtil.showError("Error", ex.getMessage());
            }
        });

        box.getChildren().addAll(lbl, new Label("New Password:"), newPass, new Label("Confirm Password:"), confirmPass, saveBtn);
        Scene scene = new Scene(box, 380, 280);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        promptStage.setScene(scene);
        promptStage.showAndWait();
    }

    private void navigateToMain(User user) {
        MainLayout mainLayout = new MainLayout(stage);
        mainLayout.show();
    }
}
