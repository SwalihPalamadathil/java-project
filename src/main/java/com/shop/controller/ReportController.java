package com.shop.controller;

import com.shop.service.ReportService;
import com.shop.util.AlertUtil;
import com.shop.util.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for generating business analytics reports (Sales, Profit & Margins, Stock Valuation, Payroll)
 * with one-click export to PDF and CSV.
 */
public class ReportController {

    private final ReportService reportService = new ReportService();

    private ComboBox<String> reportTypeCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private TableView<List<String>> reportTable;

    private List<String> currentHeaders = new ArrayList<>();
    private List<List<String>> currentRows = new ArrayList<>();

    public Node getView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));

        // 1. Selector Bar
        HBox toolbar = createToolbar();

        // 2. Report Data Table
        reportTable = new TableView<>();
        VBox.setVgrow(reportTable, Priority.ALWAYS);

        root.getChildren().addAll(toolbar, reportTable);

        // Load default report (Sales Report)
        loadSelectedReport();

        return root;
    }

    private HBox createToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("card");
        bar.setPadding(new Insets(12));

        reportTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Sales Report", "Profit & Margin Analysis", "Inventory & Stock Valuation", "Employee Payroll Expense"
        ));
        reportTypeCombo.setValue("Sales Report");
        reportTypeCombo.valueProperty().addListener((obs, o, n) -> loadSelectedReport());

        startDatePicker = new DatePicker(LocalDate.now().minusDays(30));
        endDatePicker = new DatePicker(LocalDate.now());

        Button generateBtn = new Button("🔄 Generate Report");
        generateBtn.getStyleClass().add("btn-primary");
        generateBtn.setOnAction(e -> loadSelectedReport());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportPdfBtn = new Button("📄 Export PDF");
        exportPdfBtn.getStyleClass().add("btn-danger");
        exportPdfBtn.setOnAction(e -> handleExportPdf());

        Button exportCsvBtn = new Button("📊 Export CSV");
        exportCsvBtn.getStyleClass().add("btn-success");
        exportCsvBtn.setOnAction(e -> handleExportCsv());

        bar.getChildren().addAll(new Label("Report:"), reportTypeCombo,
                new Label("From:"), startDatePicker, new Label("To:"), endDatePicker,
                generateBtn, spacer, exportPdfBtn, exportCsvBtn);
        return bar;
    }

    private void loadSelectedReport() {
        String type = reportTypeCombo.getValue();
        String start = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "";
        String end = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "";

        if ("Sales Report".equals(type)) {
            currentHeaders = reportService.getSalesHeaders();
            currentRows = reportService.getSalesReportData(start, end);
        } else if ("Profit & Margin Analysis".equals(type)) {
            currentHeaders = reportService.getProfitHeaders();
            currentRows = reportService.getProfitReportData(start, end);
        } else if ("Inventory & Stock Valuation".equals(type)) {
            currentHeaders = reportService.getStockHeaders();
            currentRows = reportService.getStockReportData();
        } else if ("Employee Payroll Expense".equals(type)) {
            currentHeaders = reportService.getPayrollHeaders();
            currentRows = reportService.getPayrollReportData(null);
        }

        populateTable(currentHeaders, currentRows);
    }

    private void populateTable(List<String> headers, List<List<String>> rows) {
        reportTable.getColumns().clear();

        for (int i = 0; i < headers.size(); i++) {
            final int colIndex = i;
            TableColumn<List<String>, String> col = new TableColumn<>(headers.get(i));
            col.setCellValueFactory(data -> {
                List<String> row = data.getValue();
                if (colIndex < row.size()) {
                    return new SimpleStringProperty(row.get(colIndex));
                }
                return new SimpleStringProperty("-");
            });
            col.setPrefWidth(125);
            reportTable.getColumns().add(col);
        }

        reportTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void handleExportPdf() {
        if (currentRows.isEmpty()) {
            AlertUtil.showWarning("Export", "No data available to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report as PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));
        chooser.setInitialFileName(reportTypeCombo.getValue().replace(" ", "_") + "_" + DateUtil.todayString() + ".pdf");

        File file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                String subtitle = "Date Range: " + (startDatePicker.getValue() != null ? startDatePicker.getValue() : "Start")
                        + " to " + (endDatePicker.getValue() != null ? endDatePicker.getValue() : "Today");
                reportService.exportReportToPdf(reportTypeCombo.getValue(), subtitle, currentHeaders, currentRows, file);
                AlertUtil.showInfo("Export Complete", "Report successfully exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertUtil.showError("Export Error", e.getMessage());
            }
        }
    }

    private void handleExportCsv() {
        if (currentRows.isEmpty()) {
            AlertUtil.showWarning("Export", "No data available to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report as CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Spreadsheet (*.csv)", "*.csv"));
        chooser.setInitialFileName(reportTypeCombo.getValue().replace(" ", "_") + "_" + DateUtil.todayString() + ".csv");

        File file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                reportService.exportReportToCsv(currentHeaders, currentRows, file);
                AlertUtil.showInfo("Export Complete", "CSV report exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertUtil.showError("Export Error", e.getMessage());
            }
        }
    }
}
