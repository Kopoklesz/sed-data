package com.employeemanager.repository;

import com.employeemanager.model.WorkRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {
    List<WorkRecord> findByEmployeeIdAndWorkDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);
    List<WorkRecord> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);
}