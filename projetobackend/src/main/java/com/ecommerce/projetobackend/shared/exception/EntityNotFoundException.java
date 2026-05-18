package com.ecommerce.projetobackend.shared.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String resource, Long id) {
        super("Resource not found: " + resource + " with id " + id);
    }
}
