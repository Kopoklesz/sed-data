package com.employeemanager.model;

import com.employeemanager.config.LocalDateAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
public class Employee {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "birth_date")
    @Convert(converter = LocalDateAttributeConverter.class)
    private LocalDate birthDate;

    @Column(name = "birth_date_str")
    private String birthDateStr;

    @Column(name = "mother_name")
    private String motherName;

    @Column(name = "tax_number", unique = true)
    private String taxNumber;

    @Column(name = "social_security_number", unique = true)
    private String socialSecurityNumber;

    private String address;

    @Column(name = "created_at")
    @Temporal(TemporalType.DATE)
    private LocalDate createdAt;

    @Column(name = "created_at_str")
    private String createdAtStr;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<WorkRecord> workRecords = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        createdAtStr = createdAt.format(DATE_FORMATTER);
        
        if (birthDate != null) {
            birthDateStr = birthDate.format(DATE_FORMATTER);
        }
    }

    public String getBirthDateStr() {
        if (birthDateStr == null && birthDate != null) {
            birthDateStr = birthDate.format(DATE_FORMATTER);
        }
        return birthDateStr;
    }

    public void setBirthDateStr(String dateStr) {
        this.birthDateStr = dateStr;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                this.birthDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (Exception e) {
                // Log error ha szükséges
            }
        }
    }

    public void setBirthDate(LocalDate date) {
        this.birthDate = date;
        if (date != null) {
            this.birthDateStr = date.format(DATE_FORMATTER);
        } else {
            this.birthDateStr = null;
        }
    }

    public void setCreatedAtStr(String dateStr) {
        this.createdAtStr = dateStr;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                this.createdAt = LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (Exception e) {
                // Log error ha szükséges
            }
        }
    }

    public String getCreatedAtStr() {
        if (createdAtStr == null && createdAt != null) {
            createdAtStr = createdAt.format(DATE_FORMATTER);
        }
        return createdAtStr;
    }

    public void setCreatedAt(LocalDate date) {
        this.createdAt = date;
        if (date != null) {
            this.createdAtStr = date.format(DATE_FORMATTER);
        } else {
            this.createdAtStr = null;
        }
    }
}