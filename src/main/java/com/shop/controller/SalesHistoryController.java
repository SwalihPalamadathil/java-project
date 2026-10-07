package com.shop.controller;

import com.shop.model.Sale;
import com.shop.model.SaleItem;
import com.shop.service.SalesService;
import com.shop.util.AlertUtil;
import com.shop.util.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * Controller for viewing past Sales Invoices, inspecting item breakdowns, processing refunds, and reprinting PDFs.
 */
public class SalesHistoryController {

    private final SalesService salesService = new SalesService();

    private TableView<Sale> salesTable;
    private TextField searchField;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Pagination pagination;

    private static final int PAGE_SIZE = 15;

    public Node getView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));

        // 1. Filter toolbar
        HBox toolbar = createToolbar();

        // 2. Sales Table
        salesTable = createTable();
        VBox.setVgrow(salesTable, Priority.ALWAYS);

        // 3. Pagination
        pagination = new Pagination(1, 0);
        pagination.setPageFactory(this::createPage);

        root.getChildren().addAll(toolbar, salesTable, pagination);
        refreshData();

        return root;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("card");
        toolbar.setPadding(new Insets(12));

        searchField = new TextField();
        searchField.setPromptText("Invoice #, customer name, phone...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((obs, o, n) -> refreshData());

        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("From Date");
        startDatePicker.valueProperty().addListener((obs, o, n) -> refreshData());

        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("To Date");
        endDatePicker.valueProperty().addListener((obs, o, n) -> refreshData());

        Button resetBtn = new Button("Reset Filters");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> {
            searchField.clear();
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            refreshData();
        });

        toolbar.getChildren().addAll(searchField, startDatePicker, endDatePicker, resetBtn);
        return toolbar;
    }

    private TableView<Sale> createTable() {
        TableView<Sale> tv = new TableView<>();

        TableColumn<Sale, String> colInv = new TableColumn<>("Invoice #");
        colInv.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        colInv.setPrefWidth(160);

        TableColumn<Sale, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(cellData -> {
            Sale s = cellData.getValue();
            return new SimpleStringProperty(s.getCreatedAt() != null ? DateUtil.formatDisplayDateTime(s.getCreatedAt()) : "-");
        });
        colDate.setPrefWidth(170);

        TableColumn<Sale, String> colCust = new TableColumn<>("Customer");
        colCust.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getCustomerName();
            return new SimpleStringProperty(name != null && !name.isEmpty() ? name : "Walk-in");
        });
        colCust.setPrefWidth(150);

        TableColumn<Sale, String> colPay = new TableColumn<>("Payment");
        colPay.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colPay.setPrefWidth(90);

        TableColumn<Sale, Double> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colSubtotal.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colSubtotal.setPrefWidth(90);

        TableColumn<Sale, Double> colTax = new TableColumn<>("Tax");
        colTax.setCellValueFactory(new PropertyValueFactory<>("taxAmount"));
        colTax.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colTax.setPrefWidth(80);

        TableColumn<Sale, Double> colTotal = new TableColumn<>("Grand Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colTotal.setPrefWidth(100);

        TableColumn<Sale, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(status);
                    if ("COMPLETED".equals(status)) lbl.getStyleClass().add("badge-success");
                    else lbl.getStyleClass().add("badge-danger");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(110);

        TableColumn<Sale, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(210);
        colActions.setCellFactory(c -> new TableCell<>() {
            private final Button viewBtn = new Button("Items");
            private final Button pdfBtn = new Button("PDF");
            private final Button refundBtn = new Button("Refund");
            private final HBox pane = new HBox(6, viewBtn, pdfBtn, refundBtn);

            {
                viewBtn.getStyleClass().add("btn-secondary");
                viewBtn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");
                pdfBtn.getStyleClass().add("btn-primary");
                pdfBtn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");
                refundBtn.getStyleClass().add("btn-danger");
                refundBtn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");

                viewBtn.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    showSaleItemsDialog(sale);
                });

                pdfBtn.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleExportPdf(sale);
                });

                refundBtn.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleRefund(sale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Sale s = getTableView().getItems().get(getIndex());
                    refundBtn.setDisable("REFUNDED".equalsIgnoreCase(s.getStatus()));
                    setGraphic(pane);
                }
            }
        });

        tv.getColumns().addAll(colInv, colDate, colCust, colPay, colSubtotal, colTax, colTotal, colStatus, colActions);
        return tv;
    }

    private Node createPage(int pageIndex) {
        String q = searchField.getText();
        String start = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : null;
        String end = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : null;

        List<Sale> sales = salesService.searchSales(q, start, end, pageIndex + 1, PAGE_SIZE);
        salesTable.setItems(FXCollections.observableArrayList(sales));
        return new VBox();
    }

    private void refreshData() {
        String q = searchField.getText();
        String start = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : null;
        String end = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : null;

        int totalCount = salesService.countSales(q, start, end);
        int pageCount = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        createPage(0);
    }

    private void showSaleItemsDialog(Sale sale) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Items for Invoice: " + sale.getInvoiceNumber());

        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        List<SaleItem> items = salesService.getSaleItems(sale.getId());
        TableView<SaleItem> itemTable = new TableView<>(FXCollections.observableArrayList(items));

        TableColumn<SaleItem, String> colName = new TableColumn<>("Product");
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colName.setPrefWidth(220);

        TableColumn<SaleItem, Double> colPrice = new TableColumn<>("Price");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colPrice.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colPrice.setPrefWidth(90);

        TableColumn<SaleItem, Integer> colQty = new TableColumn<>("Qty");
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQty.setPrefWidth(60);

        TableColumn<SaleItem, Double> colTax = new TableColumn<>("Tax");
        colTax.setCellValueFactory(new PropertyValueFactory<>("taxAmount"));
        colTax.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colTax.setPrefWidth(80);

        TableColumn<SaleItem, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colTotal.setPrefWidth(90);

        itemTable.getColumns().addAll(colName, colPrice, colQty, colTax, colTotal);
        box.getChildren().add(itemTable);

        Scene scene = new Scene(box, 560, 320);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void handleExportPdf(Sale sale) {
        try {
            File invoiceDir = new File("invoices");
            if (!invoiceDir.exists()) invoiceDir.mkdirs();

            File file = new File(invoiceDir, sale.getInvoiceNumber() + ".pdf");
            salesService.exportInvoicePdf(sale, file);
            AlertUtil.showInfo("Invoice PDF Generated", "Invoice PDF saved to: " + file.getAbsolutePath());
        } catch (Exception ex) {
            AlertUtil.showError("PDF Generation Error", ex.getMessage());
        }
    }

    private void handleRefund(Sale sale) {
        if ("REFUNDED".equalsIgnoreCase(sale.getStatus())) {
            AlertUtil.showWarning("Already Refunded", "This sale invoice is already marked as REFUNDED.");
            return;
        }

        if (AlertUtil.showConfirmation("Confirm Refund", "Are you sure you want to process a refund for "
                + sale.getInvoiceNumber() + "? All items will be returned to inventory.")) {
            try {
                salesService.processReturnAndRefund(sale.getId(), "Customer request / Return");
                AlertUtil.showInfo("Success", "Sale refunded and items restocked successfully.");
                refreshData();
            } catch (Exception ex) {
                AlertUtil.showError("Refund Error", ex.getMessage());
            }
        }
    }
}
