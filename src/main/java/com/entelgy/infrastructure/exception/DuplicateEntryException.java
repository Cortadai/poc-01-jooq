package com.entelgy.infrastructure.exception;

/**
 * Excepción lanzada cuando se intenta crear un recurso que ya existe
 *
 * Uso:
 * if (clienteYaExiste(email)) {
 *     throw new DuplicateEntryException("Cliente con email " + email + " ya existe");
 * }
 *
 * Retorna HTTP 409 CONFLICT al usuario (manejado por GlobalExceptionHandler)
 */
public class DuplicateEntryException extends RuntimeException {

    /**
     * Constructor con mensaje
     */
    public DuplicateEntryException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa
     */
    public DuplicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}