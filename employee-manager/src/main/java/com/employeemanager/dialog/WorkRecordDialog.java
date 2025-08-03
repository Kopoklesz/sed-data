package com.employeemanager.dialog;

import com.employeemanager.component.CalendarView;
import com.employeemanager.model.Employee;
import com.employeemanager.model.fx.WorkRecordFX;
import com.employeemanager.service.interfaces.EmployeeService;
import com.employeemanager.util.ValidationHelper;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkRecordDialog extends Dialog<List<WorkRecordFX>> {

    private final ComboBox<Employee> employeeComboBox = new ComboBox<>();
    private final DatePicker notificationDatePicker = new DatePicker();
    private final TextField notificationTimeField = new TextField();
    private final TextField ebevSerialField = new TextField();
    private final TextField paymentField = new TextField();
    private final TextField hoursWorkedField = new TextField();

    private final CalendarView calendarView = new CalendarView();
    private Label dateWarningLabel;

    private final EmployeeService employeeService;
    private final WorkRecordFX originalRecord;

    public WorkRecordDialog(EmployeeService employeeService) {
        this(employeeService, null);
    }

    public WorkRecordDialog(EmployeeService employeeService, WorkRecordFX workRecord) {
        this.employeeService = employeeService;
        this.originalRecord = workRecord;

        setTitle(workRecord == null ? "Új munkanapló" : "Munkanapló szerkesztése");
        setHeaderText("Kérem, adja meg a munkanapló adatait:");

        setupDialog();
        loadEmployees();
        if (workRecord != null) {
            populateFields();
        }
        setupValidation();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setPrefWidth(600);
        dialogPane.setPrefHeight(700);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        int row = 0;

        // Alkalmazott
        grid.add(new Label("Alkalmazott:"), 0, row);
        employeeComboBox.setPrefWidth(400);
        grid.add(employeeComboBox, 1, row++);

        // Bejelentés dátuma
        grid.add(new Label("Bejelentés dátuma:"), 0, row);
        notificationDatePicker.setPrefWidth(400);
        grid.add(notificationDatePicker, 1, row++);

        // Bejelentés időpontja
        grid.add(new Label("Bejelentés időpontja:"), 0, row);
        notificationTimeField.setPrefWidth(400);
        notificationTimeField.setPromptText("HH:mm");
        grid.add(notificationTimeField, 1, row++);

        // EBEV azonosító
        grid.add(new Label("EBEV azonosító:"), 0, row);
        ebevSerialField.setPrefWidth(400);
        grid.add(ebevSerialField, 1, row++);

        // Munkanapok kiválasztása
        grid.add(new Label("Munkanapok:"), 0, row);
        VBox calendarContainer = new VBox(10);
        calendarView.setMaxWidth(400);
        calendarContainer.getChildren().add(calendarView);

        grid.add(calendarContainer, 1, row++);

        // Figyelmeztetés
        dateWarningLabel = new Label();
        dateWarningLabel.setVisible(false);
        dateWarningLabel.setWrapText(true);
        dateWarningLabel.setMaxWidth(500);
        grid.add(dateWarningLabel, 0, row++, 2, 1);

        // Bérezés
        grid.add(new Label("Bérezés (Ft):"), 0, row);
        paymentField.setPrefWidth(400);
        grid.add(paymentField, 1, row++);

        // Munkaórák
        grid.add(new Label("Munkaórák:"), 0, row);
        hoursWorkedField.setPrefWidth(400);
        grid.add(hoursWorkedField, 1, row++);

        dialogPane.setContent(grid);

        // Employee ComboBox beállítása
        employeeComboBox.setConverter(new StringConverter<Employee>() {
            @Override
            public String toString(Employee employee) {
                return employee != null ? employee.getName() : "";
            }

            @Override
            public Employee fromString(String string) {
                return null;
            }
        });

        // Result converter
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return createWorkRecordsFromFields();
            }
            return null;
        });

        // OK gomb kezdetben letiltva
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        // DatePicker formátum
        StringConverter<LocalDate> dateConverter = new StringConverter<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                try {
                    return string != null && !string.isEmpty() ? LocalDate.parse(string, formatter) : null;
                } catch (Exception e) {
                    return null;
                }
            }
        };

        notificationDatePicker.setConverter(dateConverter);

        // CalendarView változás figyelése
        calendarView.getSelectedDates().addListener((ListChangeListener<LocalDate>) change -> {
            // A validáció már figyeli ezt
        });

        // Default értékek
        notificationDatePicker.setValue(LocalDate.now());
        notificationTimeField.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    private void loadEmployees() {
        List<Employee> employees = employeeService.getAllEmployees();
        employeeComboBox.getItems().addAll(employees);
    }

    private void populateFields() {
        if (originalRecord != null) {
            employeeComboBox.getSelectionModel().select(originalRecord.getEmployee());
            notificationDatePicker.setValue(originalRecord.getNotificationDate());

            if (originalRecord.getNotificationTime() != null) {
                notificationTimeField.setText(originalRecord.getNotificationTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }

            ebevSerialField.setText(originalRecord.getEbevSerialNumber());
            paymentField.setText(originalRecord.getPayment().toString());
            hoursWorkedField.setText(String.valueOf(originalRecord.getHoursWorked()));

            // Szerkesztés esetén csak az eredeti munkanapot állítjuk be
            if (originalRecord.getWorkDate() != null) {
                calendarView.setSelectedDates(List.of(originalRecord.getWorkDate()));
            }
        }
    }

    private void setupValidation() {
        ValidationHelper.setUpperCaseListener(ebevSerialField);
        ValidationHelper.setNumberOnlyListener(paymentField);
        ValidationHelper.setNumberOnlyListener(hoursWorkedField);
        ValidationHelper.setTimeFormatListener(notificationTimeField);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);

        // Dátum logika ellenőrzés
        Runnable checkDateLogic = () -> {
            LocalDate notificationDate = notificationDatePicker.getValue();
            List<LocalDate> selectedDates = new ArrayList<>(calendarView.getSelectedDates());

            if (notificationDate != null && !selectedDates.isEmpty()) {
                LocalDate earliestWorkDate = selectedDates.stream()
                        .min(LocalDate::compareTo)
                        .orElse(null);

                if (earliestWorkDate != null && notificationDate.isAfter(earliestWorkDate)) {
                    showDateWarning(true);
                } else {
                    showDateWarning(false);
                }
            }

            // CalendarView frissítése
            calendarView.setNotificationDate(notificationDate);
        };

        // Validáció
        Runnable validateFields = () -> {
            boolean isValid = employeeComboBox.getValue() != null &&
                    notificationDatePicker.getValue() != null &&
                    ValidationHelper.isValidNotificationTime(notificationTimeField.getText()) &&
                    ValidationHelper.isValidEbevSerial(ebevSerialField.getText()) &&
                    !calendarView.getSelectedDates().isEmpty() &&
                    isValidPayment() &&
                    isValidHours();

            okButton.setDisable(!isValid);
        };

        // Figyelők
        employeeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        notificationDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            checkDateLogic.run();
            validateFields.run();
        });
        notificationTimeField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        ebevSerialField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        paymentField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        hoursWorkedField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        calendarView.getSelectedDates().addListener((ListChangeListener<LocalDate>) change -> {
            checkDateLogic.run();
            validateFields.run();
        });

        validateFields.run();
    }

    private void showDateWarning(boolean show) {
        if (show) {
            dateWarningLabel.setText("⚠️ Figyelem: A bejelentés dátuma későbbi, mint az első munkanap!");
            dateWarningLabel.setStyle("-fx-text-fill: #ff6b00; -fx-font-weight: bold;");
            dateWarningLabel.setVisible(true);
        } else {
            dateWarningLabel.setVisible(false);
        }
    }

    private boolean isValidPayment() {
        try {
            BigDecimal payment = new BigDecimal(paymentField.getText());
            return payment.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidHours() {
        try {
            int hours = Integer.parseInt(hoursWorkedField.getText());
            return hours > 0 && hours <= 24;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<WorkRecordFX> createWorkRecordsFromFields() {
        LocalDate notificationDate = notificationDatePicker.getValue();
        List<LocalDate> selectedDates = new ArrayList<>(calendarView.getSelectedDates());

        if (!selectedDates.isEmpty()) {
            LocalDate earliestWorkDate = selectedDates.stream()
                    .min(LocalDate::compareTo)
                    .orElse(null);

            if (earliestWorkDate != null && notificationDate.isAfter(earliestWorkDate)) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Dátum figyelmeztetés");
                confirmAlert.setHeaderText("A bejelentés dátuma későbbi, mint az első munkanap.");
                confirmAlert.setContentText("Biztosan folytatja a mentést?");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return null;
                }
            }
        }

        List<WorkRecordFX> records = new ArrayList<>();
        LocalTime notificationTime = ValidationHelper.parseTime(notificationTimeField.getText());

        for (LocalDate workDate : selectedDates) {
            WorkRecordFX record = new WorkRecordFX();
            if (originalRecord != null && originalRecord.getId() != null) {
                record.setId(originalRecord.getId());
            }
            record.setEmployee(employeeComboBox.getValue());
            record.setNotificationDate(notificationDatePicker.getValue());
            record.setNotificationTime(notificationTime);
            record.setEbevSerialNumber(ebevSerialField.getText());
            record.setWorkDate(workDate);
            record.setPayment(new BigDecimal(paymentField.getText()));
            record.setHoursWorked(Integer.parseInt(hoursWorkedField.getText()));
            records.add(record);
        }

        return records;
    }
}