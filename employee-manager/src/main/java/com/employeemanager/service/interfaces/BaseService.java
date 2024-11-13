package com.employeemanager.service.interfaces;

import com.employeemanager.service.exception.ServiceException;

import java.util.List;
import java.util.Optional;

public interface BaseService<T, ID> {
    T save(T entity) throws ServiceException;
    Optional<T> findById(ID id) throws ServiceException;
    List<T> findAll() throws ServiceException;
    void deleteById(ID id) throws ServiceException;
    List<T> saveAll(List<T> entities) throws ServiceException;
}