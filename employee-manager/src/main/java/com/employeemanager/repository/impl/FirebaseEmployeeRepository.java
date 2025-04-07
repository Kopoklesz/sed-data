package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.util.DateUtil;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
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
            DateUtil.parseDate(birthDateStr).ifPresent(employee::setBirthDate);
        }

        employee.setMotherName(document.getString("motherName"));
        employee.setTaxNumber(document.getString("taxNumber"));
        employee.setSocialSecurityNumber(document.getString("socialSecurityNumber"));
        employee.setAddress(document.getString("address"));

        String createdAtStr = document.getString("createdAtStr");
        if (createdAtStr != null) {
            employee.setCreatedAtStr(createdAtStr);
            DateUtil.parseDate(createdAtStr).ifPresent(employee::setCreatedAt);
        }

        return employee;
    }

    @Override
    public Employee save(Employee employee) throws ExecutionException, InterruptedException {
        if (employee.getBirthDate() != null && (employee.getBirthDateStr() == null || employee.getBirthDateStr().isEmpty())) {
            employee.setBirthDateStr(employee.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (employee.getCreatedAt() != null && (employee.getCreatedAtStr() == null || employee.getCreatedAtStr().isEmpty())) {
            employee.setCreatedAtStr(employee.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        Map<String, Object> employeeData = new HashMap<>();
        employeeData.put("name", employee.getName());
        employeeData.put("birthPlace", employee.getBirthPlace());
        employeeData.put("birthDateStr", employee.getBirthDateStr());
        employeeData.put("motherName", employee.getMotherName());
        employeeData.put("taxNumber", employee.getTaxNumber());
        employeeData.put("socialSecurityNumber", employee.getSocialSecurityNumber());
        employeeData.put("address", employee.getAddress());
        employeeData.put("createdAtStr", employee.getCreatedAtStr());

        String id = getEntityId(employee);
        if (id == null) {
            DocumentReference docRef = firestore.collection(collectionName).document();
            id = docRef.getId();
            setEntityId(employee, id);
        }

        firestore.collection(collectionName)
                .document(id)
                .set(employeeData)
                .get();

        return employee;
    }
}