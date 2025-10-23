package com.entelgy.infrastructure.repository;

import com.entelgy.application.dto.CarteraReporteDTO;
import com.entelgy.application.dto.ParteTrabajoCreadoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * Repository JOOQ para ejecución de Stored Procedures
 *
 * Responsabilidades:
 * - Ejecutar procedures en la BD
 * - Mapear resultados a DTOs
 * - Manejo de parámetros de entrada/salida
 * - Logging y trazabilidad
 *
 * Procedures disponibles:
 * 1. sp_actualizar_estado_contratos() - Actualizar contratos vencidos
 * 2. sp_generar_reporte_cartera() - Estadísticas por cliente
 * 3. sp_crear_parte_trabajo() - Crear parte con validaciones
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProcedureRepository {

    private final DSLContext dsl;

    // ============= PROCEDURE 1: Actualizar Estado Contratos =============

    /**
     * Ejecuta el procedure que actualiza automáticamente los contratos vencidos
     *
     * Lógica:
     * - Cambia estado 'VIGENTE' a 'VENCIDO' si fecha_fin < hoy
     * - Registra fecha y usuario de modificación
     * - Retorna cantidad de registros actualizados
     *
     * @return Resultado con total actualizado, fecha ejecución y mensaje
     *
     * SQL Ejecutado:
     * SELECT * FROM sp_actualizar_estado_contratos()
     */
    public ProcedureResultDTO actualizarEstadoContratos() {
        log.info("Ejecutando procedure: sp_actualizar_estado_contratos()");

        try {
            Result<Record> result = dsl
                    .fetch("SELECT * FROM sp_actualizar_estado_contratos()");

            if (result.isEmpty()) {
                log.warn("sp_actualizar_estado_contratos retornó vacío");
                return new ProcedureResultDTO(0, null, "Sin resultados");
            }

            Record record = result.get(0);
            int totalActualizados = record.get("total_actualizados", Integer.class);
            Timestamp fechaEjecucion = record.get("fecha_ejecucion", Timestamp.class);
            String mensaje = record.get("mensaje", String.class);

            log.info("Procedure completado: {} contratos actualizados", totalActualizados);

            return new ProcedureResultDTO(totalActualizados, fechaEjecucion, mensaje);

        } catch (Exception e) {
            log.error("Error ejecutando sp_actualizar_estado_contratos", e);
            throw new RuntimeException("Error en procedure: " + e.getMessage(), e);
        }
    }

    // ============= PROCEDURE 2: Generar Reporte Cartera =============

    /**
     * Ejecuta el procedure que genera estadísticas completas de la cartera
     *
     * Información retornada por cliente:
     * - Total de contratos (vigentes, vencidos, cancelados, suspendidos)
     * - Volumen económico (total, vigentes, vencidos)
     * - Precio promedio
     * - Porcentaje de coberturas (material, mano de obra)
     * - Próximo vencimiento y días para vencer
     * - Estado de cartera (NORMAL, ALERTA, CRÍTICO, etc)
     *
     * @return Lista de reportes, uno por cliente
     *
     * SQL Ejecutado:
     * SELECT * FROM sp_generar_reporte_cartera()
     */
    public List<CarteraReporteDTO> generarReporteCartera() {
        log.info("Ejecutando procedure: sp_generar_reporte_cartera()");

        try {
            Result<Record> result = dsl
                    .fetch("SELECT * FROM sp_generar_reporte_cartera()");

            if (result.isEmpty()) {
                log.warn("sp_generar_reporte_cartera retornó vacío");
                return List.of();
            }

            List<CarteraReporteDTO> reportes = result
                    .into(CarteraReporteDTO.class);

            log.info("Procedure completado: {} clientes en reporte", reportes.size());

            return reportes;

        } catch (Exception e) {
            log.error("Error ejecutando sp_generar_reporte_cartera", e);
            throw new RuntimeException("Error en procedure: " + e.getMessage(), e);
        }
    }

    /**
     * Versión alternativa del reporte, retornando Map más flexible
     * Útil si los nombres de columnas del DTO no coinciden perfectamente
     *
     * @return Lista de records con todos los campos
     */
    public List<Record> generarReporteCarteraRaw() {
        log.debug("Ejecutando procedure (raw): sp_generar_reporte_cartera()");

        return dsl
                .fetch("SELECT * FROM sp_generar_reporte_cartera()")
                .stream()
                .toList();
    }

    // ============= PROCEDURE 3: Crear Parte Trabajo =============

    /**
     * Ejecuta el procedure que crea un nuevo parte de trabajo
     *
     * Validaciones automáticas:
     * 1. Contrato debe existir
     * 2. Contrato debe estar en estado VIGENTE
     * 3. fecha_inicio no puede ser en el futuro
     * 4. horas_trabajadas debe ser > 0 si se proporciona
     *
     * @param contratoId ID del contrato asociado
     * @param descripcion Descripción del trabajo
     * @param tipoTrabajo Tipo de trabajo (ej: "Revisión", "Mantenimiento")
     * @return DTO con resultado: ID, número, estado y código de error
     *
     * SQL Ejecutado:
     * SELECT * FROM sp_crear_parte_trabajo(p_contrato_id, p_descripcion, p_tipo_trabajo)
     *
     * @throws RuntimeException si hay error validando o creando el parte
     */
    public ParteTrabajoCreadoDTO crearParteTrabajo(
            Integer contratoId,
            String descripcion,
            String tipoTrabajo) {

        log.info("Ejecutando procedure: sp_crear_parte_trabajo(contratoId={}, tipo={})",
                contratoId, tipoTrabajo);

        try {
            Result<Record> result = dsl
                    .fetch(
                            "SELECT * FROM sp_crear_parte_trabajo(?, ?, ?)",
                            contratoId,
                            descripcion,
                            tipoTrabajo
                    );

            if (result.isEmpty()) {
                throw new RuntimeException("sp_crear_parte_trabajo retornó vacío");
            }

            Record record = result.get(0);
            Integer parteId = record.get("parte_id", Integer.class);
            String numero = record.get("numero", String.class);
            String estado = record.get("estado", String.class);
            String mensaje = record.get("mensaje", String.class);
            Integer codigoError = record.get("codigo_error", Integer.class);

            // Si hay error, lanzar excepción
            if (codigoError != 0) {
                log.warn("Error en sp_crear_parte_trabajo: [{}] {}", codigoError, mensaje);
                throw new RuntimeException(
                        String.format("Error en procedure (código %d): %s", codigoError, mensaje)
                );
            }

            log.info("Parte creado exitosamente: ID={}, Número={}", parteId, numero);

            return new ParteTrabajoCreadoDTO(parteId, numero, estado, mensaje, codigoError);

        } catch (RuntimeException e) {
            log.error("Error ejecutando sp_crear_parte_trabajo", e);
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en sp_crear_parte_trabajo", e);
            throw new RuntimeException("Error en procedure: " + e.getMessage(), e);
        }
    }

    /**
     * Versión avanzada del procedure de crear parte, con más parámetros opcionales
     *
     * @param contratoId ID del contrato
     * @param descripcion Descripción
     * @param tipoTrabajo Tipo de trabajo
     * @param horasTrabajadas Horas trabajadas (opcional)
     * @param usuarioCreacion Usuario que crea (default: SYSTEM)
     * @return DTO con resultado
     */
    public ParteTrabajoCreadoDTO crearParteTrabajoCompleto(
            Integer contratoId,
            String descripcion,
            String tipoTrabajo,
            Double horasTrabajadas,
            String usuarioCreacion) {

        log.info("Ejecutando procedure (completo): sp_crear_parte_trabajo() con horas={}, usuario={}",
                horasTrabajadas, usuarioCreacion);

        try {
            Result<Record> result = dsl
                    .fetch(
                            "SELECT * FROM sp_crear_parte_trabajo(?, ?, ?, CURRENT_TIMESTAMP, ?, ?)",
                            contratoId,
                            descripcion,
                            tipoTrabajo,
                            horasTrabajadas,
                            usuarioCreacion
                    );

            if (result.isEmpty()) {
                throw new RuntimeException("sp_crear_parte_trabajo retornó vacío");
            }

            Record record = result.get(0);
            Integer parteId = record.get("parte_id", Integer.class);
            String numero = record.get("numero", String.class);
            String estado = record.get("estado", String.class);
            String mensaje = record.get("mensaje", String.class);
            Integer codigoError = record.get("codigo_error", Integer.class);

            if (codigoError != 0) {
                log.warn("Error en sp_crear_parte_trabajo: [{}] {}", codigoError, mensaje);
                throw new RuntimeException(
                        String.format("Error en procedure (código %d): %s", codigoError, mensaje)
                );
            }

            log.info("Parte creado exitosamente (completo): ID={}, Número={}", parteId, numero);

            return new ParteTrabajoCreadoDTO(parteId, numero, estado, mensaje, codigoError);

        } catch (RuntimeException e) {
            log.error("Error ejecutando sp_crear_parte_trabajo (completo)", e);
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en sp_crear_parte_trabajo (completo)", e);
            throw new RuntimeException("Error en procedure: " + e.getMessage(), e);
        }
    }

    // ============= DTOs AUXILIARES =============

    /**
     * DTO para resultado de sp_actualizar_estado_contratos
     */
    public static class ProcedureResultDTO {
        public int totalActualizados;
        public Timestamp fechaEjecucion;
        public String mensaje;

        public ProcedureResultDTO(int totalActualizados, Timestamp fechaEjecucion, String mensaje) {
            this.totalActualizados = totalActualizados;
            this.fechaEjecucion = fechaEjecucion;
            this.mensaje = mensaje;
        }

        @Override
        public String toString() {
            return "ProcedureResult{" +
                    "totalActualizados=" + totalActualizados +
                    ", fechaEjecucion=" + fechaEjecucion +
                    ", mensaje='" + mensaje + '\'' +
                    '}';
        }
    }
}