package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
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
                .map(doc -> doc.toObject(Employee.class));
    }

    @Override
    public Optional<Employee> findBySocialSecurityNumber(String ssn) throws ExecutionException, InterruptedException {
        QuerySnapshot query = firestore.collection(collectionName)
                .whereEqualTo("socialSecurityNumber", ssn)
                .get()
                .get();

        return query.getDocuments().stream()
                .findFirst()
                .map(doc -> doc.toObject(Employee.class));
    }
}