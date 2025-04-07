package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import java.util.Objects;

@Repository
@Slf4j
public class FirebaseWorkRecordRepository extends BaseFirebaseRepository<WorkRecord> implements WorkRecordRepository {

    public FirebaseWorkRecordRepository(Firestore firestore) {
        super(firestore, "workrecords", WorkRecord.class);
    }

    @Override
    public List<WorkRecord> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(collectionName)
                .whereEqualTo("employee.id", employeeId)
                .whereGreaterThanOrEqualTo("workDate", startDate)
                .whereLessThanOrEqualTo("workDate", endDate)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(WorkRecord.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkRecord> findByWorkDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {
        log.debug("Querying work records between {} and {}", startDate, endDate);
        
        try {
            QuerySnapshot querySnapshot = firestore.collection(collectionName)
                    .get()
                    .get();

            log.debug("Found {} documents", querySnapshot.size());
            
            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        try {
                            return convertToWorkRecord(doc);
                        } catch (Exception e) {
                            log.error("Error converting document {}: {}", doc.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error querying work records: {}", e.getMessage(), e);
            throw e;
        }
    }

    private WorkRecord convertToWorkRecord(DocumentSnapshot document) {
        try {
            log.debug("Converting document: {}", document.getId());
            
            WorkRecord record = new WorkRecord();
            record.setId(System.currentTimeMillis()); 
            
            Map<String, Object> data = document.getData();
            if (data != null) {
                log.debug("Document data: {}", data);
                
                // Employee adatok konvertálása
                @SuppressWarnings("unchecked")
                Map<String, Object> employeeData = (Map<String, Object>) data.get("employee");
                if (employeeData != null) {
                    Employee employee = new Employee();
                    Object empId = employeeData.get("id");
                    if (empId instanceof Number) {
                        employee.setId(((Number) empId).longValue());
                    }
                    employee.setName((String) employeeData.get("name"));
                    record.setEmployee(employee);
                }
    
                // Alap mezők konvertálása
                record.setHoursWorked(((Number) data.get("hoursWorked")).intValue());
                record.setPayment(BigDecimal.valueOf(((Number) data.get("payment")).longValue()));
                record.setEbevSerialNumber((String) data.get("ebevSerialNumber"));
    
                // Dátumok konvertálása megfelelő formátummal
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                
                try {
                    String workDateStr = (String) data.get("workDateStr");
                    if (workDateStr != null && !workDateStr.trim().isEmpty()) {
                        record.setWorkDate(LocalDate.parse(workDateStr, formatter));
                    }
                } catch (Exception e) {
                    log.error("Error parsing work date: {}", data.get("workDateStr"), e);
                }
    
                try {
                    String notificationDateStr = (String) data.get("notificationDateStr");
                    if (notificationDateStr != null && !notificationDateStr.trim().isEmpty()) {
                        record.setNotificationDate(LocalDate.parse(notificationDateStr, formatter));
                    }
                } catch (Exception e) {
                    log.error("Error parsing notification date: {}", data.get("notificationDateStr"), e);
                }
    
                try {
                    String createdAtStr = (String) data.get("createdAtStr");
                    if (createdAtStr != null && !createdAtStr.trim().isEmpty()) {
                        record.setCreatedAt(LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }
                } catch (Exception e) {
                    log.error("Error parsing created at date: {}", data.get("createdAtStr"), e);
                }
            }
            
            return record;
        } catch (Exception e) {
            log.error("Error converting document {}: Error converting document: {}", document.getId(), e.getMessage());
            return null; // Null visszaadása hiba esetén, hogy a többi rekord feldolgozása folytatódhasson
        }
    }
}