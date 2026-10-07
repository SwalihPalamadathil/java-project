package com.shop.controller;

import com.shop.model.Product;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import com.shop.service.ProductService;
import com.shop.service.SalesService;
import com.shop.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Point of Sale (POS) Billing, Cart calculation, and Checkout execution.
 */
public class POSController {

    private final ProductService productService = new ProductService();
    private final SalesService salesService = new SalesService();

    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();

    private TextField barcodeSearchField;
    private ComboBox<Product> quickProductCombo;
    private TableView<SaleItem> cartTable;

    private TextField customerNameField;
    private TextField customerPhoneField;
    private TextField overallDiscountField;
    private TextField amountTenderedField;

    private Label subtotalLabel;
    private Label taxLabel;
    private Label discountLabel;
    private Label grandTotalLabel;
    private Label changeDueLabel;

    private ToggleGroup paymentGroup;
    private RadioButton cashRadio;
    private RadioButton cardRadio;
    private RadioButton upiRadio;

    public Node getView() {
        HBox root = new HBox(15);
        root.setPadding(new Insets(10));

        // Left Side: Scanner, Search & Cart Table
        VBox leftPane = createCartSection();
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        // Right Side: Totals, Customer, Payment & Checkout Button
        VBox rightPane = createCheckoutSection();
        rightPane.setPrefWidth(380);

        root.getChildren().addAll(leftPane, rightPane);
        return root;
    }

