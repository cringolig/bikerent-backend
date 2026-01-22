package com.company.bikerent.common.exception;

public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(Class<?> entityClass, Long id) {
        super(String.format("%s not found with id: %d", entityClass.getSimpleName(), id));
    }
    
    public EntityNotFoundException(Class<?> entityClass, String field, String value) {
        super(String.format("%s not found with %s: %s", entityClass.getSimpleName(), field, value));
    }
    
    public EntityNotFoundException(String message) {
        super(message);
    }
}
