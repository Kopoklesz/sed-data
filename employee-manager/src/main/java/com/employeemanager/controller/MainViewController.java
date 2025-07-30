package com.employeemanager.controller;

import com.employeemanager.dialog.EmployeeDialog;
import com.employeemanager.dialog.SettingsDialog;
import com.employeemanager.dialog.UserGuideDialog;
import com.employeemanager.dialog.WorkRecordDialog;
import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.model.fx.EmployeeFX;
import com.employeemanager.model.fx.WorkRecordFX;
import com.employeemanager.service.interfaces.EmployeeService;
import com.employeemanager.service.impl.ReportService;
import com.employeemanager.service.impl.SettingsService;
import com.employeemanager.util.AlertHelper;
import com.employeemanager.util.ExcelExporter;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import com.employeemanager.component.StatusBar;

@Controller
@RequiredArgsConstructor
public class MainViewController implements Initializable {

    private final EmployeeService employeeService;
    private final ReportService reportService;
    private final SettingsService settingsService;
    private final ExcelExporter excelExporter;

    // FXML injections for employee table
    @FXML private TextField employeeSearchField;
    @FXML private TableView<EmployeeFX> employeeTable;
    @FXML private TableColumn<EmployeeFX, String> idColumn;
    @FXML private TableColumn<EmployeeFX, String> nameColumn;
    @FXML private TableColumn<EmployeeFX, String> birthPlaceColumn;
    @FXML private TableColumn<EmployeeFX, LocalDate> birthDateColumn;
    @FXML private TableColumn<EmployeeFX, String> motherNameColumn;
    @FXML private TableColumn<EmployeeFX, String> taxNumberColumn;
    @FXML private TableColumn<EmployeeFX, String> socialSecurityColumn;
    @FXML private TableColumn<EmployeeFX, String> addressColumn;

    // FXML injections for work record table
    @FXML private TableView<WorkRecordFX> workRecordTable;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label totalHoursLabel;
    @FXML private Label totalPaymentLabel;
    @FXML private TableColumn<WorkRecordFX, String> workIdColumn;
    @FXML private TableColumn<WorkRecordFX, String> employeeNameColumn;
    @FXML private TableColumn<WorkRecordFX, LocalDate> notificationDateColumn;
    @FXML private TableColumn<WorkRecordFX, String> ebevSerialColumn;
    @FXML private TableColumn<WorkRecordFX, LocalDate> workDateColumn;
    @FXML private TableColumn<WorkRecordFX, BigDecimal> paymentColumn;
    @FXML private TableColumn<WorkRecordFX, Integer> hoursWorkedColumn;

    // FXML injections for report tab
    @FXML private DatePicker reportStartDate;
    @FXML private DatePicker reportEndDate;
    @FXML private CheckBox includeEmployeeDetails;
    @FXML private CheckBox includeWorkRecords;
    @FXML private CheckBox includeSummary;
    @FXML private ListView<String> reportList;

    @FXML private StatusBar statusBar;

