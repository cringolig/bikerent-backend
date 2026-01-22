package com.company.bikerent.common.exception;

public class ConcurrentModificationException extends RuntimeException {

  public ConcurrentModificationException(String message) {
    super(message);
  }

  public ConcurrentModificationException(Class<?> entityClass, Long id) {
    super(
        String.format(
            "%s with id %d was modified by another transaction", entityClass.getSimpleName(), id));
  }
}
