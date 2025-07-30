package com.employeemanager.model;

import com.employeemanager.util.FirebaseDateConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Entity
@Table(name = "work_records")
public class WorkRecord {

    @Id
    private String id; // Firebase ID-k String típusúak

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Transient
    private LocalDate notificationDate;

    @Transient
    private LocalDateTime notificationTime;

    @Column(name = "ebev_serial", nullable = false)
    private String ebevSerialNumber;

    @Transient
    private LocalDate workDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal payment;

    @Column(name = "hours_worked", nullable = false)
    private Integer hoursWorked;

    @Transient
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (notificationTime == null) {
            notificationTime = LocalDateTime.now();
        }
    }

    /**
     * Firebase számára Map formátumba konvertál
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("employeeId", employee != null ? employee.getId() : null);
        map.put("employeeName", employee != null ? employee.getName() : null);
        map.put("notificationDate", FirebaseDateConverter.dateToString(notificationDate));
        map.put("notificationTime", FirebaseDateConverter.dateTimeToString(notificationTime));
        map.put("ebevSerialNumber", ebevSerialNumber);
        map.put("workDate", FirebaseDateConverter.dateToString(workDate));
        map.put("payment", payment != null ? payment.toString() : "0");
        map.put("hoursWorked", hoursWorked);
        map.put("createdAt", FirebaseDateConverter.dateTimeToString(createdAt));
        return map;
    }

    /**
     * Firebase Map-ből objektummá konvertál (employee nélkül)
     */
    public static WorkRecord fromMap(Map<String, Object> map) {
        WorkRecord record = new WorkRecord();
        record.setId((String) map.get("id"));
        record.setNotificationDate(FirebaseDateConverter.stringToDate((String) map.get("notificationDate")));
        record.setNotificationTime(FirebaseDateConverter.stringToDateTime((String) map.get("notificationTime")));
        record.setEbevSerialNumber((String) map.get("ebevSerialNumber"));
        record.setWorkDate(FirebaseDateConverter.stringToDate((String) map.get("workDate")));

        String paymentStr = (String) map.get("payment");
        if (paymentStr != null && !paymentStr.isEmpty()) {
            record.setPayment(new BigDecimal(paymentStr));
        }

        Object hoursObj = map.get("hoursWorked");
        if (hoursObj instanceof Number) {
            record.setHoursWorked(((Number) hoursObj).intValue());
        }

        record.setCreatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("createdAt")));
        return record;
    }
}