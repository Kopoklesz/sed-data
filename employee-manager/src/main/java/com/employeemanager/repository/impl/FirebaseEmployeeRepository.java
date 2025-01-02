package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Repository
@Slf4j
public class FirebaseEmployeeRepository extends BaseFirebaseRepository<Employee> implements EmployeeRepository {

    public FirebaseEmployeeRepository(Firestore firestore) {
        super(firestore, "employees", Employee.class);
    }

    @Override
    protected String getEntityId(Employee employee) {
        return employee.getId() != null ? employee.getId().toString() : null;
    }

    @Override
    protected void setEntityId(Employee employee, String id) {
        employee.setId(Long.valueOf(id));
    }

    @Override
    public Optional<Employee> findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException {
        QuerySnapshot query = firestore.collection(collectionName)
                .whereEqualTo("taxNumber", taxNumber)
                .get()
                .get();

        return query.getDocuments().stream()
            .findFirst()
            .map(this::convertFromDocument);
    }

    @Override
    public Optional<Employee> findBySocialSecurityNumber(String ssn) throws ExecutionException, InterruptedException {
        QuerySnapshot query = firestore.collection(collectionName)
                .whereEqualTo("socialSecurityNumber", ssn)
                .get()
                .get();

        return query.getDocuments().stream()
            .findFirst()
            .map(this::convertFromDocument);
    }

    protected Employee convertFromDocument(DocumentSnapshot document) {
        Employee employee = new Employee();
        employee.setId(Long.parseLong(document.getId()));
        employee.setName(document.getString("name"));
        employee.setBirthPlace(document.getString("birthPlace"));
        
        String birthDateStr = document.getString("birthDateStr");
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            try {
                LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                employee.setBirthDate(birthDate);
            } catch (Exception e) {
                log.error("Error parsing birth date: " + birthDateStr, e);
            }
        }
        
        employee.setMotherName(document.getString("motherName"));
        employee.setTaxNumber(document.getString("taxNumber"));
        employee.setSocialSecurityNumber(document.getString("socialSecurityNumber"));
        employee.setAddress(document.getString("address"));
        
        String createdAtStr = document.getString("createdAtStr");
        if (createdAtStr != null) {
            try {
                employee.setCreatedAtStr(createdAtStr);
                employee.setCreatedAt(LocalDate.parse(createdAtStr));
            } catch (Exception e) {
                log.error("Error parsing created date: " + createdAtStr, e);
            }
        }
        
        return employee;
    }
}
