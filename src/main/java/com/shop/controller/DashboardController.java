package com.shop.controller;

import com.shop.model.Product;
import com.shop.service.DashboardService;
import com.shop.view.MainLayout;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * Controller for the Dashboard screen showing KPIs, trend charts, and low-stock alerts.
 */
public class DashboardController {

    private final DashboardService dashboardService = new DashboardService();
    private final MainLayout mainLayout;

    private Label todaySalesLabel;
    private Label monthSalesLabel;
    private Label totalProductsLabel;
    private Label lowStockLabel;
    private Label totalEmployeesLabel;

    private LineChart<String, Number> salesLineChart;
    private BarChart<String, Number> topProductsChart;
    private TableView<Product> lowStockTable;

    public DashboardController(MainLayout mainLayout) {
        this.mainLayout = mainLayout;
    }

    public Node getView() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(10, 10, 30, 10));

        // 1. KPI Cards Row
        HBox cardsRow = createKpiCards();

        // 2. Charts Row (7-day trend + Top 5 products)
        HBox chartsRow = createChartsRow();

        // 3. Low Stock Alerts Section
        VBox lowStockBox = createLowStockSection();

        container.getChildren().addAll(cardsRow, chartsRow, lowStockBox);

        // Load data asynchronously to avoid freezing the UI
        loadDashboardDataAsync();

        return container;
    }

    private HBox createKpiCards() {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER);

        todaySalesLabel = new Label("Loading...");
        monthSalesLabel = new Label("Loading...");
        totalProductsLabel = new Label("Loading...");
        lowStockLabel = new Label("Loading...");
        totalEmployeesLabel = new Label("Loading...");

        VBox cardToday = createCard("TODAY'S REVENUE", todaySalesLabel, "#2563eb");
        VBox cardMonth = createCard("THIS MONTH REVENUE", monthSalesLabel, "#10b981");
        VBox cardProducts = createCard("ACTIVE PRODUCTS", totalProductsLabel, "#6366f1");
        VBox cardLowStock = createCard("LOW STOCK ITEMS", lowStockLabel, "#ef4444");
        VBox cardEmployees = createCard("TOTAL EMPLOYEES", totalEmployeesLabel, "#0ea5e9");

        HBox.setHgrow(cardToday, Priority.ALWAYS);
        HBox.setHgrow(cardMonth, Priority.ALWAYS);
        HBox.setHgrow(cardProducts, Priority.ALWAYS);
        HBox.setHgrow(cardLowStock, Priority.ALWAYS);
        HBox.setHgrow(cardEmployees, Priority.ALWAYS);

        row.getChildren().addAll(cardToday, cardMonth, cardProducts, cardLowStock, cardEmployees);
        return row;
    }

    private VBox createCard(String title, Label valueLabel, String accentColor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setStyle(card.getStyle() + "; -fx-border-color: " + accentColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 8;");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        valueLabel.getStyleClass().add("card-value");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private HBox createChartsRow() {
        HBox row = new HBox(15);

        // Last 7 days Line Chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Sales ($)");
        salesLineChart = new LineChart<>(xAxis, yAxis);
        salesLineChart.setTitle("Sales Trend (Last 7 Days)");
        salesLineChart.setLegendVisible(false);
        salesLineChart.getStyleClass().add("card");
        salesLineChart.setPrefHeight(320);

        // Top 5 Products Bar Chart
        CategoryAxis catAxis = new CategoryAxis();
        catAxis.setLabel("Product");
        NumberAxis qtyAxis = new NumberAxis();
        qtyAxis.setLabel("Units Sold");
        topProductsChart = new BarChart<>(catAxis, qtyAxis);
        topProductsChart.setTitle("Top 5 Selling Products");
        topProductsChart.setLegendVisible(false);
        topProductsChart.getStyleClass().add("card");
        topProductsChart.setPrefHeight(320);

        HBox.setHgrow(salesLineChart, Priority.ALWAYS);
        HBox.setHgrow(topProductsChart, Priority.ALWAYS);

        row.getChildren().addAll(salesLineChart, topProductsChart);
        return row;
    }

    private VBox createLowStockSection() {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");

        HBox header = new HBox();
        Label title = new Label("⚠️ Low Stock & Reorder Alerts");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manageStockBtn = new Button("Go to Stock Management →");
        manageStockBtn.getStyleClass().add("btn-secondary");
        manageStockBtn.setOnAction(e -> mainLayout.showStock());

        header.getChildren().addAll(title, spacer, manageStockBtn);

        lowStockTable = new TableView<>();
        lowStockTable.setPrefHeight(200);

        TableColumn<Product, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Product, String> colName = new TableColumn<>("Product Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(250);

        TableColumn<Product, String> colSku = new TableColumn<>("SKU / Barcode");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colSku.setPrefWidth(140);

        TableColumn<Product, Integer> colStock = new TableColumn<>("Current Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colStock.setPrefWidth(120);

        TableColumn<Product, Integer> colReorder = new TableColumn<>("Reorder Level");
        colReorder.setCellValueFactory(new PropertyValueFactory<>("reorderLevel"));
        colReorder.setPrefWidth(120);

        lowStockTable.getColumns().addAll(colId, colName, colSku, colStock, colReorder);

        box.getChildren().addAll(header, lowStockTable);
        return box;
    }

    private void loadDashboardDataAsync() {
        Task<Void> task = new Task<>() {
            private double todaySales;
            private double monthRevenue;
            private int totalProducts;
            private int lowStockCount;
            private int totalEmployees;
            private Map<String, Double> last7DaysSales;
            private List<Map<String, Object>> topSelling;
            private List<Product> lowStockList;

            @Override
            protected Void call() {
                todaySales = dashboardService.getTodaySalesTotal();
                monthRevenue = dashboardService.getCurrentMonthRevenue();
                totalProducts = dashboardService.getTotalProductCount();
                lowStockCount = dashboardService.getLowStockProductCount();
                totalEmployees = dashboardService.getTotalEmployeeCount();
                last7DaysSales = dashboardService.getLast7DaysSales();
                topSelling = dashboardService.getTopSellingProducts(5);
                lowStockList = dashboardService.getLowStockAlertList();
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    todaySalesLabel.setText("$" + String.format("%.2f", todaySales));
                    monthSalesLabel.setText("$" + String.format("%.2f", monthRevenue));
                    totalProductsLabel.setText(String.valueOf(totalProducts));
                    lowStockLabel.setText(String.valueOf(lowStockCount));
                    totalEmployeesLabel.setText(String.valueOf(totalEmployees));

                    // Populate 7-day Line Chart
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    last7DaysSales.forEach((date, total) -> {
                        // Extract short MM-dd
                        String label = date.length() > 5 ? date.substring(5) : date;
                        series.getData().add(new XYChart.Data<>(label, total));
                    });
                    salesLineChart.getData().clear();
                    salesLineChart.getData().add(series);

                    // Populate Top 5 Bar Chart
                    XYChart.Series<String, Number> topSeries = new XYChart.Series<>();
                    for (Map<String, Object> map : topSelling) {
                        String name = (String) map.get("productName");
                        if (name.length() > 14) name = name.substring(0, 12) + "..";
                        int qty = (int) map.get("totalQty");
                        topSeries.getData().add(new XYChart.Data<>(name, qty));
                    }
                    topProductsChart.getData().clear();
                    topProductsChart.getData().add(topSeries);

                    // Populate Low Stock Table
                    lowStockTable.getItems().setAll(lowStockList);
                });
            }
        };

        new Thread(task).start();
    }
}
