package com.employeemanager.repository.impl;

import com.employeemanager.repository.interfaces.BaseRepository;
import com.google.cloud.firestore.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class BaseFirebaseRepository<T> implements BaseRepository<T, String> {
    protected final Firestore firestore;
    protected final String collectionName;
    protected final Class<T> entityClass;

    protected BaseFirebaseRepository(Firestore firestore, String collectionName, Class<T> entityClass) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        this.entityClass = entityClass;
    }

    @Override
    public T save(T entity) throws ExecutionException, InterruptedException {
        String id = getEntityId(entity);
        if (id == null) {
            DocumentReference docRef = firestore.collection(collectionName).document();
            setEntityId(entity, docRef.getId());
        }

        firestore.collection(collectionName)
                .document(getEntityId(entity))
                .set(entity)
                .get();

        return entity;
    }

    @Override
    public List<T> saveAll(List<T> entities) throws ExecutionException, InterruptedException {
        WriteBatch batch = firestore.batch();
        List<T> savedEntities = new ArrayList<>();

        for (T entity : entities) {
            String id = getEntityId(entity);
            if (id == null) {
                DocumentReference docRef = firestore.collection(collectionName).document();
                setEntityId(entity, docRef.getId());
            }
            DocumentReference docRef = firestore.collection(collectionName).document(getEntityId(entity));
            batch.set(docRef, entity);
            savedEntities.add(entity);
        }

        batch.commit().get();
        return savedEntities;
    }

    @Override
    public Optional<T> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection(collectionName)
                .document(id)
                .get()
                .get();

        return Optional.ofNullable(document.exists() ? document.toObject(entityClass) : null);
    }

    @Override
    public List<T> findAll() throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(collectionName).get().get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(entityClass))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        firestore.collection(collectionName)
                .document(id)
                .delete()
                .get();
    }

    protected abstract String getEntityId(T entity);
    protected abstract void setEntityId(T entity, String id);
}