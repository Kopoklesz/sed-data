package com.employeemanager.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "work_records")
public class WorkRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;

    @Column(name = "notification_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime notificationTime;

    @Column(name = "ebev_serial", nullable = false)
    private String ebevSerialNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal payment;

    @Column(name = "hours_worked", nullable = false)
    private Integer hoursWorked;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}