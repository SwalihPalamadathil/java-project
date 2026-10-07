package com.shop.controller;

import com.shop.model.Category;
import com.shop.model.Product;
import com.shop.model.Supplier;
import com.shop.service.ProductService;
import com.shop.util.AlertUtil;
import com.shop.util.SessionManager;
import com.shop.util.Validator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller managing Product inventory catalog, categories, suppliers, and search filters.
 */
public class ProductController {

    private final ProductService productService = new ProductService();

    private TableView<Product> table;
    private TextField searchField;
    private ComboBox<Category> categoryFilterCombo;
    private ComboBox<String> stockStatusFilterCombo;
    private Pagination pagination;

    private static final int PAGE_SIZE = 15;

    public Node getView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));

        // 1. Toolbar & Filters
        HBox toolbar = createToolbar();

        // 2. Products Table
        table = createTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // 3. Pagination Controls
        pagination = new Pagination(1, 0);
        pagination.setPageFactory(this::createPage);

        root.getChildren().addAll(toolbar, table, pagination);
        loadCategories();
        refreshData();

        return root;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("card");
        toolbar.setPadding(new Insets(12));

        searchField = new TextField();
        searchField.setPromptText("Search by name, SKU, or brand...");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldV, newV) -> refreshData());

        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.setPromptText("All Categories");
        categoryFilterCombo.valueProperty().addListener((obs, o, n) -> refreshData());

        stockStatusFilterCombo = new ComboBox<>(FXCollections.observableArrayList("ALL", "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"));
        stockStatusFilterCombo.setValue("ALL");
        stockStatusFilterCombo.valueProperty().addListener((obs, o, n) -> refreshData());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Product");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openProductDialog(null));

        Button catBtn = new Button("Manage Categories");
        catBtn.getStyleClass().add("btn-secondary");
        catBtn.setOnAction(e -> openCategoriesDialog());

        Button supBtn = new Button("Manage Suppliers");
        supBtn.getStyleClass().add("btn-secondary");
        supBtn.setOnAction(e -> openSuppliersDialog());

        boolean isAdmin = SessionManager.getInstance().isAdmin();
        if (!isAdmin) {
            addBtn.setVisible(false);
            catBtn.setVisible(false);
            supBtn.setVisible(false);
        }

        toolbar.getChildren().addAll(searchField, categoryFilterCombo, stockStatusFilterCombo, spacer, addBtn, catBtn, supBtn);
        return toolbar;
    }

    private TableView<Product> createTable() {
        TableView<Product> tv = new TableView<>();

        TableColumn<Product, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(45);

        TableColumn<Product, String> colName = new TableColumn<>("Product Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(220);

        TableColumn<Product, String> colSku = new TableColumn<>("SKU / Barcode");
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colSku.setPrefWidth(130);

        TableColumn<Product, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCat.setPrefWidth(140);

        TableColumn<Product, String> colBrand = new TableColumn<>("Brand");
        colBrand.setCellValueFactory(new PropertyValueFactory<>("brand"));
        colBrand.setPrefWidth(110);

        TableColumn<Product, Double> colCost = new TableColumn<>("Cost Price");
        colCost.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colCost.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colCost.setPrefWidth(90);

        TableColumn<Product, Double> colPrice = new TableColumn<>("Sell Price");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colPrice.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colPrice.setPrefWidth(90);

        TableColumn<Product, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colStock.setPrefWidth(70);

        TableColumn<Product, String> colStatus = new TableColumn<>("Status");
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
        colStatus.setPrefWidth(110);

        TableColumn<Product, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(150);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox pane = new HBox(6, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");

                editBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    openProductDialog(p);
                });

                delBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || !SessionManager.getInstance().isAdmin()) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        tv.getColumns().addAll(colId, colName, colSku, colCat, colBrand, colCost, colPrice, colStock, colStatus, colActions);
        return tv;
    }

    private Node createPage(int pageIndex) {
        Category cat = categoryFilterCombo.getValue();
        Integer catId = (cat != null && cat.getId() > 0) ? cat.getId() : null;
        String status = stockStatusFilterCombo.getValue();
        String q = searchField.getText();

        List<Product> products = productService.searchProducts(q, catId, status, pageIndex + 1, PAGE_SIZE);
        table.setItems(FXCollections.observableArrayList(products));
        return new VBox(); // Dummy node since table is outside pagination container
    }

    private void refreshData() {
        Category cat = categoryFilterCombo.getValue();
        Integer catId = (cat != null && cat.getId() > 0) ? cat.getId() : null;
        String status = stockStatusFilterCombo.getValue();
        String q = searchField.getText();

        int totalCount = productService.countProducts(q, catId, status);
        int pageCount = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        createPage(0);
    }

    private void loadCategories() {
        List<Category> list = productService.getAllCategories();
        Category all = new Category(0, "All Categories", "");
        list.add(0, all);
        categoryFilterCombo.setItems(FXCollections.observableArrayList(list));
        categoryFilterCombo.setValue(all);
    }

    private void handleDeleteProduct(Product product) {
        if (AlertUtil.showConfirmation("Delete Product", "Are you sure you want to delete product '" + product.getName() + "'?")) {
            boolean ok = productService.deleteProduct(product.getId());
            if (ok) {
                AlertUtil.showInfo("Success", "Product removed successfully.");
                refreshData();
            } else {
                AlertUtil.showError("Error", "Failed to delete product.");
            }
        }
    }

    private void openProductDialog(Product existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add New Product" : "Edit Product: " + existing.getName());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField skuField = new TextField(existing != null ? existing.getSku() : "");
        TextField brandField = new TextField(existing != null && existing.getBrand() != null ? existing.getBrand() : "");
        ComboBox<Category> catCombo = new ComboBox<>(FXCollections.observableArrayList(productService.getAllCategories()));
        ComboBox<Supplier> supCombo = new ComboBox<>(FXCollections.observableArrayList(productService.getAllSuppliers()));
        ComboBox<String> unitCombo = new ComboBox<>(FXCollections.observableArrayList("pcs", "kg", "litre", "pack", "box"));
        unitCombo.setValue(existing != null ? existing.getUnit() : "pcs");

        TextField costField = new TextField(existing != null ? String.valueOf(existing.getPurchasePrice()) : "0.00");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getSellingPrice()) : "0.00");
        TextField taxField = new TextField(existing != null ? String.valueOf(existing.getTaxRate()) : "5.0");
        TextField stockField = new TextField(existing != null ? String.valueOf(existing.getCurrentStock()) : "0");
        stockField.setDisable(existing != null); // Stock can only be modified via Stock Management movements

        TextField reorderField = new TextField(existing != null ? String.valueOf(existing.getReorderLevel()) : "5");
        DatePicker expiryPicker = new DatePicker();
        if (existing != null && existing.getExpiryDate() != null && !existing.getExpiryDate().isEmpty()) {
            expiryPicker.setValue(LocalDate.parse(existing.getExpiryDate()));
        }
        TextArea descArea = new TextArea(existing != null && existing.getDescription() != null ? existing.getDescription() : "");
        descArea.setPrefRowCount(3);

        // Pre-select category and supplier
        if (existing != null) {
            catCombo.getItems().stream().filter(c -> c.getId() == existing.getCategoryId()).findFirst().ifPresent(catCombo::setValue);
            supCombo.getItems().stream().filter(s -> s.getId() == existing.getSupplierId()).findFirst().ifPresent(supCombo::setValue);
        }

        grid.add(new Label("Product Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("SKU / Barcode:*"), 0, 1);
        grid.add(skuField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(catCombo, 1, 2);
        grid.add(new Label("Brand:"), 0, 3);
        grid.add(brandField, 1, 3);
        grid.add(new Label("Supplier:"), 0, 4);
        grid.add(supCombo, 1, 4);
        grid.add(new Label("Unit:"), 0, 5);
        grid.add(unitCombo, 1, 5);

        grid.add(new Label("Cost Price ($):*"), 2, 0);
        grid.add(costField, 3, 0);
        grid.add(new Label("Selling Price ($):*"), 2, 1);
        grid.add(priceField, 3, 1);
        grid.add(new Label("Tax Rate (%):"), 2, 2);
        grid.add(taxField, 3, 2);
        grid.add(new Label("Opening Stock:"), 2, 3);
        grid.add(stockField, 3, 3);
        grid.add(new Label("Reorder Level:"), 2, 4);
        grid.add(reorderField, 3, 4);
        grid.add(new Label("Expiry Date:"), 2, 5);
        grid.add(expiryPicker, 3, 5);

        grid.add(new Label("Description:"), 0, 6);
        grid.add(descArea, 1, 6, 3, 1);

        Button saveBtn = new Button("Save Product");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                Product p = existing != null ? existing : new Product();
                p.setName(nameField.getText());
                p.setSku(skuField.getText());
                p.setBrand(brandField.getText());
                p.setCategoryId(catCombo.getValue() != null ? catCombo.getValue().getId() : 0);
                p.setSupplierId(supCombo.getValue() != null ? supCombo.getValue().getId() : 0);
                p.setUnit(unitCombo.getValue());
                p.setPurchasePrice(Double.parseDouble(costField.getText()));
                p.setSellingPrice(Double.parseDouble(priceField.getText()));
                p.setTaxRate(Double.parseDouble(taxField.getText()));
                if (existing == null) {
                    p.setCurrentStock(Integer.parseInt(stockField.getText()));
                }
                p.setReorderLevel(Integer.parseInt(reorderField.getText()));
                p.setExpiryDate(expiryPicker.getValue() != null ? expiryPicker.getValue().toString() : null);
                p.setDescription(descArea.getText());

                productService.saveProduct(p);
                AlertUtil.showInfo("Success", "Product saved successfully!");
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                AlertUtil.showError("Validation Error", ex.getMessage());
            }
        });

        grid.add(saveBtn, 3, 7);

        Scene scene = new Scene(grid, 650, 420);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openCategoriesDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Categories");

        VBox box = new VBox(10);
        box.setPadding(new Insets(15));

        TableView<Category> catTable = new TableView<>(FXCollections.observableArrayList(productService.getAllCategories()));
        TableColumn<Category, String> cName = new TableColumn<>("Category Name");
        cName.setCellValueFactory(new PropertyValueFactory<>("name"));
        cName.setPrefWidth(180);
        TableColumn<Category, String> cDesc = new TableColumn<>("Description");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        cDesc.setPrefWidth(250);
        catTable.getColumns().addAll(cName, cDesc);

        HBox form = new HBox(8);
        TextField tfName = new TextField();
        tfName.setPromptText("New Category Name");
        TextField tfDesc = new TextField();
        tfDesc.setPromptText("Description");
        Button btnAdd = new Button("Add");
        btnAdd.getStyleClass().add("btn-primary");

        btnAdd.setOnAction(e -> {
            if (tfName.getText().trim().isEmpty()) return;
            try {
                productService.saveCategory(new Category(tfName.getText().trim(), tfDesc.getText().trim()));
                catTable.setItems(FXCollections.observableArrayList(productService.getAllCategories()));
                tfName.clear();
                tfDesc.clear();
                loadCategories();
            } catch (Exception ex) {
                AlertUtil.showError("Error", ex.getMessage());
            }
        });

        form.getChildren().addAll(tfName, tfDesc, btnAdd);
        box.getChildren().addAll(catTable, form);

        Scene scene = new Scene(box, 500, 360);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openSuppliersDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Suppliers");

        VBox box = new VBox(10);
        box.setPadding(new Insets(15));

        TableView<Supplier> supTable = new TableView<>(FXCollections.observableArrayList(productService.getAllSuppliers()));
        TableColumn<Supplier, String> sName = new TableColumn<>("Company Name");
        sName.setCellValueFactory(new PropertyValueFactory<>("name"));
        sName.setPrefWidth(160);
        TableColumn<Supplier, String> sPhone = new TableColumn<>("Phone");
        sPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        sPhone.setPrefWidth(120);
        TableColumn<Supplier, String> sEmail = new TableColumn<>("Email");
        sEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        sEmail.setPrefWidth(160);
        supTable.getColumns().addAll(sName, sPhone, sEmail);

        HBox form = new HBox(8);
        TextField tfName = new TextField();
        tfName.setPromptText("Supplier Name");
        TextField tfPhone = new TextField();
        tfPhone.setPromptText("Phone");
        TextField tfEmail = new TextField();
        tfEmail.setPromptText("Email");
        Button btnAdd = new Button("Add");
        btnAdd.getStyleClass().add("btn-primary");

        btnAdd.setOnAction(e -> {
            if (tfName.getText().trim().isEmpty()) return;
            try {
                productService.saveSupplier(new Supplier(tfName.getText().trim(), "Contact", tfPhone.getText().trim(), tfEmail.getText().trim(), ""));
                supTable.setItems(FXCollections.observableArrayList(productService.getAllSuppliers()));
                tfName.clear();
                tfPhone.clear();
                tfEmail.clear();
            } catch (Exception ex) {
                AlertUtil.showError("Error", ex.getMessage());
            }
        });

        form.getChildren().addAll(tfName, tfPhone, tfEmail, btnAdd);
        box.getChildren().addAll(supTable, form);

        Scene scene = new Scene(box, 520, 360);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
