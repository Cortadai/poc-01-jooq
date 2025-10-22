package com.entelgy.presentation;

import com.entelgy.infrastructure.exception.DuplicateEntryException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones
 *
 * Centraliza el manejo de todas las excepciones de la aplicación
 * Convierte excepciones a respuestas HTTP estándar con formato consistente
 *
 * Orden de evaluación (importante):
 * 1. Excepciones específicas (EntityNotFoundException, IllegalArgumentException, etc)
 * 2. Exception genérica (last resort)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja IllegalArgumentException
     * Lanzada cuando la validación de argumentos falla
     *
     * HTTP: 400 BAD_REQUEST
     *
     * Ejemplo:
     * throw new IllegalArgumentException("ID debe ser mayor que 0");
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validación de argumento fallida: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Maneja MethodArgumentNotValidException
     * Lanzada cuando la validación @Valid falla en @RequestBody
     *
     * HTTP: 400 BAD_REQUEST
     *
     * Ejemplo JSON:
     * {
     *   "timestamp": "2025-10-22T10:50:00",
     *   "status": 400,
     *   "error": "Validation Error",
     *   "message": "Errores en los datos de entrada",
     *   "errors": {
     *     "nombre": "must not be blank",
     *     "email": "must be a valid email"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {
        log.warn("Errores de validación en request: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Error");
        response.put("message", "Errores en los datos de entrada");

        // Mapear errores de campo específicos
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        response.put("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Maneja EntityNotFoundException
     * Lanzada cuando no se encuentra un recurso solicitado
     *
     * HTTP: 404 NOT_FOUND
     *
     * Ejemplo:
     * throw new EntityNotFoundException("Contrato con ID 999 no encontrado");
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        log.warn("Entidad no encontrada: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Maneja DuplicateEntryException
     * Lanzada cuando se intenta crear un recurso que ya existe
     *
     * HTTP: 409 CONFLICT
     *
     * Ejemplo:
     * throw new DuplicateEntryException("Cliente con email ya existe");
     */
    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateEntryException ex) {
        log.warn("Entrada duplicada: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict");
        response.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * Maneja AccessDeniedException
     * Lanzada cuando no hay permisos para acceder a un recurso
     *
     * HTTP: 403 FORBIDDEN
     *
     * Ejemplo:
     * throw new AccessDeniedException("No tienes permisos para esta operación");
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("error", "Forbidden");
        response.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * Maneja cualquier otra excepción no capturada
     * IMPORTANTE: Este handler debe ser el último (Spring lo evalúa al final)
     *
     * HTTP: 500 INTERNAL_SERVER_ERROR
     *
     * Nunca revela detalles internos al usuario (seguridad)
     * Loguea el stack trace completo (para debugging)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error inesperado no capturado", ex);  // ← Stack trace completo en logs

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "Ocurrió un error inesperado en el servidor");
        // NO incluir: ex.getMessage() - para no exponer detalles internos

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}