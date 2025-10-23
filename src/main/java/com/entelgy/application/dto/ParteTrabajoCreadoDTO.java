package com.entelgy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el resultado del procedure sp_crear_parte_trabajo()
 *
 * Mapea la respuesta del procedure con información del parte creado
 * Incluye: ID, número generado, estado, mensajes y código de error
 *
 * Códigos de error:
 * - 0: OK - Parte creado exitosamente
 * - 1: Contrato no existe
 * - 2: Contrato no está vigente
 * - 3: Fecha inicio es futura (validación)
 * - 4: Horas trabajadas inválidas (<=0)
 * - 5: Violación de unique constraint (número duplicado)
 * - 6: Violación de foreign key
 * - 99: Error inesperado
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParteTrabajoCreadoDTO {

    // ============= Datos del Parte Creado =============
    private Integer parteId;
    private String numero;

    // ============= Status de la Operación =============
    private String estado;              // OK | ERROR
    private String mensaje;             // Descripción del resultado
    private Integer codigoError;        // 0 = OK, 1+ = Error

    // ============= Métodos Helper =============

    /**
     * Indica si la operación fue exitosa
     */
    public boolean isExitoso() {
        return codigoError == 0 && "OK".equals(estado);
    }

    /**
     * Indica si hubo error
     */
    public boolean isError() {
        return codigoError != 0;
    }

    /**
     * Obtiene descripción amigable del error
     */
    public String getDescripcionError() {
        return switch (codigoError) {
            case 0 -> "Operación exitosa";
            case 1 -> "El contrato especificado no existe en la base de datos";
            case 2 -> "El contrato no está en estado VIGENTE. No se pueden crear partes para contratos inactivos";
            case 3 -> "La fecha de inicio no puede ser futura. Debe ser igual o anterior a hoy";
            case 4 -> "Las horas trabajadas deben ser mayor a 0 si se especifican";
            case 5 -> "Ya existe un parte con el mismo número. Intente nuevamente";
            case 6 -> "Error de integridad de datos (foreign key)";
            case 99 -> "Error inesperado en el servidor. Intente nuevamente";
            default -> "Código de error desconocido: " + codigoError;
        };
    }

    /**
     * Obtiene el tipo de error en términos de negocio
     */
    public TipoError getTipoError() {
        return switch (codigoError) {
            case 0 -> TipoError.NINGUNO;
            case 1, 2 -> TipoError.VALIDACION_NEGOCIO;
            case 3, 4 -> TipoError.VALIDACION_DATOS;
            case 5, 6 -> TipoError.INTEGRIDAD_DATOS;
            default -> TipoError.DESCONOCIDO;
        };
    }

    /**
     * Retorna resumen textual del resultado
     */
    public String getResumen() {
        if (isExitoso()) {
            return String.format("✓ Parte creado: %s (ID: %d)", numero, parteId);
        } else {
            return String.format("✗ Error [%d]: %s", codigoError, getDescripcionError());
        }
    }

    @Override
    public String toString() {
        return "ParteTrabajoCreadoDTO{" +
                "parteId=" + parteId +
                ", numero='" + numero + '\'' +
                ", estado='" + estado + '\'' +
                ", codigoError=" + codigoError +
                ", mensaje='" + mensaje + '\'' +
                '}';
    }

    /**
     * Enumeración para tipos de error
     */
    public enum TipoError {
        NINGUNO,                    // 0 - OK
        VALIDACION_NEGOCIO,        // 1-2 - Reglas de negocio
        VALIDACION_DATOS,          // 3-4 - Datos inválidos
        INTEGRIDAD_DATOS,          // 5-6 - Integridad BD
        DESCONOCIDO                // 99 - Otros
    }
}