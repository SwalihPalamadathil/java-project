package com.shop.controller;

import com.shop.config.AppConfig;
import com.shop.dao.SettingDao;
import com.shop.dao.SettingDaoImpl;
import com.shop.service.AuthService;
import com.shop.util.AlertUtil;
import com.shop.util.DateUtil;
import com.shop.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Map;

/**
 * Controller for managing Shop Profile, Tax/Currency preferences, Database Backups, and Security.
 */
public class SettingsController {

    private final SettingDao settingDao = new SettingDaoImpl();
    private final AuthService authService = new AuthService();

    private TextField shopNameField;
    private TextField shopAddressField;
    private TextField shopPhoneField;
    private TextField shopGstField;
    private TextField shopCurrencyField;

    private PasswordField currentPassField;
    private PasswordField newPassField;
    private PasswordField confirmPassField;

    public Node getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(10));

        // 1. Shop Profile Configuration Box
        VBox shopBox = createShopProfileSection();

        // 2. Change Password Security Box
        VBox securityBox = createSecuritySection();

        // 3. Database Maintenance Box (Backup & Restore)
        VBox backupBox = createBackupSection();

        root.getChildren().addAll(shopBox, securityBox, backupBox);
        loadSettings();

        return root;
    }

    private VBox createShopProfileSection() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));

        Label title = new Label("🏪 Shop Profile & Tax Details");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        shopNameField = new TextField();
        shopNameField.setPrefWidth(300);

        shopAddressField = new TextField();
        shopAddressField.setPrefWidth(300);

        shopPhoneField = new TextField();
        shopGstField = new TextField();
        shopCurrencyField = new TextField("$");
        shopCurrencyField.setPrefWidth(60);

        grid.add(new Label("Business / Shop Name:"), 0, 0);
        grid.add(shopNameField, 1, 0);

        grid.add(new Label("Store Address:"), 0, 1);
        grid.add(shopAddressField, 1, 1);

        grid.add(new Label("Phone Number:"), 0, 2);
        grid.add(shopPhoneField, 1, 2);

        grid.add(new Label("Tax / GSTIN Number:"), 0, 3);
        grid.add(shopGstField, 1, 3);

        grid.add(new Label("Currency Symbol:"), 0, 4);
        grid.add(shopCurrencyField, 1, 4);

        Button saveBtn = new Button("Save Shop Details");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> handleSaveSettings());

        grid.add(saveBtn, 1, 5);

        box.getChildren().addAll(title, grid);
        return box;
    }

    private VBox createSecuritySection() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));

        Label title = new Label("🔒 Security & Change Password");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        currentPassField = new PasswordField();
        newPassField = new PasswordField();
        confirmPassField = new PasswordField();

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(currentPassField, 1, 0);

        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPassField, 1, 1);

        grid.add(new Label("Confirm New Password:"), 0, 2);
        grid.add(confirmPassField, 1, 2);

        Button changePassBtn = new Button("Update Password");
        changePassBtn.getStyleClass().add("btn-warning");
        changePassBtn.setOnAction(e -> handleChangePassword());

        grid.add(changePassBtn, 1, 3);

        box.getChildren().addAll(title, grid);
        return box;
    }

    private VBox createBackupSection() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));

        Label title = new Label("💾 Database Backup & Restore");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        Label desc = new Label("Export an instant backup snapshot of the entire SQLite database or restore from a previous backup file.");
        desc.setStyle("-fx-text-fill: #64748b;");

        HBox actions = new HBox(15);
        Button backupBtn = new Button("📥 Backup Database Now");
        backupBtn.getStyleClass().add("btn-success");
        backupBtn.setOnAction(e -> handleBackup());

        Button restoreBtn = new Button("🔄 Restore Database from File");
        restoreBtn.getStyleClass().add("btn-danger");
        restoreBtn.setOnAction(e -> handleRestore());

        actions.getChildren().addAll(backupBtn, restoreBtn);
        box.getChildren().addAll(title, desc, actions);
        return box;
    }

    private void loadSettings() {
        Map<String, String> map = settingDao.getAll();
        shopNameField.setText(map.getOrDefault("shop_name", "Apex Supermarket"));
        shopAddressField.setText(map.getOrDefault("shop_address", "104 Market Road, City Centre"));
        shopPhoneField.setText(map.getOrDefault("shop_phone", "+1 555-019-2834"));
        shopGstField.setText(map.getOrDefault("shop_gst", "29ABCDE1234F1Z5"));
        shopCurrencyField.setText(map.getOrDefault("shop_currency", "$"));
    }

    private void handleSaveSettings() {
        try {
            settingDao.set("shop_name", shopNameField.getText().trim(), "Retail Shop Business Name");
            settingDao.set("shop_address", shopAddressField.getText().trim(), "Physical Shop Address");
            settingDao.set("shop_phone", shopPhoneField.getText().trim(), "Contact Telephone Number");
            settingDao.set("shop_gst", shopGstField.getText().trim(), "Tax / GST Identification Number");
            settingDao.set("shop_currency", shopCurrencyField.getText().trim(), "Currency symbol");

            AlertUtil.showInfo("Success", "Shop details updated successfully!");
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    private void handleChangePassword() {
        String cp = currentPassField.getText();
        String np = newPassField.getText();
        String cnp = confirmPassField.getText();

        if (cp.isEmpty() || np.isEmpty()) {
            AlertUtil.showError("Validation Error", "All password fields are required.");
            return;
        }

        if (np.length() < 6) {
            AlertUtil.showError("Validation Error", "New password must be at least 6 characters.");
            return;
        }

        if (!np.equals(cnp)) {
            AlertUtil.showError("Validation Error", "New passwords do not match.");
            return;
        }

        try {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            authService.changePassword(userId, cp, np);
            AlertUtil.showInfo("Success", "Password updated successfully!");
            currentPassField.clear();
            newPassField.clear();
            confirmPassField.clear();
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    private void handleBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Database Backup");
        chooser.setInitialFileName("shop_backup_" + DateUtil.todayString() + ".db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database (*.db)", "*.db"));

        File target = chooser.showSaveDialog(null);
        if (target != null) {
            try {
                File source = new File(AppConfig.DB_FILE_NAME);
                if (!source.exists()) {
                    AlertUtil.showError("Error", "Source database file does not exist.");
                    return;
                }
                copyFile(source, target);
                AlertUtil.showInfo("Backup Complete", "Database successfully backed up to:\n" + target.getAbsolutePath());
            } catch (Exception e) {
                AlertUtil.showError("Backup Error", e.getMessage());
            }
        }
    }

    private void handleRestore() {
        if (!AlertUtil.showConfirmation("Database Restore Warning",
                "Restoring will overwrite current database data. Please restart the application after restore. Continue?")) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Database Backup to Restore");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database (*.db)", "*.db"));

        File source = chooser.showOpenDialog(null);
        if (source != null) {
            try {
                File target = new File(AppConfig.DB_FILE_NAME);
                copyFile(source, target);
                AlertUtil.showInfo("Restore Successful", "Database restored! Please restart the application to reload changes.");
            } catch (Exception e) {
                AlertUtil.showError("Restore Error", e.getMessage());
            }
        }
    }

    private void copyFile(File source, File dest) throws Exception {
        try (FileChannel src = new FileInputStream(source).getChannel();
             FileChannel dst = new FileOutputStream(dest).getChannel()) {
            dst.transferFrom(src, 0, src.size());
        }
    }
}
