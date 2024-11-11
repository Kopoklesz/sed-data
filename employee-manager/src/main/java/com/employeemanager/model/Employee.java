package com.employeemanager.model;

import jakarta.persistence.*;
import javafx.beans.property.*;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@NoArgsConstructor
public class Employee {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty birthPlace = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> birthDate = new SimpleObjectProperty<>();
    private final StringProperty motherName = new SimpleStringProperty();
    private final StringProperty taxNumber = new SimpleStringProperty();
    private final StringProperty socialSecurityNumber = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> createdAt = new SimpleObjectProperty<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id.get();
    }

    public void setId(Long id) {
        this.id.set(id);
    }

    public LongProperty idProperty() {
        return id;
    }

    @Column(nullable = false)
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    @Column(name = "birth_place")
    public String getBirthPlace() {
        return birthPlace.get();
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace.set(birthPlace);
    }

    public StringProperty birthPlaceProperty() {
        return birthPlace;
    }

    @Column(name = "birth_date")
    public LocalDate getBirthDate() {
        return birthDate.get();
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate.set(birthDate);
    }

    public ObjectProperty<LocalDate> birthDateProperty() {
        return birthDate;
    }

    @Column(name = "mother_name")
    public String getMotherName() {
        return motherName.get();
    }

    public void setMotherName(String motherName) {
        this.motherName.set(motherName);
    }

    public StringProperty motherNameProperty() {
        return motherName;
    }

    @Column(name = "tax_number", unique = true)
    public String getTaxNumber() {
        return taxNumber.get();
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber.set(taxNumber);
    }

    public StringProperty taxNumberProperty() {
        return taxNumber;
    }

    @Column(name = "social_security_number", unique = true)
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

    @Column(name = "created_at")
    public LocalDate getCreatedAt() {
        return createdAt.get();
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt.set(createdAt);
    }

    public ObjectProperty<LocalDate> createdAtProperty() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        setCreatedAt(LocalDate.now());
    }
}