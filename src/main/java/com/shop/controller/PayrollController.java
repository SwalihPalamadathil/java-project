package com.shop.controller;

import com.shop.model.Employee;
import com.shop.model.SalaryRecord;
import com.shop.service.PayrollService;
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

import java.io.File;
import java.util.List;

/**
 * Controller for Monthly Payroll processing, deduction formulas, payment marking, and payslip exports.
 */
public class PayrollController {

    private final PayrollService payrollService = new PayrollService();

    private TableView<SalaryRecord> salaryTable;
    private TextField monthYearField;

    public Node getView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));

        // 1. Toolbar
        HBox toolbar = createToolbar();

        // 2. Table
        salaryTable = createTable();
        VBox.setVgrow(salaryTable, Priority.ALWAYS);

        root.getChildren().addAll(toolbar, salaryTable);
        refreshData();

        return root;
    }

    private HBox createToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("card");
        bar.setPadding(new Insets(12));

        Label label = new Label("Select Month (YYYY-MM):");
        label.setStyle("-fx-font-weight: bold;");

        monthYearField = new TextField(DateUtil.currentMonthYear());
        monthYearField.setPrefWidth(110);
        monthYearField.textProperty().addListener((obs, o, n) -> refreshData());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button calculateBtn = new Button("🧮 Compute Monthly Payroll for Employee");
        calculateBtn.getStyleClass().add("btn-primary");
        calculateBtn.setOnAction(e -> openComputeSalaryDialog());

        bar.getChildren().addAll(label, monthYearField, spacer, calculateBtn);
        return bar;
    }

    private TableView<SalaryRecord> createTable() {
        TableView<SalaryRecord> tv = new TableView<>();

        TableColumn<SalaryRecord, String> colMonth = new TableColumn<>("Month");
        colMonth.setCellValueFactory(new PropertyValueFactory<>("monthYear"));
        colMonth.setPrefWidth(90);

        TableColumn<SalaryRecord, String> colEmp = new TableColumn<>("Employee Name");
        colEmp.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colEmp.setPrefWidth(190);

        TableColumn<SalaryRecord, Double> colBase = new TableColumn<>("Base Salary");
        colBase.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        colBase.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colBase.setPrefWidth(100);

        TableColumn<SalaryRecord, Double> colAbsence = new TableColumn<>("Absence Ded.");
        colAbsence.setCellValueFactory(new PropertyValueFactory<>("absenceDeduction"));
        colAbsence.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "-$" + String.format("%.2f", val));
            }
        });
        colAbsence.setPrefWidth(100);

        TableColumn<SalaryRecord, Double> colBonus = new TableColumn<>("Overtime/Bonus");
        colBonus.setCellValueFactory(cellData -> {
            SalaryRecord sr = cellData.getValue();
            return new javafx.beans.property.SimpleDoubleProperty(sr.getOvertimePay() + sr.getBonus()).asObject();
        });
        colBonus.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "+$" + String.format("%.2f", val));
            }
        });
        colBonus.setPrefWidth(110);

        TableColumn<SalaryRecord, Double> colAdvance = new TableColumn<>("Advance Ded.");
        colAdvance.setCellValueFactory(new PropertyValueFactory<>("advanceDeduction"));
        colAdvance.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "-$" + String.format("%.2f", val));
            }
        });
        colAdvance.setPrefWidth(100);

        TableColumn<SalaryRecord, Double> colNet = new TableColumn<>("Net Salary");
        colNet.setCellValueFactory(new PropertyValueFactory<>("netSalary"));
        colNet.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colNet.setPrefWidth(110);

        TableColumn<SalaryRecord, String> colStatus = new TableColumn<>("Payment Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(status);
                    if ("PAID".equalsIgnoreCase(status)) lbl.getStyleClass().add("badge-success");
                    else lbl.getStyleClass().add("badge-warning");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(110);

        TableColumn<SalaryRecord, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(210);
        colActions.setCellFactory(c -> new TableCell<>() {
            private final Button payBtn = new Button("Mark Paid");
            private final Button slipBtn = new Button("Payslip PDF");
            private final HBox pane = new HBox(6, payBtn, slipBtn);

            {
                payBtn.getStyleClass().add("btn-success");
                payBtn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");
                slipBtn.getStyleClass().add("btn-primary");
                slipBtn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");

                payBtn.setOnAction(e -> {
                    SalaryRecord sr = getTableView().getItems().get(getIndex());
                    handleMarkPaid(sr);
                });

                slipBtn.setOnAction(e -> {
                    SalaryRecord sr = getTableView().getItems().get(getIndex());
                    handleDownloadPayslip(sr);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SalaryRecord sr = getTableView().getItems().get(getIndex());
                    payBtn.setDisable("PAID".equalsIgnoreCase(sr.getPaymentStatus()));
                    setGraphic(pane);
                }
            }
        });

        tv.getColumns().addAll(colMonth, colEmp, colBase, colAbsence, colBonus, colAdvance, colNet, colStatus, colActions);
        return tv;
    }

    private void refreshData() {
        String ym = monthYearField.getText().trim();
        List<SalaryRecord> list = payrollService.getSalaryRecordsByMonth(ym);
        salaryTable.setItems(FXCollections.observableArrayList(list));
    }

    private void openComputeSalaryDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Compute Monthly Payroll Record");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Employee> empCombo = new ComboBox<>(FXCollections.observableArrayList(payrollService.getActiveEmployees()));
        empCombo.setPrefWidth(240);

        TextField ymField = new TextField(monthYearField.getText().trim());
        TextField otField = new TextField("0.00");
        TextField bonusField = new TextField("0.00");
        TextField advanceField = new TextField("0.00");
        TextField remarksField = new TextField();

        Label previewNetLabel = new Label("Net: $0.00");
        previewNetLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");

        Runnable updatePreview = () -> {
            Employee emp = empCombo.getValue();
            if (emp == null) return;
            try {
                double ot = Double.parseDouble(otField.getText().isEmpty() ? "0" : otField.getText());
                double bonus = Double.parseDouble(bonusField.getText().isEmpty() ? "0" : bonusField.getText());
                double adv = Double.parseDouble(advanceField.getText().isEmpty() ? "0" : advanceField.getText());
                SalaryRecord sr = payrollService.calculateMonthlySalary(emp.getId(), ymField.getText().trim(), ot, bonus, adv, remarksField.getText());
                previewNetLabel.setText("Estimated Net: $" + String.format("%.2f", sr.getNetSalary())
                        + " (Absence Deductions: -$" + String.format("%.2f", sr.getAbsenceDeduction()) + ")");
            } catch (Exception ignored) {}
        };

        empCombo.valueProperty().addListener((obs, o, n) -> updatePreview.run());
        otField.textProperty().addListener((obs, o, n) -> updatePreview.run());
        bonusField.textProperty().addListener((obs, o, n) -> updatePreview.run());
        advanceField.textProperty().addListener((obs, o, n) -> updatePreview.run());

        grid.add(new Label("Employee:*"), 0, 0);
        grid.add(empCombo, 1, 0);
        grid.add(new Label("Month (YYYY-MM):*"), 0, 1);
        grid.add(ymField, 1, 1);
        grid.add(new Label("Overtime Pay ($):"), 0, 2);
        grid.add(otField, 1, 2);
        grid.add(new Label("Performance Bonus ($):"), 0, 3);
        grid.add(bonusField, 1, 3);
        grid.add(new Label("Advance / Loan Deduction ($):"), 0, 4);
        grid.add(advanceField, 1, 4);
        grid.add(new Label("Remarks:"), 0, 5);
        grid.add(remarksField, 1, 5);
        grid.add(previewNetLabel, 1, 6);

        Button saveBtn = new Button("Calculate & Save Payroll");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            Employee emp = empCombo.getValue();
            if (emp == null) {
                AlertUtil.showError("Validation Error", "Please select an employee.");
                return;
            }
            try {
                double ot = Double.parseDouble(otField.getText().trim());
                double bonus = Double.parseDouble(bonusField.getText().trim());
                double adv = Double.parseDouble(advanceField.getText().trim());
                SalaryRecord sr = payrollService.calculateMonthlySalary(emp.getId(), ymField.getText().trim(), ot, bonus, adv, remarksField.getText().trim());
                payrollService.saveSalaryRecord(sr);
                AlertUtil.showInfo("Success", "Payroll record for " + emp.getName() + " calculated and saved!");
                dialog.close();
                refreshData();
            } catch (Exception ex) {
                AlertUtil.showError("Calculation Error", ex.getMessage());
            }
        });

        grid.add(saveBtn, 1, 7);

        Scene scene = new Scene(grid, 520, 350);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void handleMarkPaid(SalaryRecord sr) {
        if (AlertUtil.showConfirmation("Confirm Payment", "Mark salary of $" + String.format("%.2f", sr.getNetSalary())
                + " for " + sr.getEmployeeName() + " as PAID?")) {
            payrollService.markSalaryPaid(sr.getId(), "BANK_TRANSFER");
            AlertUtil.showInfo("Success", "Salary marked as PAID.");
            refreshData();
        }
    }

    private void handleDownloadPayslip(SalaryRecord sr) {
        try {
            File payslipDir = new File("payslips");
            if (!payslipDir.exists()) payslipDir.mkdirs();

            File file = new File(payslipDir, "Payslip_" + sr.getEmployeeName().replace(" ", "_") + "_" + sr.getMonthYear() + ".pdf");
            payrollService.generatePayslip(sr.getId(), file);
            AlertUtil.showInfo("Payslip Generated", "Payslip PDF saved to: " + file.getAbsolutePath());
        } catch (Exception ex) {
            AlertUtil.showError("Error", ex.getMessage());
        }
    }
}
