package com.employeemanager.model.fx;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import javafx.beans.property.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WorkRecordFX {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty employeeName = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> notificationDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> notificationTime = new SimpleObjectProperty<>();
    private final StringProperty ebevSerialNumber = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> workDate = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> payment = new SimpleObjectProperty<>();
    private final IntegerProperty hoursWorked = new SimpleIntegerProperty();

    @Getter
    private Employee employee;
    private WorkRecord originalRecord;

    public WorkRecordFX() {
    }

    public WorkRecordFX(WorkRecord record) {
        this.originalRecord = record;
        this.employee = record.getEmployee();

        setId(record.getId());
        setEmployeeName(record.getEmployee().getName());
        setNotificationDate(record.getNotificationDate());
        setNotificationTime(record.getNotificationTime());
        setEbevSerialNumber(record.getEbevSerialNumber());
        setWorkDate(record.getWorkDate());
        setPayment(record.getPayment());
        setHoursWorked(record.getHoursWorked());
    }

    public WorkRecord toWorkRecord() {
        WorkRecord record = originalRecord != null ? originalRecord : new WorkRecord();
        record.setId(getId());
        record.setEmployee(employee);
        record.setNotificationDate(getNotificationDate());
        record.setNotificationTime(getNotificationTime());
        record.setEbevSerialNumber(getEbevSerialNumber());
        record.setWorkDate(getWorkDate());
        record.setPayment(getPayment());
        record.setHoursWorked(getHoursWorked());
        return record;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
        if (employee != null) {
            setEmployeeName(employee.getName());
        }
    }
    public Long getId() {
        return id.get();
    }

    public void setId(Long id) {
        this.id.set(id);
    }

    public LongProperty idProperty() {
        return id;
    }

    public String getEmployeeName() {
        return employeeName.get();
    }

    public void setEmployeeName(String name) {
        this.employeeName.set(name);
    }

    public StringProperty employeeNameProperty() {
        return employeeName;
    }

    public LocalDate getNotificationDate() {
        return notificationDate.get();
    }

    public void setNotificationDate(LocalDate date) {
        this.notificationDate.set(date);
    }

    public ObjectProperty<LocalDate> notificationDateProperty() {
        return notificationDate;
    }

    public LocalDateTime getNotificationTime() {
        return notificationTime.get();
    }

    public void setNotificationTime(LocalDateTime time) {
        this.notificationTime.set(time);
    }

    public ObjectProperty<LocalDateTime> notificationTimeProperty() {
        return notificationTime;
    }

    public String getEbevSerialNumber() {
        return ebevSerialNumber.get();
    }

    public void setEbevSerialNumber(String number) {
        this.ebevSerialNumber.set(number);
    }

    public StringProperty ebevSerialNumberProperty() {
        return ebevSerialNumber;
    }

    public LocalDate getWorkDate() {
        return workDate.get();
    }

    public void setWorkDate(LocalDate date) {
        this.workDate.set(date);
    }

    public ObjectProperty<LocalDate> workDateProperty() {
        return workDate;
    }

    public BigDecimal getPayment() {
        return payment.get();
    }

    public void setPayment(BigDecimal amount) {
        this.payment.set(amount);
    }

    public ObjectProperty<BigDecimal> paymentProperty() {
        return payment;
    }

    public Integer getHoursWorked() {
        return hoursWorked.get();
    }

    public void setHoursWorked(Integer hours) {
        this.hoursWorked.set(hours);
    }

    public IntegerProperty hoursWorkedProperty() {
        return hoursWorked;
    }
}