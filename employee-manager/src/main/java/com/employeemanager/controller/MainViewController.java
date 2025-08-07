package com.employeemanager.controller;

import com.employeemanager.config.DatabaseConnectionManager;
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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final DatabaseConnectionManager connectionManager;

    // FXML injections for main TabPane
    @FXML private TabPane mainTabPane;

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
    @FXML private TableColumn<WorkRecordFX, LocalTime> notificationTimeColumn;
    @FXML private TableColumn<WorkRecordFX, String> ebevSerialColumn;
    @FXML private TableColumn<WorkRecordFX, LocalDate> workDateColumn;
    @FXML private TableColumn<WorkRecordFX, BigDecimal> paymentColumn;
    @FXML private TableColumn<WorkRecordFX, Integer> hoursWorkedColumn;

    // Szűrési radio buttonok
    @FXML private RadioButton filterByNotificationDate;
    @FXML private RadioButton filterByWorkDate;
    @FXML private RadioButton filterByBoth;
    @FXML private ToggleGroup filterGroup;

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

        // Add window event listener for refresh
        Platform.runLater(() -> {
            Stage stage = (Stage) mainTabPane.getScene().getWindow();
            if (stage != null) {
                stage.addEventHandler(javafx.stage.WindowEvent.WINDOW_SHOWN, event -> {
                    // This will be triggered when settings dialog signals a refresh
                    if (event.getTarget() == stage) {
                        refreshData();
                    }
                });
            }
        });

        updateStatus("Alkalmazás betöltve - Aktív kapcsolat: " +
                (connectionManager.getActiveConnection() != null ?
                        connectionManager.getActiveConnection().getProfileName() + " (" +
                                connectionManager.getActiveConnection().getType().getDisplayName() + ")" :
                        "Nincs"));
    }

    public void refreshData() {
        Platform.runLater(() -> {
            try {
                updateStatus("Adatok frissítése...");

                // Clear current data
                employeeTable.getItems().clear();
                workRecordTable.getItems().clear();
                reportList.getItems().clear();

                // Reload all data
                loadInitialData();

                updateStatus("Adatok frissítve - Aktív kapcsolat: " +
                        connectionManager.getActiveConnection().getProfileName() + " (" +
                        connectionManager.getActiveConnection().getType().getDisplayName() + ")");
            } catch (Exception e) {
                AlertHelper.showError("Hiba", "Nem sikerült frissíteni az adatokat", e.getMessage());
                updateStatus("Hiba az adatok frissítése közben");
            }
        });
    }

    // ==========================================
    // MENÜ AKCIÓK - FÁJL MENÜ
    // ==========================================

    @FXML
    private void showAddEmployeeDialog() {
        if (isSafeToExecuteShortcut()) {
            Dialog<EmployeeFX> dialog = new EmployeeDialog();
            dialog.showAndWait().ifPresent(this::saveEmployee);
        }
    }

    @FXML
    private void showAddWorkRecordDialog() {
        if (isSafeToExecuteShortcut()) {
            Dialog<List<WorkRecordFX>> dialog = new WorkRecordDialog(employeeService);
            dialog.showAndWait().ifPresent(this::saveWorkRecords);
        }
    }

    // ==========================================
    // MENÜ AKCIÓK - SZERKESZTÉS MENÜ
    // ==========================================

    @FXML
    private void editSelectedEmployee() {
        if (!isSafeToExecuteShortcut()) {
            return;
        }

        EmployeeFX selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();

        if (selectedEmployee == null) {
            showEmployeeTab();
            updateStatus("Válasszon ki egy alkalmazottat a szerkesztéshez");
            AlertHelper.showInformation("Alkalmazott szerkesztése",
                    "Nincs kiválasztott alkalmazott",
                    "Kérem válasszon ki egy alkalmazottat a táblázatból a szerkesztéshez.");
            return;
        }

        Dialog<EmployeeFX> dialog = new EmployeeDialog(selectedEmployee);
        dialog.showAndWait().ifPresent(this::saveEmployee);
    }

    @FXML
    private void editSelectedWorkRecord() {
        if (!isSafeToExecuteShortcut()) {
            return;
        }

        WorkRecordFX selectedRecord = workRecordTable.getSelectionModel().getSelectedItem();

        if (selectedRecord == null) {
            showWorkRecordTab();
            updateStatus("Válasszon ki egy munkanaplót a szerkesztéshez");
            AlertHelper.showInformation("Munkanapló szerkesztése",
                    "Nincs kiválasztott munkanapló",
                    "Kérem válasszon ki egy munkanaplót a táblázatból a szerkesztéshez.");
            return;
        }

        Dialog<List<WorkRecordFX>> dialog = new WorkRecordDialog(employeeService, selectedRecord);
        dialog.showAndWait().ifPresent(records -> {
            if (records != null && !records.isEmpty()) {
                saveWorkRecords(records);
            }
        });
    }

    // ==========================================
    // MENÜ AKCIÓK - NÉZET MENÜ
    // ==========================================

    @FXML
    private void showEmployeeTab() {
        mainTabPane.getSelectionModel().select(0);
        updateStatus("Alkalmazottak tab megjelenítve");
    }

    @FXML
    private void showWorkRecordTab() {
        mainTabPane.getSelectionModel().select(1);
        updateStatus("Munkanaplók tab megjelenítve");
    }

    @FXML
    private void showReportsTab() {
        mainTabPane.getSelectionModel().select(2);
        updateStatus("Riportok tab megjelenítve");
    }

    // ==========================================
    // MENÜ AKCIÓK - ESZKÖZÖK MENÜ
    // ==========================================

    @FXML
    private void showDatabaseSettings() {
        try {
            Dialog<Void> dialog = new SettingsDialog(settingsService, connectionManager);
            dialog.showAndWait();
            updateStatus("Adatbázis beállítások megjelenítve");
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült megnyitni a beállításokat", e.getMessage());
            updateStatus("Hiba a beállítások megnyitása közben");
        }
    }

    @FXML
    private void showUserGuide() {
        try {
            Dialog<Void> dialog = new UserGuideDialog();
            dialog.showAndWait();
            updateStatus("Használati útmutató megjelenítve");
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült megnyitni a használati útmutatót", e.getMessage());
            updateStatus("Hiba a használati útmutató megnyitása közben");
        }
    }

    @FXML
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Névjegy");
        alert.setHeaderText("Alkalmazott Nyilvántartó Rendszer");
        alert.setContentText("Verzió: " + settingsService.getApplicationVersion() + "\n" +
                "Készítette: " + settingsService.getApplicationAuthor() + "\n\n" +
                "Modern JavaFX alkalmazás alkalmazottak és munkanaplók kezelésére.\n" +
                "Firebase adatbázis támogatással és Excel export funkcióval.\n\n" +
                "© 2024 Minden jog fenntartva");
        alert.showAndWait();
        updateStatus("Névjegy megjelenítve");
    }

    // ==========================================
    // SEGÉD METÓDUSOK
    // ==========================================

    private boolean isSafeToExecuteShortcut() {
        Node focusedNode = mainTabPane.getScene().getFocusOwner();
        if (focusedNode instanceof TextInputControl) {
            return false;
        }
        return true;
    }

    private void setupEmployeeTable() {
        employeeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        employeeTable.setRowFactory(tv -> {
            TableRow<EmployeeFX> row = new TableRow<>();
            row.setPrefHeight(40);
            return row;
        });

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        birthPlaceColumn.setCellValueFactory(new PropertyValueFactory<>("birthPlace"));
        birthDateColumn.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        motherNameColumn.setCellValueFactory(new PropertyValueFactory<>("motherName"));
        taxNumberColumn.setCellValueFactory(new PropertyValueFactory<>("taxNumber"));
        socialSecurityColumn.setCellValueFactory(new PropertyValueFactory<>("socialSecurityNumber"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));

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

        employeeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !employeeTable.getSelectionModel().isEmpty()) {
                EmployeeFX selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();
                if (selectedEmployee != null) {
                    loadEmployeeWorkRecords(selectedEmployee);
                    showWorkRecordTab();
                    updateStatus("Megjelenítve: " + selectedEmployee.getName() + " munkanaplói");
                }
            }
        });
    }

    private void setupWorkRecordTable() {
        workRecordTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        workRecordTable.setRowFactory(tv -> {
            TableRow<WorkRecordFX> row = new TableRow<>();
            row.setPrefHeight(40);
            return row;
        });

        workIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        employeeNameColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        notificationDateColumn.setCellValueFactory(new PropertyValueFactory<>("notificationDate"));
        notificationTimeColumn.setCellValueFactory(new PropertyValueFactory<>("notificationTime"));
        ebevSerialColumn.setCellValueFactory(new PropertyValueFactory<>("ebevSerialNumber"));
        workDateColumn.setCellValueFactory(new PropertyValueFactory<>("workDate"));
        paymentColumn.setCellValueFactory(new PropertyValueFactory<>("payment"));
        hoursWorkedColumn.setCellValueFactory(new PropertyValueFactory<>("hoursWorked"));

        notificationDateColumn.setCellFactory(column -> new TableCell<WorkRecordFX, LocalDate>() {
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

        workDateColumn.setCellFactory(column -> new TableCell<WorkRecordFX, LocalDate>() {
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

        notificationTimeColumn.setCellFactory(column -> new TableCell<WorkRecordFX, LocalTime>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("HH:mm")));
                }
            }
        });
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

        filterByNotificationDate.setSelected(true);
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
            loadReportList();
            updateStatus("Adatok betöltve");
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült betölteni az adatokat", e.getMessage());
            updateStatus("Hiba az adatok betöltése közben");
        }
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

    private void saveWorkRecords(List<WorkRecordFX> workRecordFXList) {
        if (workRecordFXList == null || workRecordFXList.isEmpty()) {
            return;
        }

        try {
            List<WorkRecord> workRecords = workRecordFXList.stream()
                    .map(WorkRecordFX::toWorkRecord)
                    .collect(Collectors.toList());

            List<WorkRecord> savedRecords = employeeService.addWorkRecords(workRecords);
            filterWorkRecords();
            updateStatus(savedRecords.size() + " munkanapló mentve");
        } catch (Exception e) {
            AlertHelper.showError("Hiba", "Nem sikerült menteni a munkanaplókat", e.getMessage());
            updateStatus("Hiba a munkanaplók mentése közben");
        }
    }

    // ==========================================
    // CONTEXT MENU ÉS EGYÉB AKCIÓK
    // ==========================================

    @FXML
    private void showEditEmployeeDialog() {
        editSelectedEmployee();
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
                employeeService.deleteEmployee(selectedEmployee.getId());
                loadInitialData();
                updateStatus("Alkalmazott törölve: " + selectedEmployee.getName());
            } catch (Exception e) {
                AlertHelper.showError("Hiba", "Nem sikerült törölni az alkalmazottat", e.getMessage());
                updateStatus("Hiba az alkalmazott törlése közben");
            }
        }
    }

    @FXML
    private void showEditWorkRecordDialog() {
        editSelectedWorkRecord();
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
                employeeService.deleteWorkRecord(selectedRecord.getId());
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
            List<WorkRecord> workRecords;

            if (filterByNotificationDate.isSelected()) {
                workRecords = employeeService.getRecordsByNotificationDate(start, end);
            } else if (filterByWorkDate.isSelected()) {
                workRecords = employeeService.getMonthlyRecords(start, end);
            } else {
                workRecords = employeeService.getRecordsByBothDates(start, end, start, end);
            }

            List<WorkRecordFX> workRecordFXList = workRecords.stream()
                    .map(WorkRecordFX::new)
                    .collect(Collectors.toList());

            workRecordTable.setItems(FXCollections.observableArrayList(workRecordFXList));
            updateSummary(workRecordFXList);
            updateStatus("Munkanaplók szűrve (" + workRecordFXList.size() + " találat)");
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
            AlertHelper.showInformation("Riport generálva",
                    "A riport sikeresen elkészült",
                    "Fájl helye: " + reportPath);
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

    private void updateStatus(String message) {
        if (statusBar != null) {
            statusBar.setText(message + " - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    private void loadEmployeeWorkRecords(EmployeeFX employee) {
        try {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (start != null && end != null) {
                List<WorkRecord> records = employeeService.getEmployeeMonthlyRecords(
                        employee.getId(), start, end);

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
    private void searchEmployees() {
        String searchText = employeeSearchField.getText();
        if (filteredEmployees != null) {
            filteredEmployees.setPredicate(employee -> {
                if (searchText == null || searchText.isEmpty()) {
                    return true;
                }
                return employee.getName().toLowerCase().contains(searchText.toLowerCase());
            });
        }
        updateStatus("Keresés: " + (searchText.isEmpty() ? "minden alkalmazott" : searchText));
    }

    @FXML
    private void showEmployeeWorkRecords() {
        EmployeeFX selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();
        if (selectedEmployee != null) {
            loadEmployeeWorkRecords(selectedEmployee);
            showWorkRecordTab();
        }
    }

    @FXML
    private void openReport() {
        String selectedReport = reportList.getSelectionModel().getSelectedItem();
        if (selectedReport == null) {
            AlertHelper.showWarning("Figyelmeztetés", "Nincs kiválasztott riport");
            return;
        }

        try {
            updateStatus("Riport megnyitása: " + selectedReport);
            AlertHelper.showInformation("Riport megnyitása",
                    "Fejlesztés alatt",
                    "A riport megnyitása funkció hamarosan elérhető lesz.");
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
            updateStatus("Riport exportálása: " + selectedReport);
            AlertHelper.showInformation("Riport exportálása",
                    "Fejlesztés alatt",
                    "A riport exportálása funkció hamarosan elérhető lesz.");
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
                loadReportList();
                updateStatus("Riport törölve: " + selectedReport);
                AlertHelper.showInformation("Riport törölve",
                        "Fejlesztés alatt",
                        "A riport törlése funkció hamarosan elérhető lesz.");
            } catch (Exception e) {
                AlertHelper.showError("Hiba", "Nem sikerült törölni a riportot", e.getMessage());
            }
        }
    }
}