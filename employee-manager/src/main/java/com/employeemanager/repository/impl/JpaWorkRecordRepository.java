package com.employeemanager.repository.impl;

import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Repository
@Primary
@ConditionalOnExpression("'${database.type}' == 'MYSQL' or '${database.type}' == 'POSTGRESQL'")
@Transactional
public class JpaWorkRecordRepository implements WorkRecordRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public WorkRecord save(WorkRecord entity) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            WorkRecord managedEntity = entity;
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(UUID.randomUUID().toString());
                entityManager.persist(entity);
            } else {
                managedEntity = entityManager.merge(entity);
            }
            entityManager.flush();
            return managedEntity;
        }).get();
    }

    @Override
    public Optional<WorkRecord> findById(String id) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() ->
                Optional.ofNullable(entityManager.find(WorkRecord.class, id))
        ).get();
    }

    @Override
    public List<WorkRecord> findAll() throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            TypedQuery<WorkRecord> query = entityManager.createQuery("SELECT w FROM WorkRecord w", WorkRecord.class);
            return query.getResultList();
        }).get();
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        CompletableFuture.runAsync(() -> {
            WorkRecord entity = entityManager.find(WorkRecord.class, id);
            if (entity != null) {
                entityManager.remove(entity);
            }
        }).get();
    }

    @Override
    public List<WorkRecord> saveAll(List<WorkRecord> entities) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            for (WorkRecord entity : entities) {
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
    public List<WorkRecord> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            TypedQuery<WorkRecord> query = entityManager.createQuery(
                    "SELECT w FROM WorkRecord w WHERE w.employee.id = :employeeId " +
                            "AND w.workDate BETWEEN :startDate AND :endDate ORDER BY w.workDate DESC",
                    WorkRecord.class);
            query.setParameter("employeeId", employeeId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        }).get();
    }

    @Override
    public List<WorkRecord> findByWorkDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            TypedQuery<WorkRecord> query = entityManager.createQuery(
                    "SELECT w FROM WorkRecord w WHERE w.workDate BETWEEN :startDate AND :endDate " +
                            "ORDER BY w.workDate DESC", WorkRecord.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        }).get();
    }

    @Override
    public List<WorkRecord> findByNotificationDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            TypedQuery<WorkRecord> query = entityManager.createQuery(
                    "SELECT w FROM WorkRecord w WHERE w.notificationDate BETWEEN :startDate AND :endDate " +
                            "ORDER BY w.notificationDate DESC", WorkRecord.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        }).get();
    }

    @Override
    public List<WorkRecord> findByNotificationDateAndWorkDateBetween(
            LocalDate notifStart, LocalDate notifEnd, LocalDate workStart, LocalDate workEnd)
            throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            TypedQuery<WorkRecord> query = entityManager.createQuery(
                    "SELECT w FROM WorkRecord w WHERE w.notificationDate BETWEEN :notifStart AND :notifEnd " +
                            "AND w.workDate BETWEEN :workStart AND :workEnd ORDER BY w.workDate DESC",
                    WorkRecord.class);
            query.setParameter("notifStart", notifStart);
            query.setParameter("notifEnd", notifEnd);
            query.setParameter("workStart", workStart);
            query.setParameter("workEnd", workEnd);
            return query.getResultList();
        }).get();
    }
}