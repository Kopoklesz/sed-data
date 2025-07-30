package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.employeemanager.util.FirebaseDateConverter;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class FirebaseWorkRecordRepository extends BaseFirebaseRepository<WorkRecord> implements WorkRecordRepository {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public FirebaseWorkRecordRepository(Firestore firestore, EmployeeRepository employeeRepository) {
        super(firestore, "workrecords", WorkRecord.class);
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected String getEntityId(WorkRecord workRecord) {
        return workRecord.getId();
    }

    @Override
    protected void setEntityId(WorkRecord workRecord, String id) {
        workRecord.setId(id);
    }

    @Override
    protected Map<String, Object> convertToMap(WorkRecord workRecord) {
        return workRecord.toMap();
    }

    @Override
    protected WorkRecord convertFromMap(Map<String, Object> data) {
        try {
            WorkRecord record = WorkRecord.fromMap(data);

            // Employee kapcsolat betöltése
            String employeeId = (String) data.get("employeeId");
            if (employeeId != null) {
                try {
                    employeeRepository.findById(employeeId).ifPresent(record::setEmployee);
                } catch (Exception e) {
                    log.error("Error loading employee with ID: {}", employeeId, e);
                    // Minimális employee adatok beállítása
                    Employee employee = new Employee();
                    employee.setId(employeeId);
                    employee.setName((String) data.get("employeeName"));
                    record.setEmployee(employee);
                }
            }

            return record;
        } catch (Exception e) {
            log.error("Error converting map to WorkRecord: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<WorkRecord> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {

        String startDateStr = FirebaseDateConverter.dateToString(startDate);
        String endDateStr = FirebaseDateConverter.dateToString(endDate);

        QuerySnapshot querySnapshot = firestore.collection(collectionName)
                .whereEqualTo("employeeId", employeeId)
                .whereGreaterThanOrEqualTo("workDate", startDateStr)
                .whereLessThanOrEqualTo("workDate", endDateStr)
                .orderBy("workDate", Query.Direction.DESCENDING)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        return convertFromMap(data);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkRecord> findByWorkDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {

        String startDateStr = FirebaseDateConverter.dateToString(startDate);
        String endDateStr = FirebaseDateConverter.dateToString(endDate);

        log.debug("Querying work records between {} and {}", startDateStr, endDateStr);

        QuerySnapshot querySnapshot = firestore.collection(collectionName)
                .whereGreaterThanOrEqualTo("workDate", startDateStr)
                .whereLessThanOrEqualTo("workDate", endDateStr)
                .orderBy("workDate", Query.Direction.DESCENDING)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        return convertFromMap(data);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}