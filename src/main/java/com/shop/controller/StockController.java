package com.shop.controller;

import com.shop.model.Product;
import com.shop.model.StockMovement;
import com.shop.service.ProductService;
import com.shop.service.StockService;
import com.shop.util.AlertUtil;
import com.shop.util.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for Stock Management: Purchases, Adjustments, Valuations, and Audit Logs.
 */
public class StockController {

    private final ProductService productService = new ProductService();
    private final StockService stockService = new StockService();

    private TableView<Product> stockTable;
    private TableView<StockMovement> movementTable;
    private Label valuationLabel;

    public Node getView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));

        // 1. Top Summary Bar
        HBox topBar = createTopBar();

        // 2. Tabs: (Tab 1: Inventory Stock Levels, Tab 2: Stock Movement Audit Log)
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab stockTab = new Tab("📦 Current Inventory & Stock", createStockTabContent());
        Tab movementTab = new Tab("📜 Movement History & Audit Log", createMovementTabContent());

        tabPane.getTabs().addAll(stockTab, movementTab);
        root.getChildren().addAll(topBar, tabPane);

        refreshData();
        return root;
    }

    private HBox createTopBar() {
        HBox bar = new HBox(15);
        bar.getStyleClass().add("card");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 20, 12, 20));

        valuationLabel = new Label("Total Inventory Valuation: $0.00");
        valuationLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button stockInBtn = new Button("📥 + Purchase Stock-In");
        stockInBtn.getStyleClass().add("btn-success");
        stockInBtn.setOnAction(e -> openStockInDialog());

        Button adjustBtn = new Button("⚠️ Stock Adjustment / Damage");
        adjustBtn.getStyleClass().add("btn-warning");
        adjustBtn.setOnAction(e -> openAdjustmentDialog());

        bar.getChildren().addAll(valuationLabel, spacer, stockInBtn, adjustBtn);
        return bar;
    }

    private VBox createStockTabContent() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10, 0, 0, 0));

        stockTable = new TableView<>();
        VBox.setVgrow(stockTable, Priority.ALWAYS);

        TableColumn<Product, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Product, String> colName = new TableColumn<>("Product Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(250);

        TableColumn<Product, String> colSku = new TableColumn<>("SKU / Barcode");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colSku.setPrefWidth(140);

        TableColumn<Product, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCat.setPrefWidth(140);

        TableColumn<Product, Double> colCost = new TableColumn<>("Unit Cost");
        colCost.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colCost.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colCost.setPrefWidth(90);

        TableColumn<Product, Integer> colStock = new TableColumn<>("Current Qty");
        colStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colStock.setPrefWidth(100);

        TableColumn<Product, Integer> colReorder = new TableColumn<>("Reorder Level");
        colReorder.setCellValueFactory(new PropertyValueFactory<>("reorderLevel"));
        colReorder.setPrefWidth(100);

        TableColumn<Product, String> colStatus = new TableColumn<>("Health Status");
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStockStatus()));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.replace("_", " "));
                    if ("IN_STOCK".equals(item)) lbl.getStyleClass().add("badge-success");
                    else if ("LOW_STOCK".equals(item)) lbl.getStyleClass().add("badge-warning");
                    else lbl.getStyleClass().add("badge-danger");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(120);

        TableColumn<Product, String> colValuation = new TableColumn<>("Stock Value");
        colValuation.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            return new SimpleStringProperty("$" + String.format("%.2f", p.getCurrentStock() * p.getPurchasePrice()));
        });
        colValuation.setPrefWidth(120);

        stockTable.getColumns().addAll(colId, colName, colSku, colCat, colCost, colStock, colReorder, colStatus, colValuation);
        box.getChildren().add(stockTable);
        return box;
    }

    private VBox createMovementTabContent() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10, 0, 0, 0));

        movementTable = new TableView<>();
        VBox.setVgrow(movementTable, Priority.ALWAYS);

        TableColumn<StockMovement, Integer> colId = new TableColumn<>("#");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(45);

        TableColumn<StockMovement, String> colProd = new TableColumn<>("Product");
        colProd.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colProd.setPrefWidth(220);

        TableColumn<StockMovement, String> colType = new TableColumn<>("Movement Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(type);
                    if (type.contains("IN")) lbl.getStyleClass().add("badge-success");
                    else if (type.contains("DAMAGE") || type.contains("LOSS")) lbl.getStyleClass().add("badge-danger");
                    else lbl.getStyleClass().add("badge-warning");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colType.setPrefWidth(140);

        TableColumn<StockMovement, Integer> colQty = new TableColumn<>("Quantity");
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQty.setPrefWidth(80);

        TableColumn<StockMovement, Integer> colBefore = new TableColumn<>("Before");
        colBefore.setCellValueFactory(new PropertyValueFactory<>("stockBefore"));
        colBefore.setPrefWidth(70);

        TableColumn<StockMovement, Integer> colAfter = new TableColumn<>("After");
        colAfter.setCellValueFactory(new PropertyValueFactory<>("stockAfter"));
        colAfter.setPrefWidth(70);

        TableColumn<StockMovement, String> colRef = new TableColumn<>("Reference / Bill #");
        colRef.setCellValueFactory(new PropertyValueFactory<>("referenceNumber"));
        colRef.setPrefWidth(130);

        TableColumn<StockMovement, String> colReason = new TableColumn<>("Notes / Reason");
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colReason.setPrefWidth(200);

        TableColumn<StockMovement, String> colUser = new TableColumn<>("User");
        colUser.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        colUser.setPrefWidth(100);

        TableColumn<StockMovement, String> colDate = new TableColumn<>("Timestamp");
        colDate.setCellValueFactory(cellData -> {
            StockMovement sm = cellData.getValue();
            return new SimpleStringProperty(sm.getCreatedAt() != null ? DateUtil.formatDisplayDateTime(sm.getCreatedAt()) : "-");
        });
        colDate.setPrefWidth(160);

        movementTable.getColumns().addAll(colId, colProd, colType, colQty, colBefore, colAfter, colRef, colReason, colUser, colDate);
        box.getChildren().add(movementTable);
        return box;
    }

    private void refreshData() {
        List<Product> products = productService.getAllActiveProducts();
        stockTable.setItems(FXCollections.observableArrayList(products));

        double valuation = stockService.getInventoryValuation();
        valuationLabel.setText("Total Inventory Valuation: $" + String.format("%.2f", valuation));

        List<StockMovement> movements = stockService.getMovementHistory(200, 0);
        movementTable.setItems(FXCollections.observableArrayList(movements));
    }

    private void openStockInDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Process Incoming Stock (Purchase)");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<Product> prodCombo = new ComboBox<>(FXCollections.observableArrayList(productService.getAllActiveProducts()));
        prodCombo.setPrefWidth(300);

        TextField qtyField = new TextField("1");
        TextField costField = new TextField("0.00");
        TextField invoiceField = new TextField();
        invoiceField.setPromptText("e.g. SUP-INV-9821");
        TextField notesField = new TextField();
        notesField.setPromptText("Optional delivery notes");

        prodCombo.valueProperty().addListener((obs, oldP, newP) -> {
            if (newP != null) {
                costField.setText(String.format("%.2f", newP.getPurchasePrice()));
            }
        });

        grid.add(new Label("Select Product:*"), 0, 0);
        grid.add(prodCombo, 1, 0);
        grid.add(new Label("Quantity Received:*"), 0, 1);
        grid.add(qtyField, 1, 1);
        grid.add(new Label("Unit Cost ($):*"), 0, 2);
        grid.add(costField, 1, 2);
        grid.add(new Label("Supplier Bill / Invoice #:"), 0, 3);
        grid.add(invoiceField, 1, 3);
        grid.add(new Label("Notes:"), 0, 4);
        grid.add(notesField, 1, 4);

        Button saveBtn = new Button("Record Stock-In");
        saveBtn.getStyleClass().add("btn-success");
        saveBtn.setOnAction(e -> {
            Product p = prodCombo.getValue();
            if (p == null) {
                AlertUtil.showError("Validation Error", "Please select a product.");
                return;
            }
            try {
                int qty = Integer.parseInt(qtyField.getText());
                double cost = Double.parseDouble(costField.getText());
                stockService.processStockIn(p.getId(), qty, cost, invoiceField.getText(), notesField.getText());
                AlertUtil.showInfo("Success", "Stock-In recorded successfully! Inventory updated.");
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                AlertUtil.showError("Error", ex.getMessage());
            }
        });

        grid.add(saveBtn, 1, 5);

        Scene scene = new Scene(grid, 480, 290);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openAdjustmentDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Stock Adjustment / Damage Loss");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<Product> prodCombo = new ComboBox<>(FXCollections.observableArrayList(productService.getAllActiveProducts()));
        prodCombo.setPrefWidth(300);

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "DAMAGE_LOSS", "MANUAL_ADJUSTMENT_SUB", "MANUAL_ADJUSTMENT_ADD"
        ));
        typeCombo.setValue("DAMAGE_LOSS");

        TextField qtyField = new TextField("1");
        TextField reasonField = new TextField();
        reasonField.setPromptText("e.g. Expired, packaging broken, count discrepancy");

        grid.add(new Label("Select Product:*"), 0, 0);
        grid.add(prodCombo, 1, 0);
        grid.add(new Label("Adjustment Type:*"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Adjustment Quantity:*"), 0, 2);
        grid.add(qtyField, 1, 2);
        grid.add(new Label("Reason / Justification:*"), 0, 3);
        grid.add(reasonField, 1, 3);

        Button saveBtn = new Button("Apply Adjustment");
        saveBtn.getStyleClass().add("btn-warning");
        saveBtn.setOnAction(e -> {
            Product p = prodCombo.getValue();
            if (p == null) {
                AlertUtil.showError("Validation Error", "Please select a product.");
                return;
            }
            if (reasonField.getText().trim().isEmpty()) {
                AlertUtil.showError("Validation Error", "Please provide a reason for the adjustment.");
                return;
            }
            try {
                int qty = Integer.parseInt(qtyField.getText());
                stockService.processStockAdjustment(p.getId(), typeCombo.getValue(), qty, reasonField.getText().trim());
                AlertUtil.showInfo("Success", "Inventory adjusted and movement logged.");
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                AlertUtil.showError("Error", ex.getMessage());
            }
        });

        grid.add(saveBtn, 1, 4);

        Scene scene = new Scene(grid, 480, 260);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
