package com.employeemanager.model;

import com.employeemanager.config.LocalDateAttributeConverter;
import com.employeemanager.util.DateUtil;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }

        if (createdAt != null) {
            createdAtStr = DateUtil.formatDate(createdAt);
        }

        if (birthDate != null) {
            birthDateStr = DateUtil.formatDate(birthDate);
        }
    }

    public String getBirthDateStr() {
        return birthDateStr;
    }

    public void setBirthDateStr(String dateStr) {
        this.birthDateStr = dateStr;
        // Ne állítsuk be automatikusan a birthDate-et
    }

    public void setBirthDate(LocalDate date) {
        this.birthDate = date;
        if (date != null) {
            this.birthDateStr = DateUtil.formatDate(date);
        } else {
            this.birthDateStr = null;
        }
    }

    public void setCreatedAtStr(String dateStr) {
        this.createdAtStr = dateStr;
    }

    public String getCreatedAtStr() {
        return createdAtStr;
    }

    public void setCreatedAt(LocalDate date) {
        this.createdAt = date;
        if (date != null) {
            this.createdAtStr = DateUtil.formatDate(date);
        } else {
            this.createdAtStr = null;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("birthPlace", birthPlace);
        map.put("birthDateStr", birthDateStr);
        map.put("motherName", motherName);
        map.put("taxNumber", taxNumber);
        map.put("socialSecurityNumber", socialSecurityNumber);
        map.put("address", address);
        map.put("createdAtStr", createdAtStr);
        return map;
    }
}