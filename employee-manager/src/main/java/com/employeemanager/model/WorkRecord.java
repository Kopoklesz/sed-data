package com.employeemanager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

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

    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;

    @Column(name = "notification_time", nullable = false)
    private LocalDateTime notificationTime;

    @Column(name = "ebev_serial", nullable = false)
    private String ebevSerialNumber;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private BigDecimal payment;

    @Column(name = "hours_worked", nullable = false)
    private Double hoursWorked;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}