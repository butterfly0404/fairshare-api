package com.alishri.fairshare.common;

/**
 * Thrown when a requested entity does not exist. Mapped to HTTP 404 by the
 * global exception handler, so no controller has to think about status codes.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Long id) {
        super("%s %d not found".formatted(entity, id));
    }
}