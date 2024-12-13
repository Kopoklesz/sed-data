package com.employeemanager.dialog;

import com.employeemanager.model.Employee;
import com.employeemanager.model.fx.WorkRecordFX;
import com.employeemanager.service.interfaces.EmployeeService;
import com.employeemanager.util.ValidationHelper;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WorkRecordDialog extends Dialog<WorkRecordFX> {

    private final ComboBox<Employee> employeeComboBox = new ComboBox<>();
    private final DatePicker notificationDatePicker = new DatePicker();
    private final TextField ebevSerialField = new TextField();
    private final DatePicker workDatePicker = new DatePicker();
    private final TextField paymentField = new TextField();
    private final TextField hoursWorkedField = new TextField();

    private final EmployeeService employeeService;
    private final WorkRecordFX workRecord;

    public WorkRecordDialog(EmployeeService employeeService) {
        this(employeeService, new WorkRecordFX());
    }

    public WorkRecordDialog(EmployeeService employeeService, WorkRecordFX workRecord) {
        this.employeeService = employeeService;
        this.workRecord = workRecord;

        setTitle(workRecord.getId() == null ? "Új munkanapló" : "Munkanapló szerkesztése");
        setHeaderText("Kérem, adja meg a munkanapló adatait:");

        setupDialog();
        loadEmployees();
        populateFields();
        setupValidation();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Alkalmazott:"), 0, 0);
        grid.add(employeeComboBox, 1, 0);
        grid.add(new Label("Bejelentés dátuma:"), 0, 1);
        grid.add(notificationDatePicker, 1, 1);
        grid.add(new Label("EBEV azonosító:"), 0, 2);
        grid.add(ebevSerialField, 1, 2);
        grid.add(new Label("Munkanap:"), 0, 3);
        grid.add(workDatePicker, 1, 3);
        grid.add(new Label("Bérezés (Ft):"), 0, 4);
        grid.add(paymentField, 1, 4);
        grid.add(new Label("Munkaórák:"), 0, 5);
        grid.add(hoursWorkedField, 1, 5);

        dialogPane.setContent(grid);

        // Mezők szélességének beállítása
        employeeComboBox.setPrefWidth(300);
        notificationDatePicker.setPrefWidth(300);
        ebevSerialField.setPrefWidth(300);
        workDatePicker.setPrefWidth(300);
        paymentField.setPrefWidth(300);
        hoursWorkedField.setPrefWidth(300);

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

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return createWorkRecordFromFields();
            }
            return null;
        });

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(true);

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
        workDatePicker.setConverter(dateConverter);

        // Munkanap nem lehet jövőbeli
        workDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });

    }

    private void loadEmployees() {
        List<Employee> employees = employeeService.getAllEmployees();
        employeeComboBox.getItems().addAll(employees);
    }

    private void populateFields() {
        if (workRecord.getId() != null) {
            employeeComboBox.getSelectionModel().select(workRecord.getEmployee());
            notificationDatePicker.setValue(workRecord.getNotificationDate());
            ebevSerialField.setText(workRecord.getEbevSerialNumber());
            workDatePicker.setValue(workRecord.getWorkDate());
            paymentField.setText(workRecord.getPayment().toString());
            hoursWorkedField.setText(String.valueOf(workRecord.getHoursWorked()));
        } else {
            notificationDatePicker.setValue(LocalDate.now());
            workDatePicker.setValue(LocalDate.now());
        }
    }

    private void setupValidation() {
        ValidationHelper.setUpperCaseListener(ebevSerialField);
        ValidationHelper.setNumberOnlyListener(paymentField);
        ValidationHelper.setNumberOnlyListener(hoursWorkedField);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);

        Runnable validateFields = () -> {
            boolean isValid = employeeComboBox.getValue() != null &&
                    notificationDatePicker.getValue() != null &&
                    ValidationHelper.isValidEbevSerial(ebevSerialField.getText()) &&
                    workDatePicker.getValue() != null &&
                    isValidPayment() &&
                    isValidHours();

            okButton.setDisable(!isValid);
        };

        employeeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        notificationDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        ebevSerialField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        workDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        paymentField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        hoursWorkedField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());

        validateFields.run();
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

    private WorkRecordFX createWorkRecordFromFields() {
        WorkRecordFX result = new WorkRecordFX();
        result.setId(workRecord.getId());
        result.setEmployee(employeeComboBox.getValue());
        result.setNotificationDate(notificationDatePicker.getValue());
        result.setNotificationTime(LocalDateTime.now());
        result.setEbevSerialNumber(ebevSerialField.getText());
        result.setWorkDate(workDatePicker.getValue());
        result.setPayment(new BigDecimal(paymentField.getText()));
        result.setHoursWorked(Integer.parseInt(hoursWorkedField.getText()));
        return result;
    }
}