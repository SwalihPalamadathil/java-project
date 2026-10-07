package com.shop.view;

import com.shop.config.AppConfig;
import com.shop.controller.DashboardController;
import com.shop.controller.EmployeeController;
import com.shop.controller.LoginController;
import com.shop.controller.POSController;
import com.shop.controller.PayrollController;
import com.shop.controller.ProductController;
import com.shop.controller.ReportController;
import com.shop.controller.SalesHistoryController;
import com.shop.controller.SettingsController;
import com.shop.controller.StockController;
import com.shop.model.User;
import com.shop.util.AlertUtil;
import com.shop.util.DateUtil;
import com.shop.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application shell with persistent navigation sidebar and header.
 */
public class MainLayout {

    private final Stage stage;
    private BorderPane rootLayout;
    private StackPane contentArea;
    private final List<Button> navButtons = new ArrayList<>();
    private Label headerTitleLabel;

    public MainLayout(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        rootLayout = new BorderPane();

        // 1. Sidebar Navigation
        VBox sidebar = createSidebar();
        rootLayout.setLeft(sidebar);

        // 2. Top Header Bar
        HBox header = createHeader();
        rootLayout.setTop(header);

        // 3. Center Content Area
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 20;");
        
        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        rootLayout.setCenter(scrollPane);

        Scene scene = new Scene(rootLayout, 1280, 800);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }

        stage.setTitle(AppConfig.APP_NAME);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        // Load default view (Dashboard)
        showDashboard();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        Label brandLabel = new Label("APEX RETAIL");
        brandLabel.getStyleClass().add("sidebar-title");

        Button btnDashboard = createNavButton("📊  Dashboard", this::showDashboard);
        Button btnPos = createNavButton("🛒  POS / Billing", this::showPOS);
        Button btnProducts = createNavButton("📦  Products", this::showProducts);
        Button btnStock = createNavButton("📋  Inventory & Stock", this::showStock);
        Button btnSales = createNavButton("🧾  Sales History", this::showSalesHistory);
        Button btnEmployees = createNavButton("👥  Employees & HR", this::showEmployees);
        Button btnPayroll = createNavButton("💵  Monthly Payroll", this::showPayroll);
        Button btnReports = createNavButton("📈  Reports & Analytics", this::showReports);
        Button btnSettings = createNavButton("⚙️  Shop Settings", this::showSettings);

        sidebar.getChildren().addAll(brandLabel, btnDashboard, btnPos, btnProducts, btnStock, btnSales);

        boolean isAdmin = SessionManager.getInstance().isAdmin();
        if (isAdmin) {
            sidebar.getChildren().addAll(btnEmployees, btnPayroll, btnReports, btnSettings);
        } else {
            // Staff roles have view-only stock and POS
            btnProducts.setText("📦  Products (View)");
        }

        // Active indicator on first button
        setActiveButton(btnDashboard);

        return sidebar;
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            setActiveButton(btn);
            action.run();
        });
        navButtons.add(btn);
        return btn;
    }

    private void setActiveButton(Button active) {
        for (Button b : navButtons) {
            b.getStyleClass().remove("sidebar-button-active");
        }
        active.getStyleClass().add("sidebar-button-active");
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        headerTitleLabel = new Label("Dashboard Overview");
        headerTitleLabel.getStyleClass().add("header-title");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        User currentUser = SessionManager.getInstance().getCurrentUser();
        String roleStr = currentUser != null ? currentUser.getRole() : "GUEST";
        String nameStr = currentUser != null ? currentUser.getFullName() : "User";

        Label userBadge = new Label(nameStr + " (" + roleStr + ")");
        userBadge.getStyleClass().add("user-badge");

        Label dateLabel = new Label("Today: " + DateUtil.formatDisplayDate(DateUtil.todayString()));
        dateLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: 500;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("btn-danger");
        logoutBtn.setStyle("-fx-padding: 6 12;");
        logoutBtn.setOnAction(e -> handleLogout());

        header.getChildren().addAll(headerTitleLabel, spacer, dateLabel, userBadge, logoutBtn);
        return header;
    }

    private void setView(String title, Node viewNode) {
        headerTitleLabel.setText(title);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(viewNode);
    }

    private void handleLogout() {
        if (AlertUtil.showConfirmation("Logout Confirmation", "Are you sure you want to log out?")) {
            SessionManager.getInstance().logout();
            LoginController loginController = new LoginController(stage);
            loginController.show();
        }
    }

    // View Navigation Methods
    public void showDashboard() {
        DashboardController controller = new DashboardController(this);
        setView("Dashboard Overview", controller.getView());
    }

    public void showPOS() {
        POSController controller = new POSController();
        setView("Point of Sale (POS) Billing", controller.getView());
    }

    public void showProducts() {
        ProductController controller = new ProductController();
        setView("Product Inventory & Catalog", controller.getView());
    }

    public void showStock() {
        StockController controller = new StockController();
        setView("Stock Operations & Audit Log", controller.getView());
    }

    public void showSalesHistory() {
        SalesHistoryController controller = new SalesHistoryController();
        setView("Sales History & Invoice Records", controller.getView());
    }

    public void showEmployees() {
        EmployeeController controller = new EmployeeController();
        setView("Employee Management & Attendance", controller.getView());
    }

    public void showPayroll() {
        PayrollController controller = new PayrollController();
        setView("Payroll & Payslip Management", controller.getView());
    }

    public void showReports() {
        ReportController controller = new ReportController();
        setView("Business Analytics & Reports", controller.getView());
    }

    public void showSettings() {
        SettingsController controller = new SettingsController();
        setView("Shop Profile & System Settings", controller.getView());
    }
}