    private VBox createCartSection() {
        VBox box = new VBox(12);

        // Barcode Scanner & Quick Select Bar
        HBox scanBar = new HBox(10);
        scanBar.getStyleClass().add("card");
        scanBar.setAlignment(Pos.CENTER_LEFT);
        scanBar.setPadding(new Insets(12));

        barcodeSearchField = new TextField();
        barcodeSearchField.setPromptText("📷 Scan barcode / enter SKU & press ENTER...");
        barcodeSearchField.setPrefWidth(300);
        barcodeSearchField.setOnAction(e -> handleBarcodeScan());

        quickProductCombo = new ComboBox<>(FXCollections.observableArrayList(productService.getAllActiveProducts()));
        quickProductCombo.setPromptText("Select product from catalog...");
        quickProductCombo.setPrefWidth(260);

        Button addComboBtn = new Button("Add to Cart");
        addComboBtn.getStyleClass().add("btn-primary");
        addComboBtn.setOnAction(e -> {
            Product p = quickProductCombo.getValue();
            if (p != null) {
                addProductToCart(p);
            }
        });

        Button clearCartBtn = new Button("Clear Cart");
        clearCartBtn.getStyleClass().add("btn-secondary");
        clearCartBtn.setOnAction(e -> {
            cartItems.clear();
            recalculateCart();
        });

        scanBar.getChildren().addAll(barcodeSearchField, quickProductCombo, addComboBtn, clearCartBtn);

        // Cart TableView
        cartTable = new TableView<>(cartItems);
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        TableColumn<SaleItem, String> colName = new TableColumn<>("Product Description");
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colName.setPrefWidth(220);

        TableColumn<SaleItem, String> colSku = new TableColumn<>("SKU");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colSku.setPrefWidth(120);

        TableColumn<SaleItem, Double> colPrice = new TableColumn<>("Price");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colPrice.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colPrice.setPrefWidth(85);

        TableColumn<SaleItem, Integer> colQty = new TableColumn<>("Qty");
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQty.setCellFactory(c -> new TableCell<>() {
            private final Button minusBtn = new Button("-");
            private final Label qtyLabel = new Label();
            private final Button plusBtn = new Button("+");
            private final HBox pane = new HBox(4, minusBtn, qtyLabel, plusBtn);

            {
                pane.setAlignment(Pos.CENTER);
                minusBtn.setStyle("-fx-padding: 2 6; -fx-font-weight: bold;");
                plusBtn.setStyle("-fx-padding: 2 6; -fx-font-weight: bold;");

                minusBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        getTableView().refresh();
                        recalculateCart();
                    } else {
                        cartItems.remove(item);
                        recalculateCart();
                    }
                });

                plusBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    // Verify inventory availability
                    Optional<Product> pOpt = productService.getProductById(item.getProductId());
                    if (pOpt.isPresent() && item.getQuantity() + 1 > pOpt.get().getCurrentStock()) {
                        AlertUtil.showWarning("Stock Limit", "Cannot add more. Only " + pOpt.get().getCurrentStock() + " in stock.");
                        return;
                    }
                    item.setQuantity(item.getQuantity() + 1);
                    getTableView().refresh();
                    recalculateCart();
                });
            }

            @Override
            protected void updateItem(Integer qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setGraphic(null);
                } else {
                    qtyLabel.setText(String.valueOf(qty));
                    setGraphic(pane);
                }
            }
        });
        colQty.setPrefWidth(110);

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

        TableColumn<SaleItem, Double> colTotal = new TableColumn<>("Line Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colTotal.setPrefWidth(95);

        TableColumn<SaleItem, Void> colRemove = new TableColumn<>("");
        colRemove.setPrefWidth(45);
        colRemove.setCellFactory(c -> new TableCell<>() {
            private final Button delBtn = new Button("✕");
            {
                delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-cursor: hand;");
                delBtn.setOnAction(e -> {
                    cartItems.remove(getIndex());
                    recalculateCart();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        cartTable.getColumns().addAll(colName, colSku, colPrice, colQty, colTax, colTotal, colRemove);
        box.getChildren().addAll(scanBar, cartTable);
        return box;
    }

    private VBox createCheckoutSection() {
        VBox box = new VBox(15);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));

        Label title = new Label("Checkout & Payment");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        // Customer Info
        GridPane custGrid = new GridPane();
        custGrid.setHgap(8);
        custGrid.setVgap(8);

        customerNameField = new TextField();
        customerNameField.setPromptText("Optional Customer Name");
        customerPhoneField = new TextField();
        customerPhoneField.setPromptText("Optional Phone Number");

        custGrid.add(new Label("Customer:"), 0, 0);
        custGrid.add(customerNameField, 1, 0);
        custGrid.add(new Label("Phone:"), 0, 1);
        custGrid.add(customerPhoneField, 1, 1);

        // Overall Discount Field
        HBox discBox = new HBox(8);
        discBox.setAlignment(Pos.CENTER_LEFT);
        overallDiscountField = new TextField("0.00");
        overallDiscountField.setPrefWidth(90);
        overallDiscountField.textProperty().addListener((obs, o, n) -> recalculateCart());
        discBox.getChildren().addAll(new Label("Discount ($):"), overallDiscountField);

        // Totals Breakdown
        VBox totalsBox = new VBox(6);
        totalsBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 12; -fx-background-radius: 8;");

        subtotalLabel = new Label("$0.00");
        taxLabel = new Label("$0.00");
        discountLabel = new Label("-$0.00");
        grandTotalLabel = new Label("$0.00");
        grandTotalLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");

        totalsBox.getChildren().addAll(
                createTotalRow("Subtotal:", subtotalLabel),
                createTotalRow("Tax Total:", taxLabel),
                createTotalRow("Discount:", discountLabel),
                new Label("──────────────────────────────────"),
                createTotalRow("GRAND TOTAL:", grandTotalLabel)
        );

        // Payment Method Selector
        paymentGroup = new ToggleGroup();
        cashRadio = new RadioButton("Cash");
        cashRadio.setToggleGroup(paymentGroup);
        cashRadio.setSelected(true);

        cardRadio = new RadioButton("Card");
        cardRadio.setToggleGroup(paymentGroup);

        upiRadio = new RadioButton("UPI");
        upiRadio.setToggleGroup(paymentGroup);

        HBox payMethods = new HBox(12, cashRadio, cardRadio, upiRadio);
        payMethods.setAlignment(Pos.CENTER_LEFT);

        // Tendered & Change
        HBox tenderedBox = new HBox(8);
        tenderedBox.setAlignment(Pos.CENTER_LEFT);
        amountTenderedField = new TextField("0.00");
        amountTenderedField.setPrefWidth(100);
        amountTenderedField.textProperty().addListener((obs, o, n) -> calculateChange());

        changeDueLabel = new Label("$0.00");
        changeDueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #16a34a;");

        tenderedBox.getChildren().addAll(new Label("Tendered ($):"), amountTenderedField, new Label("Change:"), changeDueLabel);

        // Checkout Button
        Button checkoutBtn = new Button("💳 Complete Sale & Print Invoice");
        checkoutBtn.getStyleClass().add("btn-success");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setStyle("-fx-font-size: 14px; -fx-padding: 12;");
        checkoutBtn.setOnAction(e -> handleCheckout());

        box.getChildren().addAll(title, custGrid, discBox, totalsBox, new Label("Payment Method:"), payMethods, tenderedBox, checkoutBtn);
        return box;
    }

    private HBox createTotalRow(String label, Label valueLabel) {
        HBox row = new HBox();
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: 600; -fx-text-fill: #475569;");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(l, spacer, valueLabel);
        return row;
    }

    private void handleBarcodeScan() {
        String sku = barcodeSearchField.getText();
        if (sku == null || sku.trim().isEmpty()) return;

        Optional<Product> opt = productService.getProductBySku(sku.trim());
        if (opt.isPresent()) {
            addProductToCart(opt.get());
            barcodeSearchField.clear();
        } else {
            AlertUtil.showWarning("Not Found", "No product found with SKU/Barcode: " + sku);
        }
    }

    private void addProductToCart(Product p) {
        if (p.getCurrentStock() <= 0) {
            AlertUtil.showError("Out of Stock", "Product '" + p.getName() + "' is out of stock!");
            return;
        }

        // Check if already in cart
        for (SaleItem item : cartItems) {
            if (item.getProductId() == p.getId()) {
                if (item.getQuantity() + 1 > p.getCurrentStock()) {
                    AlertUtil.showWarning("Stock Limit", "Cannot add more. Only " + p.getCurrentStock() + " available.");
                    return;
                }
                item.setQuantity(item.getQuantity() + 1);
                cartTable.refresh();
                recalculateCart();
                return;
            }
        }

        // Add new item
        SaleItem item = new SaleItem(p.getId(), p.getName(), p.getSku(), p.getSellingPrice(), 1, p.getTaxRate(), 0.0);
        cartItems.add(item);
        recalculateCart();
    }

    private void recalculateCart() {
        double subtotal = 0;
        double tax = 0;

        for (SaleItem item : cartItems) {
            subtotal += (item.getUnitPrice() * item.getQuantity());
            tax += item.getTaxAmount();
        }

        double discount = 0;
        try {
            discount = Double.parseDouble(overallDiscountField.getText());
        } catch (Exception ignored) {}

        double grandTotal = Math.max(0, (subtotal + tax) - discount);

        subtotalLabel.setText("$" + String.format("%.2f", subtotal));
        taxLabel.setText("$" + String.format("%.2f", tax));
        discountLabel.setText("-$" + String.format("%.2f", discount));
        grandTotalLabel.setText("$" + String.format("%.2f", grandTotal));

        // Auto-fill amount tendered for cash if default
        if (Double.parseDouble(amountTenderedField.getText().isEmpty() ? "0" : amountTenderedField.getText()) < grandTotal) {
            amountTenderedField.setText(String.format("%.2f", grandTotal));
        }

        calculateChange();
    }

    private void calculateChange() {
        try {
            double grandTotal = Double.parseDouble(grandTotalLabel.getText().replace("$", ""));
            double tendered = Double.parseDouble(amountTenderedField.getText().trim());
            double change = Math.max(0, tendered - grandTotal);
            changeDueLabel.setText("$" + String.format("%.2f", change));
        } catch (Exception e) {
            changeDueLabel.setText("$0.00");
        }
    }

    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            AlertUtil.showError("Cart Empty", "Please add items to the cart before checkout.");
            return;
        }

        try {
            double subtotal = Double.parseDouble(subtotalLabel.getText().replace("$", ""));
            double tax = Double.parseDouble(taxLabel.getText().replace("$", ""));
            double discount = Double.parseDouble(discountLabel.getText().replace("-$", ""));
            double grandTotal = Double.parseDouble(grandTotalLabel.getText().replace("$", ""));
            double tendered = Double.parseDouble(amountTenderedField.getText().trim());

            if (tendered < grandTotal) {
                AlertUtil.showError("Payment Error", "Amount tendered ($" + tendered + ") is less than Grand Total ($" + grandTotal + ").");
                return;
            }

            Sale sale = new Sale();
            sale.setCustomerName(customerNameField.getText().trim());
            sale.setCustomerPhone(customerPhoneField.getText().trim());
            sale.setSubtotal(subtotal);
            sale.setTaxAmount(tax);
            sale.setDiscountAmount(discount);
            sale.setGrandTotal(grandTotal);
            sale.setAmountTendered(tendered);

            String method = "CASH";
            if (cardRadio.isSelected()) method = "CARD";
            else if (upiRadio.isSelected()) method = "UPI";
            sale.setPaymentMethod(method);

            sale.setItems(List.copyOf(cartItems));

            // Process checkout in one database transaction
            Sale processed = salesService.processCheckout(sale);

            // Automatically export Invoice PDF
            File invoiceDir = new File("invoices");
            if (!invoiceDir.exists()) invoiceDir.mkdirs();

            File pdfFile = new File(invoiceDir, processed.getInvoiceNumber() + ".pdf");
            salesService.exportInvoicePdf(processed, pdfFile);

            AlertUtil.showInfo("Checkout Completed!",
                    "Invoice #" + processed.getInvoiceNumber() + " processed successfully!\n"
                    + "Change Due: $" + String.format("%.2f", processed.getChangeDue()) + "\n"
                    + "PDF Receipt saved to: " + pdfFile.getAbsolutePath());

            // Reset form
            cartItems.clear();
            customerNameField.clear();
            customerPhoneField.clear();
            overallDiscountField.setText("0.00");
            amountTenderedField.setText("0.00");
            recalculateCart();

            // Refresh quick product combo stock
            quickProductCombo.setItems(FXCollections.observableArrayList(productService.getAllActiveProducts()));

        } catch (Exception ex) {
            AlertUtil.showError("Transaction Error", ex.getMessage());
        }
    }
}
