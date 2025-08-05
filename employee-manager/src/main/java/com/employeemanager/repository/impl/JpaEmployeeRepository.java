package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Repository
@Primary
@ConditionalOnProperty(name = "database.type", havingValue = "MYSQL", matchIfMissing = false)
@ConditionalOnProperty(name = "database.type", havingValue = "POSTGRESQL", matchIfMissing = false)
@Transactional
public class JpaEmployeeRepository implements EmployeeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Employee save(Employee entity) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(UUID.randomUUID().toString());
                entityManager.persist(entity);
            } else {
                entity = entityManager.merge(entity);
            }
            entityManager.flush();
            return entity;
        }).get();
    }

    @Override
    public Optional<Employee> findById(String id) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() ->
                Optional.ofNullable(entityManager.find(Employee.class, id))
        ).get();
    }

    @Override
    public List<Employee> findAll() throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            TypedQuery<Employee> query = entityManager.createQuery("SELECT e FROM Employee e", Employee.class);
            return query.getResultList();
        }).get();
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        CompletableFuture.runAsync(() -> {
            Employee entity = entityManager.find(Employee.class, id);
            if (entity != null) {
                entityManager.remove(entity);
            }
        }).get();
    }

    @Override
    public List<Employee> saveAll(List<Employee> entities) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            for (Employee entity : entities) {
                if (entity.getId() == null || entity.getId().isEmpty()) {
                    entity.setId(UUID.randomUUID().toString());
                    entityManager.persist(entity);
                } else {
                    entityManager.merge(entity);
                }
            }
            entityManager.flush();
            return entities;
        }).get();
    }

    @Override
    public Optional<Employee> findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            TypedQuery<Employee> query = entityManager.createQuery(
                    "SELECT e FROM Employee e WHERE e.taxNumber = :taxNumber", Employee.class);
            query.setParameter("taxNumber", taxNumber);
            List<Employee> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }).get();
    }

    @Override
    public Optional<Employee> findBySocialSecurityNumber(String ssn) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            TypedQuery<Employee> query = entityManager.createQuery(
                    "SELECT e FROM Employee e WHERE e.socialSecurityNumber = :ssn", Employee.class);
            query.setParameter("ssn", ssn);
            List<Employee> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }).get();
    }
}