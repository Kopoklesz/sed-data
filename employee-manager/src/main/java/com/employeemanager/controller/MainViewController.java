package com.employeemanager.controller;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.service.EmployeeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Controller
@RequiredArgsConstructor
public class MainViewController implements Initializable {

    private final EmployeeService employeeService;

    @FXML private TableView<Employee> employeeTable;
    @FXML private TableView<WorkRecord> workRecordTable;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    // Employee table columns
    @FXML private TableColumn<Employee, Long> idColumn;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, String> birthPlaceColumn;
    @FXML private TableColumn<Employee, LocalDate> birthDateColumn;
    @FXML private TableColumn<Employee, String> motherNameColumn;
    @FXML private TableColumn<Employee, String> taxNumberColumn;
    @FXML private TableColumn<Employee, String> socialSecurityColumn;
    @FXML private TableColumn<Employee, String> addressColumn;

    // Work record table columns
    @FXML private TableColumn<WorkRecord, Long> workIdColumn;
    @FXML private TableColumn<WorkRecord, String> employeeNameColumn;
    @FXML private TableColumn<WorkRecord, LocalDate> notificationDateColumn;
    @FXML private TableColumn<WorkRecord, String> ebevSerialColumn;
    @FXML private TableColumn<WorkRecord, LocalDate> workDateColumn;
    @FXML private TableColumn<WorkRecord, Double> paymentColumn;
    @FXML private TableColumn<WorkRecord, Double> hoursWorkedColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupEmployeeTable();
        setupWorkRecordTable();
        loadInitialData();
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
    }

    private void setupWorkRecordTable() {
        workIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        employeeNameColumn.setCellValueFactory(cellData ->
                cellData.getValue().getEmployee().nameProperty());
        notificationDateColumn.setCellValueFactory(new PropertyValueFactory<>("notificationDate"));
        ebevSerialColumn.setCellValueFactory(new PropertyValueFactory<>("ebevSerialNumber"));
        workDateColumn.setCellValueFactory(new PropertyValueFactory<>("workDate"));
        paymentColumn.setCellValueFactory(new PropertyValueFactory<>("payment"));
        hoursWorkedColumn.setCellValueFactory(new PropertyValueFactory<>("hoursWorked"));
    }

    private void loadInitialData() {
        employeeTable.setItems(FXCollections.observableArrayList(
                employeeService.getAllEmployees()));
    }

    @FXML
    private void showAddEmployeeDialog() {
        // TODO: Implement add employee dialog
    }

    @FXML
    private void showAddWorkRecordDialog() {
        // TODO: Implement add work record dialog
    }

    @FXML
    private void showEditEmployeeDialog() {
        Employee selectedEmployee = employeeTable.getSelectionModel().getSelectedItem();
        if (selectedEmployee == null) {
            showAlert("No Selection", "Please select an employee to edit.");
            return;
        }
        // TODO: Implement edit employee dialog
    }

    @FXML
    private void showEditWorkRecordDialog() {
        WorkRecord selectedRecord = workRecordTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null) {
            showAlert("No Selection", "Please select a work record to edit.");
            return;
        }
        // TODO: Implement edit work record dialog
    }

    @FXML
    private void filterWorkRecords() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            showAlert("Invalid Date Range", "Please select both start and end dates.");
            return;
        }

        workRecordTable.setItems(FXCollections.observableArrayList(
                employeeService.getMonthlyRecords(start, end)));
    }

    @FXML
    private void exportToExcel() {
        // TODO: Implement Excel export
    }

    @FXML
    private void exitApplication() {
        System.exit(0);
    }

    @FXML
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Employee Manager");
        alert.setHeaderText(null);
        alert.setContentText("Employee Manager v1.0\n" +
                "A simple application to manage employee data and work records.\n\n" +
                "© 2024 Your Company Name");
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}