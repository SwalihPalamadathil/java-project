package com.shop.controller;

import com.shop.model.Attendance;
import com.shop.model.Employee;
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
import javafx.scene.control.DatePicker;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Controller managing Employee directory, staff CRUD, and daily attendance marking.
 */
public class EmployeeController {

    private final PayrollService payrollService = new PayrollService();

    private TableView<Employee> employeeTable;
    private TableView<Attendance> attendanceTable;
    private DatePicker attendanceDatePicker;

    public Node getView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab empTab = new Tab("👥 Employee Directory", createEmployeeTabContent());
        Tab attTab = new Tab("📅 Daily Attendance Marker", createAttendanceTabContent());

        tabPane.getTabs().addAll(empTab, attTab);
        root.getChildren().add(tabPane);

        refreshEmployees();
        refreshAttendance();

        return root;
    }

    private VBox createEmployeeTabContent() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(10, 0, 0, 0));

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("card");
        toolbar.setPadding(new Insets(12));

        Label title = new Label("Staff & Employee Roster");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Employee");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openEmployeeDialog(null));

        toolbar.getChildren().addAll(title, spacer, addBtn);

        employeeTable = new TableView<>();
        VBox.setVgrow(employeeTable, Priority.ALWAYS);

        TableColumn<Employee, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(45);

        TableColumn<Employee, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(180);

        TableColumn<Employee, String> colPhone = new TableColumn<>("Phone Number");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPhone.setPrefWidth(130);

        TableColumn<Employee, String> colDesig = new TableColumn<>("Designation");
        colDesig.setCellValueFactory(new PropertyValueFactory<>("designation"));
        colDesig.setPrefWidth(150);

        TableColumn<Employee, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setPrefWidth(85);

        TableColumn<Employee, Double> colSalary = new TableColumn<>("Base Salary");
        colSalary.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        colSalary.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : "$" + String.format("%.2f", val));
            }
        });
        colSalary.setPrefWidth(110);

        TableColumn<Employee, String> colDate = new TableColumn<>("Joining Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("joiningDate"));
        colDate.setPrefWidth(110);

        TableColumn<Employee, String> colStatus = new TableColumn<>("Status");
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
                    if ("ACTIVE".equalsIgnoreCase(status)) lbl.getStyleClass().add("badge-success");
                    else lbl.getStyleClass().add("badge-danger");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(90);

        TableColumn<Employee, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(140);
        colActions.setCellFactory(c -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Deactivate");
            private final HBox pane = new HBox(6, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setStyle("-fx-padding: 3 8; -fx-font-size: 11px;");

                editBtn.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    openEmployeeDialog(emp);
                });

                delBtn.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    handleDeactivate(emp);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        employeeTable.getColumns().addAll(colId, colName, colPhone, colDesig, colRole, colSalary, colDate, colStatus, colActions);
        box.getChildren().addAll(toolbar, employeeTable);
        return box;
    }

    private VBox createAttendanceTabContent() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(10, 0, 0, 0));

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("card");
        toolbar.setPadding(new Insets(12));

        attendanceDatePicker = new DatePicker(LocalDate.now());
        attendanceDatePicker.valueProperty().addListener((obs, o, n) -> refreshAttendance());

        Button markBtn = new Button("✍️ Mark Attendance for Employee");
        markBtn.getStyleClass().add("btn-primary");
        markBtn.setOnAction(e -> openMarkAttendanceDialog());

        toolbar.getChildren().addAll(new Label("Select Date:"), attendanceDatePicker, markBtn);

        attendanceTable = new TableView<>();
        VBox.setVgrow(attendanceTable, Priority.ALWAYS);

        TableColumn<Attendance, Integer> colId = new TableColumn<>("#");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(45);

        TableColumn<Attendance, String> colEmp = new TableColumn<>("Employee Name");
        colEmp.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colEmp.setPrefWidth(220);

        TableColumn<Attendance, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setPrefWidth(120);

        TableColumn<Attendance, String> colStatus = new TableColumn<>("Attendance Status");
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
                    if ("PRESENT".equals(status)) lbl.getStyleClass().add("badge-success");
                    else if ("HALF_DAY".equals(status) || "LEAVE".equals(status)) lbl.getStyleClass().add("badge-warning");
                    else lbl.getStyleClass().add("badge-danger");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(140);

        TableColumn<Attendance, String> colRemarks = new TableColumn<>("Remarks / Notes");
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        colRemarks.setPrefWidth(250);

        attendanceTable.getColumns().addAll(colId, colEmp, colDate, colStatus, colRemarks);
        box.getChildren().addAll(toolbar, attendanceTable);
        return box;
    }

    private void refreshEmployees() {
        List<Employee> list = payrollService.getAllEmployees();
        employeeTable.setItems(FXCollections.observableArrayList(list));
    }

    private void refreshAttendance() {
        String d = attendanceDatePicker.getValue() != null ? attendanceDatePicker.getValue().toString() : DateUtil.todayString();
        List<Attendance> list = payrollService.getAttendanceByDate(d);
        attendanceTable.setItems(FXCollections.observableArrayList(list));
    }

    private void openEmployeeDialog(Employee existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add Employee" : "Edit Employee: " + existing.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField phoneField = new TextField(existing != null ? existing.getPhone() : "");
        TextField addrField = new TextField(existing != null ? existing.getAddress() : "");
        TextField desigField = new TextField(existing != null ? existing.getDesignation() : "");
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("ADMIN", "STAFF"));
        roleCombo.setValue(existing != null ? existing.getRole() : "STAFF");

        DatePicker joinDatePicker = new DatePicker(existing != null && existing.getJoiningDate() != null ? LocalDate.parse(existing.getJoiningDate()) : LocalDate.now());
        TextField salaryField = new TextField(existing != null ? String.valueOf(existing.getBaseSalary()) : "2000.00");
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));
        statusCombo.setValue(existing != null ? existing.getStatus() : "ACTIVE");

        grid.add(new Label("Full Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:*"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Address:"), 0, 2);
        grid.add(addrField, 1, 2);
        grid.add(new Label("Designation:*"), 0, 3);
        grid.add(desigField, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(roleCombo, 1, 4);
        grid.add(new Label("Joining Date:"), 0, 5);
        grid.add(joinDatePicker, 1, 5);
        grid.add(new Label("Base Salary ($):*"), 0, 6);
        grid.add(salaryField, 1, 6);
        grid.add(new Label("Status:"), 0, 7);
        grid.add(statusCombo, 1, 7);

        Button saveBtn = new Button("Save Employee");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                Employee emp = existing != null ? existing : new Employee();
                emp.setName(nameField.getText().trim());
                emp.setPhone(phoneField.getText().trim());
                emp.setAddress(addrField.getText().trim());
                emp.setDesignation(desigField.getText().trim());
                emp.setRole(roleCombo.getValue());
                emp.setJoiningDate(joinDatePicker.getValue().toString());
                emp.setBaseSalary(Double.parseDouble(salaryField.getText()));
                emp.setStatus(statusCombo.getValue());

                payrollService.saveEmployee(emp);
                AlertUtil.showInfo("Success", "Employee record saved successfully!");
                dialog.close();
                refreshEmployees();
            } catch (Exception ex) {
                AlertUtil.showError("Validation Error", ex.getMessage());
            }
        });

        grid.add(saveBtn, 1, 8);

        Scene scene = new Scene(grid, 450, 400);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openMarkAttendanceDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Mark Employee Attendance");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Employee> empCombo = new ComboBox<>(FXCollections.observableArrayList(payrollService.getActiveEmployees()));
        empCombo.setPrefWidth(250);

        DatePicker datePicker = new DatePicker(attendanceDatePicker.getValue());
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("PRESENT", "ABSENT", "HALF_DAY", "LEAVE"));
        statusCombo.setValue("PRESENT");

        TextField remarksField = new TextField();
        remarksField.setPromptText("Optional remarks");

        grid.add(new Label("Employee:*"), 0, 0);
        grid.add(empCombo, 1, 0);
        grid.add(new Label("Date:*"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Status:*"), 0, 2);
        grid.add(statusCombo, 1, 2);
        grid.add(new Label("Remarks:"), 0, 3);
        grid.add(remarksField, 1, 3);

        Button saveBtn = new Button("Record Attendance");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            Employee emp = empCombo.getValue();
            if (emp == null) {
                AlertUtil.showError("Validation Error", "Please select an employee.");
                return;
            }
            payrollService.markAttendance(emp.getId(), datePicker.getValue().toString(), statusCombo.getValue(), remarksField.getText().trim());
            AlertUtil.showInfo("Success", "Attendance recorded successfully!");
            dialog.close();
            refreshAttendance();
        });

        grid.add(saveBtn, 1, 4);

        Scene scene = new Scene(grid, 420, 240);
        if (getClass().getResource("/com/shop/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/shop/styles.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void handleDeactivate(Employee emp) {
        if (AlertUtil.showConfirmation("Deactivate Employee", "Are you sure you want to deactivate " + emp.getName() + "?")) {
            payrollService.deactivateEmployee(emp.getId());
            refreshEmployees();
        }
    }
}
