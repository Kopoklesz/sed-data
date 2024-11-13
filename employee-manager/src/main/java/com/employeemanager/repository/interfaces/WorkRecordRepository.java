package com.employeemanager.repository.interfaces;

import com.employeemanager.model.WorkRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface WorkRecordRepository extends BaseRepository<WorkRecord, String> {
    List<WorkRecord> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException;
    List<WorkRecord> findByWorkDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException;
}