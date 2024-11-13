package com.employeemanager.model.fx;

import com.employeemanager.model.Employee;
import javafx.beans.property.*;
import java.time.LocalDate;

public class EmployeeFX {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty birthPlace = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> birthDate = new SimpleObjectProperty<>();
    private final StringProperty motherName = new SimpleStringProperty();
    private final StringProperty taxNumber = new SimpleStringProperty();
    private final StringProperty socialSecurityNumber = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> createdAt = new SimpleObjectProperty<>();

    public EmployeeFX() {
    }

    public EmployeeFX(Employee employee) {
        setId(employee.getId());
        setName(employee.getName());
        setBirthPlace(employee.getBirthPlace());
        setBirthDate(employee.getBirthDate());
        setMotherName(employee.getMotherName());
        setTaxNumber(employee.getTaxNumber());
        setSocialSecurityNumber(employee.getSocialSecurityNumber());
        setAddress(employee.getAddress());
        setCreatedAt(employee.getCreatedAt());
    }

    public Employee toEmployee() {
        Employee employee = new Employee();
        employee.setId(getId());
        employee.setName(getName());
        employee.setBirthPlace(getBirthPlace());
        employee.setBirthDate(getBirthDate());
        employee.setMotherName(getMotherName());
        employee.setTaxNumber(getTaxNumber());
        employee.setSocialSecurityNumber(getSocialSecurityNumber());
        employee.setAddress(getAddress());
        employee.setCreatedAt(getCreatedAt());
        return employee;
    }

    // Getter/Setter és Property metódusok
    public Long getId() {
        return id.get();
    }

    public void setId(Long id) {
        this.id.set(id);
    }

    public LongProperty idProperty() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getBirthPlace() {
        return birthPlace.get();
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace.set(birthPlace);
    }

    public StringProperty birthPlaceProperty() {
        return birthPlace;
    }

    public LocalDate getBirthDate() {
        return birthDate.get();
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate.set(birthDate);
    }

    public ObjectProperty<LocalDate> birthDateProperty() {
        return birthDate;
    }

    public String getMotherName() {
        return motherName.get();
    }

    public void setMotherName(String motherName) {
        this.motherName.set(motherName);
    }

    public StringProperty motherNameProperty() {
        return motherName;
    }

    public String getTaxNumber() {
        return taxNumber.get();
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber.set(taxNumber);
    }

    public StringProperty taxNumberProperty() {
        return taxNumber;
    }

    public String getSocialSecurityNumber() {
        return socialSecurityNumber.get();
    }

    public void setSocialSecurityNumber(String socialSecurityNumber) {
        this.socialSecurityNumber.set(socialSecurityNumber);
    }

    public StringProperty socialSecurityNumberProperty() {
        return socialSecurityNumber;
    }

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    public StringProperty addressProperty() {
        return address;
    }

    public LocalDate getCreatedAt() {
        return createdAt.get();
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt.set(createdAt);
    }

    public ObjectProperty<LocalDate> createdAtProperty() {
        return createdAt;
    }

    protected void onCreate() {
        setCreatedAt(LocalDate.now());
    }
}