    private FilteredList<EmployeeFX> filteredEmployees;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEmployeeTable();
        setupWorkRecordTable();
        setupSearchField();
        setupDatePickers();
        loadInitialData();
        updateStatus("Alkalmazás betöltve");
    }

    private void setupEmployeeTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        birthPlaceColumn.setCellValueFactory(new PropertyValueFactory<>("birthPlace"));
        birthDateColumn.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        motherNameColumn.setCellValueFactory(new PropertyValueFactory<>("motherName"));
        taxNumberColumn.setCellValueFactory(new PropertyValueFactory<>("taxNumber"));
        socialSecurityColumn.setCellValueFactory(new PropertyValueFactory<>("socialSecurityNumber"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));

        // Dátum formázás a táblázatban
        birthDateColumn.setCellFactory(column -> new TableCell<EmployeeFX, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            }
        });

        // Dupla kattintás esemény - váltás a munkanaplók tab-ra
        employeeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !employeeTable.getSelectionModel().isEmpty()) {
                EmployeeFX selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();
                if (selectedEmployee != null) {
                    // Betöltjük a munkanaplókat
                    loadEmployeeWorkRecords(selectedEmployee);
                    // Átváltunk a Havi nyilvántartás tab-ra
                    TabPane tabPane = (TabPane) employeeTable.getScene().lookup(".tab-pane");
                    if (tabPane != null) {
                        tabPane.getSelectionModel().select(1); // 1 = Havi nyilvántartás tab
                    }
                    updateStatus("Megjelenítve: " + selectedEmployee.getName() + " munkanaplói");
                }
            }
        });
    }

    private void setupWorkRecordTable() {
        workIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        employeeNameColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        notificationDateColumn.setCellValueFactory(new PropertyValueFactory<>("notificationDate"));
        ebevSerialColumn.setCellValueFactory(new PropertyValueFactory<>("ebevSerialNumber"));
        workDateColumn.setCellValueFactory(new PropertyValueFactory<>("workDate"));
        paymentColumn.setCellValueFactory(new PropertyValueFactory<>("payment"));
        hoursWorkedColumn.setCellValueFactory(new PropertyValueFactory<>("hoursWorked"));
    }

    private void setupSearchField() {
        employeeSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredEmployees != null) {
                filteredEmployees.setPredicate(employee -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return employee.getName().toLowerCase().contains(lowerCaseFilter);
                });
            }
        });
    }

    private void setupDatePickers() {
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));

        reportStartDate.setValue(now.withDayOfMonth(1));
        reportEndDate.setValue(now.withDayOfMonth(now.lengthOfMonth()));
    }

    private void loadInitialData() {
        try {
            List<Employee> employees = employeeService.getAllEmployees();
            List<EmployeeFX> employeeFXList = employees.stream()
                    .map(EmployeeFX::new)
                    .collect(Collectors.toList());

            filteredEmployees = new FilteredList<>(FXCollections.observableArrayList(employeeFXList));
            employeeTable.setItems(filteredEmployees);

            filterWorkRecords();
            updateStatus("Adatok betöltve");
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült betölteni az adatokat", e.getMessage());
            updateStatus("Hiba az adatok betöltése közben");
        }
    }

    @FXML
    private void showAddEmployeeDialog() {
        Dialog<EmployeeFX> dialog = new EmployeeDialog();
        dialog.showAndWait().ifPresent(this::saveEmployee);
    }

    @FXML
    private void showEditEmployeeDialog() {
        EmployeeFX selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();
        if (selectedEmployee == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott alkalmazott");
            return;
        }

        Dialog<EmployeeFX> dialog = new EmployeeDialog(selectedEmployee);
        dialog.showAndWait().ifPresent(this::saveEmployee);
    }

    private void saveEmployee(EmployeeFX employeeFX) {
        try {
            Employee savedEmployee = employeeService.saveEmployee(employeeFX.toEmployee());
            loadInitialData();
            updateStatus("Alkalmazott mentve: " + savedEmployee.getName());
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült menteni az alkalmazottat", e.getMessage());
            updateStatus("Hiba az alkalmazott mentése közben");
        }
    }

    @FXML
    private void deleteEmployee() {
        EmployeeFX selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();
        if (selectedEmployee == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott alkalmazott");
            return;
        }

        if (AlertHelper.showConfirmation("Törlés megerősítése",
                "Biztosan törli a kiválasztott alkalmazottat?",
                "Ez a művelet nem vonható vissza.")) {
            try {
                employeeService.deleteEmployee(selectedEmployee.getId()); // String ID használata
                loadInitialData();
                updateStatus("Alkalmazott törölve: " + selectedEmployee.getName());
            } catch (Exception e) {
                AlertHelper.showError("Hiba", "Nem sikerült törölni az alkalmazottat", e.getMessage());
                updateStatus("Hiba az alkalmazott törlése közben");
            }
        }
    }

    @FXML
    private void showAddWorkRecordDialog() {
        Dialog<WorkRecordFX> dialog = new WorkRecordDialog(employeeService);
        dialog.showAndWait().ifPresent(this::saveWorkRecord);
    }

    @FXML
    private void showEditWorkRecordDialog() {
        WorkRecordFX selectedRecord = workRecordTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott munkanapló");
            return;
        }

        Dialog<WorkRecordFX> dialog = new WorkRecordDialog(employeeService, selectedRecord);
        dialog.showAndWait().ifPresent(this::saveWorkRecord);
    }

    private void saveWorkRecord(WorkRecordFX workRecordFX) {
        try {
            WorkRecord savedRecord = employeeService.addWorkRecord(workRecordFX.toWorkRecord());
            filterWorkRecords();
            updateStatus("Munkanapló mentve");
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült menteni a munkanaplót", e.getMessage());
            updateStatus("Hiba a munkanapló mentése közben");
        }
    }

    @FXML
    private void deleteWorkRecord() {
        WorkRecordFX selectedRecord = workRecordTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott munkanapló");
            return;
        }

        if (AlertHelper.showConfirmation("Törlés megerősítése",
                "Biztosan törli a kiválasztott munkanaplót?",
                "Ez a művelet nem vonható vissza.")) {
            try {
                employeeService.deleteWorkRecord(selectedRecord.getId()); // String ID használata
                filterWorkRecords();
                updateStatus("Munkanapló törölve");
            } catch (Exception e) {
                AlertHelper.showError("Hiba", "Nem sikerült törölni a munkanaplót", e.getMessage());
                updateStatus("Hiba a munkanapló törlése közben");
            }
        }
    }

    @FXML
    private void filterWorkRecords() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Kérem válasszon időszakot");
            return;
        }

        try {
            List<WorkRecord> workRecords = employeeService.getMonthlyRecords(start, end);
            List<WorkRecordFX> workRecordFXList = workRecords.stream()
                    .map(WorkRecordFX::new)
                    .collect(Collectors.toList());

            workRecordTable.setItems(FXCollections.observableArrayList(workRecordFXList));
            updateSummary(workRecordFXList);
            updateStatus("Munkanaplók szűrve");
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült szűrni a munkanaplókat", e.getMessage());
            updateStatus("Hiba a munkanaplók szűrése közben");
        }
    }

    private void updateSummary(List<WorkRecordFX> records) {
        int totalHours = records.stream()
                .mapToInt(WorkRecordFX::getHoursWorked)
                .sum();

        BigDecimal totalPayment = records.stream()
                .map(WorkRecordFX::getPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalHoursLabel.setText(String.format("%d óra", totalHours));
        totalPaymentLabel.setText(String.format("%,.0f Ft", totalPayment));
    }

    @FXML
    private void generateReport() {
        try {
            LocalDate start = reportStartDate.getValue();
            LocalDate end = reportEndDate.getValue();

            if (start == null || end == null) {
                AlertHelper.showWarning("Figyelmeztetés", "Kérem válasszon időszakot");
                return;
            }

            String reportPath = reportService.generateReport(start, end,
                    includeEmployeeDetails.isSelected(),
                    includeWorkRecords.isSelected(),
                    includeSummary.isSelected());

            loadReportList();
            updateStatus("Riport generálva: " + reportPath);
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült generálni a riportot", e.getMessage());
            updateStatus("Hiba a riport generálása közben");
        }
    }

    private void loadReportList() {
        try {
            List<String> reports = reportService.getAvailableReports();
            reportList.setItems(FXCollections.observableArrayList(reports));
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült betölteni a riportokat", e.getMessage());
        }
    }

    @FXML
    private void exportToExcel() {
        try {
            String filePath = excelExporter.exportWorkRecords(
                    workRecordTable.getItems(),
                    startDatePicker.getValue(),
                    endDatePicker.getValue());

            updateStatus("Excel exportálva: " + filePath);
            AlertHelper.showInformation("Sikeres exportálás",
                    "Az Excel fájl elkészült",
                    "Fájl helye: " + filePath);
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült exportálni az Excel fájlt", e.getMessage());
            updateStatus("Hiba az Excel exportálás közben");
        }
    }

    @FXML
    private void showSettings() {
        try {
            Dialog<Void> dialog = new SettingsDialog(settingsService);
            dialog.showAndWait();
            updateStatus("Beállítások mentve");
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült megnyitni a beállításokat", e.getMessage());
            updateStatus("Hiba a beállítások megnyitása közben");
        }
    }

    @FXML
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Névjegy");
        alert.setHeaderText("Alkalmazott Nyilvántartó Rendszer");
        alert.setContentText("Verzió: " + settingsService.getApplicationVersion() + "\n" +
                "Készítette: " + settingsService.getApplicationAuthor() + "\n\n" +
                "© 2024 Minden jog fenntartva");
        alert.showAndWait();
    }

    @FXML
    private void showUserGuide() {
        try {
            Dialog<Void> dialog = new UserGuideDialog();
            dialog.showAndWait();
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült megnyitni a használati útmutatót", e.getMessage());
        }
    }

    @FXML
    private void exitApplication() {
        if (AlertHelper.showConfirmation("Kilépés",
                "Biztosan ki szeretne lépni?",
                "Minden nem mentett változás elvész.")) {
            System.exit(0);
        }
    }

    private void updateStatus(String message) {
        statusBar.setText(message + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private void loadEmployeeWorkRecords(EmployeeFX employee) {
        try {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (start != null && end != null) {
                List<WorkRecord> records = employeeService.getEmployeeMonthlyRecords(
                        employee.getId(), start, end); // String ID használata

                List<WorkRecordFX> workRecordFXList = records.stream()
                        .map(WorkRecordFX::new)
                        .collect(Collectors.toList());

                workRecordTable.setItems(FXCollections.observableArrayList(workRecordFXList));
                updateSummary(workRecordFXList);
                updateStatus(employee.getName() + " munkanaplói betöltve");
            }
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült betölteni az alkalmazott munkanaplóit", e.getMessage());
            updateStatus("Hiba a munkanaplók betöltése közben");
        }
    }

    @FXML
    private void showEmployeeList() {
        // Alapértelmezetten már az alkalmazottak tab van megnyitva
    }

    @FXML
    private void showEmployeeSearch() {
        // A keresés már implementálva van a TextField-en keresztül
        employeeSearchField.requestFocus();
    }

    @FXML
    private void searchEmployees() {
        // A keresés már automatikusan működik a TextField listener-en keresztül
        String searchText = employeeSearchField.getText();
        if (filteredEmployees != null) {
            filteredEmployees.setPredicate(employee -> {
                if (searchText == null || searchText.isEmpty()) {
                    return true;
                }
                return employee.getName().toLowerCase().contains(searchText.toLowerCase());
            });
        }
    }

    @FXML
    private void showMonthlyOverview() {
        // Átvált a havi nyilvántartás tabra
        TabPane tabPane = (TabPane) workRecordTable.getScene().lookup(".tab-pane");
        tabPane.getSelectionModel().select(1); // 1 = második tab
    }

    @FXML
    private void showWorkRecordSearch() {
        // Átvált a havi nyilvántartás tabra és fókuszba helyezi a dátumválasztót
        TabPane tabPane = (TabPane) workRecordTable.getScene().lookup(".tab-pane");
        tabPane.getSelectionModel().select(1);
        startDatePicker.requestFocus();
    }

    @FXML
    private void generateMonthlyReport() {
        // Átvált a riportok tabra és beállítja az aktuális hónapot
        TabPane tabPane = (TabPane) reportList.getScene().lookup(".tab-pane");
        tabPane.getSelectionModel().select(2); // 2 = harmadik tab
        LocalDate now = LocalDate.now();
        reportStartDate.setValue(now.withDayOfMonth(1));
        reportEndDate.setValue(now.withDayOfMonth(now.lengthOfMonth()));
        includeWorkRecords.setSelected(true);
        includeSummary.setSelected(true);
        generateReport();
    }

    @FXML
    private void generateEmployeeReport() {
        // Átvált a riportok tabra és beállítja az alkalmazotti részleteket
        TabPane tabPane = (TabPane) reportList.getScene().lookup(".tab-pane");
        tabPane.getSelectionModel().select(2);
        includeEmployeeDetails.setSelected(true);
        generateReport();
    }

    @FXML
    private void showCustomReportDialog() {
        // Átvált a riportok tabra
        TabPane tabPane = (TabPane) reportList.getScene().lookup(".tab-pane");
        tabPane.getSelectionModel().select(2);
    }

    @FXML
    private void openReport() {
        String selectedReport = reportList.getSelectionModel().getSelectedItem();
        if (selectedReport == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott riport");
            return;
        }

        try {
            // TODO: Implement report opening logic
            updateStatus("Riport megnyitása: " + selectedReport);
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült megnyitni a riportot", e.getMessage());
        }
    }

    @FXML
    private void exportReport() {
        String selectedReport = reportList.getSelectionModel().getSelectedItem();
        if (selectedReport == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott riport");
            return;
        }

        try {
            // TODO: Implement report export logic
            updateStatus("Riport exportálása: " + selectedReport);
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült exportálni a riportot", e.getMessage());
        }
    }

    @FXML
    private void deleteReport() {
        String selectedReport = reportList.getSelectionModel().getSelectedItem();
        if (selectedReport == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott riport");
            return;
        }

        if (AlertHelper.showConfirmation("Törlés megerősítése",
                "Biztosan törli a kiválasztott riportot?",
                "Ez a művelet nem vonható vissza.")) {
            try {
                // TODO: Implement report deletion logic
                loadReportList();
                updateStatus("Riport törölve: " + selectedReport);
            } catch (Exception e) {
                AlertHelper.showError("Hiba", "Nem sikerült törölni a riportot", e.getMessage());
            }
        }
    }

    @FXML
    private void showEmployeeWorkRecords() {
        EmployeeFX selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();
        if (selectedEmployee != null) {
            loadEmployeeWorkRecords(selectedEmployee);
        }
    }
}