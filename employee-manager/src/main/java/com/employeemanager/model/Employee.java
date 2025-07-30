package com.employeemanager.model;

import com.employeemanager.util.FirebaseDateConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
public class Employee {

    @Id
    private String id; // Firebase ID-k String típusúak

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_place")
    private String birthPlace;

    @Transient
    private LocalDate birthDate;

    @Column(name = "mother_name")
    private String motherName;

    @Column(name = "tax_number", unique = true)
    private String taxNumber;

    @Column(name = "social_security_number", unique = true)
    private String socialSecurityNumber;

    private String address;

    @Transient
    private LocalDate createdAt;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<WorkRecord> workRecords = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }

    /**
     * Firebase számára Map formátumba konvertál
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("birthPlace", birthPlace);
        map.put("birthDate", FirebaseDateConverter.dateToString(birthDate));
        map.put("motherName", motherName);
        map.put("taxNumber", taxNumber);
        map.put("socialSecurityNumber", socialSecurityNumber);
        map.put("address", address);
        map.put("createdAt", FirebaseDateConverter.dateToString(createdAt));
        return map;
    }

    /**
     * Firebase Map-ből objektummá konvertál
     */
    public static Employee fromMap(Map<String, Object> map) {
        Employee employee = new Employee();
        employee.setId((String) map.get("id"));
        employee.setName((String) map.get("name"));
        employee.setBirthPlace((String) map.get("birthPlace"));
        employee.setBirthDate(FirebaseDateConverter.stringToDate((String) map.get("birthDate")));
        employee.setMotherName((String) map.get("motherName"));
        employee.setTaxNumber((String) map.get("taxNumber"));
        employee.setSocialSecurityNumber((String) map.get("socialSecurityNumber"));
        employee.setAddress((String) map.get("address"));
        employee.setCreatedAt(FirebaseDateConverter.stringToDate((String) map.get("createdAt")));
        return employee;
    }
}