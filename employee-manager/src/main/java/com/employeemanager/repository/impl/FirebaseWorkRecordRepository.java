package com.employeemanager.repository.impl;

import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class FirebaseWorkRecordRepository extends BaseFirebaseRepository<WorkRecord> implements WorkRecordRepository {

    public FirebaseWorkRecordRepository(Firestore firestore) {
        super(firestore, "workrecords", WorkRecord.class);
    }

    @Override
    protected String getEntityId(WorkRecord workRecord) {
        return workRecord.getId() != null ? workRecord.getId().toString() : null;
    }

    @Override
    protected void setEntityId(WorkRecord workRecord, String id) {
        workRecord.setId(Long.valueOf(id));
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
        QuerySnapshot querySnapshot = firestore.collection(collectionName)
                .whereGreaterThanOrEqualTo("workDate", startDate)
                .whereLessThanOrEqualTo("workDate", endDate)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(WorkRecord.class))
                .collect(Collectors.toList());
    }
}