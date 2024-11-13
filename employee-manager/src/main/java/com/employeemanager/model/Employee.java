package com.employeemanager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "mother_name")
    private String motherName;

    @Column(name = "tax_number", unique = true)
    private String taxNumber;

    @Column(name = "social_security_number", unique = true)
    private String socialSecurityNumber;

    private String address;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<WorkRecord> workRecords = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }
}