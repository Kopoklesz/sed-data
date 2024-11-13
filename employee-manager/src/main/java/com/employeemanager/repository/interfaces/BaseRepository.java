package com.employeemanager.repository.interfaces;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public interface BaseRepository<T, ID> {
    T save(T entity) throws ExecutionException, InterruptedException;
    Optional<T> findById(ID id) throws ExecutionException, InterruptedException;
    List<T> findAll() throws ExecutionException, InterruptedException;
    void deleteById(ID id) throws ExecutionException, InterruptedException;
    List<T> saveAll(List<T> entities) throws ExecutionException, InterruptedException;
}