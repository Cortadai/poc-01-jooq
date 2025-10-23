package com.entelgy.application;

import com.entelgy.application.dto.CarteraReporteDTO;
import com.entelgy.application.dto.ParteTrabajoCreadoDTO;
import com.entelgy.infrastructure.repository.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application Service para Procedures
 *
 * Responsabilidades:
 * - Orquestar la ejecución de stored procedures
 * - Manejar transacciones
 * - Validar parámetros de entrada
 * - Logging y auditoría
 * - Manejo de excepciones
 *
 * Procedures disponibles:
 * 1. Actualizar estado contratos (batch automático)
 * 2. Generar reporte de cartera (análisis y estadísticas)
 * 3. Crear parte de trabajo (con validaciones automáticas)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcedureApplicationService {

    private final ProcedureRepository procedureRepository;

    // ============= PROCEDURE 1: Actualizar Estado Contratos =============

    /**
     * Ejecuta el procedure que actualiza automáticamente los contratos vencidos
     *
     * Caso de uso:
     * - Ejecutar diariamente (mediante scheduler/cron)
     * - Mantener estado consistente de contratos
     * - Cambiar VIGENTE a VENCIDO cuando fecha_fin < hoy
     *
     * Flujo:
     * 1. Llamar procedure
     * 2. Registrar resultado en logs
     * 3. Retornar información de actualización
     *
     * @return Información de contratos actualizados
     * @throws RuntimeException si falla la ejecución del procedure
     */
    @Transactional
    public ProcedureRepository.ProcedureResultDTO actualizarContratosvencidos() {
        log.info("=== Iniciando actualización de contratos vencidos ===");

        try {
            ProcedureRepository.ProcedureResultDTO resultado = procedureRepository.actualizarEstadoContratos();

            log.info("Actualización completada: {} contratos vencidos. Mensaje: {}",
                    resultado.totalActualizados,
                    resultado.mensaje);

            return resultado;

        } catch (Exception e) {
            log.error("Error al actualizar contratos vencidos", e);
            throw new RuntimeException("No se pudieron actualizar los contratos vencidos: " + e.getMessage(), e);
        }
    }

    // ============= PROCEDURE 2: Generar Reporte Cartera =============

    /**
     * Ejecuta el procedure que genera estadísticas completas de la cartera
     *
     * Caso de uso:
     * - Generar reportes gerenciales
     * - Análisis de cartera por cliente
     * - Identificar clientes en riesgo
     * - Dashboard de estado de contratos
     *
     * Información retornada por cliente:
     * - Cantidad de contratos por estado
     * - Volúmenes económicos
     * - Coberturas (material, mano de obra)
     * - Próximo vencimiento
     * - Estado general (NORMAL, ALERTA, CRÍTICO, etc)
     *
     * Flujo:
     * 1. Llamar procedure
     * 2. Mapear resultados a DTOs
     * 3. Registrar en logs
     * 4. Retornar lista de reportes
     *
     * @return Lista de reportes, uno por cliente
     * @throws RuntimeException si falla la ejecución
     */
    @Transactional(readOnly = true)
    public List<CarteraReporteDTO> obtenerReporteCartera() {
        log.info("=== Generando reporte de cartera ===");

        try {
            List<CarteraReporteDTO> reportes = procedureRepository.generarReporteCartera();

            log.info("Reporte generado: {} clientes analizados", reportes.size());

            // Log adicional de clientes en riesgo
            long clientesEnRiesgo = reportes.stream()
                    .filter(CarteraReporteDTO::tieneRiesgo)
                    .count();

            if (clientesEnRiesgo > 0) {
                log.warn("ALERTA: {} clientes con cartera en riesgo (CRÍTICO o VENCIDO)",
                        clientesEnRiesgo);
            }

            return reportes;

        } catch (Exception e) {
            log.error("Error al generar reporte de cartera", e);
            throw new RuntimeException("No se pudo generar el reporte de cartera: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el reporte de cartera y filtra solo clientes en alerta
     *
     * Útil para dashboards ejecutivos que solo muestran clientes problemáticos
     *
     * @return Lista de reportes filtrada (solo estados CRÍTICO, VENCIDO, ALERTA)
     */
    @Transactional(readOnly = true)
    public List<CarteraReporteDTO> obtenerReporteCarteraEnAlerta() {
        log.info("Obteniendo reporte de cartera en ALERTA");

        return obtenerReporteCartera().stream()
                .filter(CarteraReporteDTO::tieneAlerta)
                .toList();
    }

    /**
     * Obtiene reporte de cartera y filtra por volumen mínimo
     *
     * Útil para análisis de 80/20 (clientes con mayor volumen)
     *
     * @param volumenMinimo Volumen mínimo en euros
     * @return Lista de clientes con volumen >= volumenMinimo
     */
    @Transactional(readOnly = true)
    public List<CarteraReporteDTO> obtenerReporteCarteraPorVolumen(Double volumenMinimo) {
        log.info("Obteniendo reporte de cartera con volumen >= {}", volumenMinimo);

        return obtenerReporteCartera().stream()
                .filter(r -> r.getVolumenVigentes().doubleValue() >= volumenMinimo)
                .toList();
    }

    /**
     * Análisis de cobertura: cliente más asegurado
     *
     * @return Reporte del cliente con mejor cobertura
     */
    @Transactional(readOnly = true)
    public CarteraReporteDTO obtenerClienteConMejorCobertura() {
        log.debug("Buscando cliente con mejor cobertura");

        return obtenerReporteCartera().stream()
                .filter(r -> r.getTotalContratos() > 0)
                .max((r1, r2) -> Double.compare(
                        r1.getPorcentajeCoberturasMaterial().doubleValue(),
                        r2.getPorcentajeCoberturasMaterial().doubleValue()
                ))
                .orElse(null);
    }

    /**
     * Resumen ejecutivo del reporte
     *
     * @return Información resumida de la cartera
     */
    @Transactional(readOnly = true)
    public ResumenCarteraDTO obtenerResumenEjecutivo() {
        log.info("Generando resumen ejecutivo de cartera");

        List<CarteraReporteDTO> reportes = obtenerReporteCartera();

        return ResumenCarteraDTO.builder()
                .totalClientes(reportes.size())
                .totalContratos(reportes.stream().mapToLong(CarteraReporteDTO::getTotalContratos).sum())
                .totalContratosVigentes(reportes.stream().mapToLong(CarteraReporteDTO::getContratosVigentes).sum())
                .volumenTotal(reportes.stream()
                        .map(CarteraReporteDTO::getVolumenTotal)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                .volumenVigentes(reportes.stream()
                        .map(CarteraReporteDTO::getVolumenVigentes)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                .clientesEnRiesgo(reportes.stream().filter(CarteraReporteDTO::tieneRiesgo).count())
                .clientesEnAlerta(reportes.stream().filter(CarteraReporteDTO::tieneAlerta).count())
                .build();
    }

    // ============= PROCEDURE 3: Crear Parte Trabajo =============

    /**
     * Crea un nuevo parte de trabajo con validaciones automáticas
     *
     * Caso de uso:
     * - Registrar trabajos realizados
     * - Crear partes de incidencias
     * - Registrar mantenimiento
     *
     * Validaciones automáticas:
     * 1. Contrato debe existir
     * 2. Contrato debe estar VIGENTE
     * 3. fecha_inicio no puede ser futura
     * 4. horas_trabajadas debe ser > 0
     *
     * Flujo:
     * 1. Validar entrada
     * 2. Llamar procedure
     * 3. Verificar resultado
     * 4. Registrar en logs
     * 5. Retornar resultado
     *
     * @param contratoId ID del contrato
     * @param descripcion Descripción del trabajo
     * @param tipoTrabajo Tipo de trabajo (ej: "Revisión", "Mantenimiento")
     * @return DTO con información del parte creado o error
     * @throws IllegalArgumentException si los parámetros son inválidos
     * @throws RuntimeException si falla el procedure
     */
    @Transactional
    public ParteTrabajoCreadoDTO crearParteTrabajo(
            Integer contratoId,
            String descripcion,
            String tipoTrabajo) {

        log.info("=== Creando parte de trabajo para contrato {} ===", contratoId);

        // VALIDACIONES DE ENTRADA
        if (contratoId == null || contratoId <= 0) {
            throw new IllegalArgumentException("Contrato ID inválido");
        }

        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("Descripción requerida");
        }

        if (tipoTrabajo == null || tipoTrabajo.trim().isEmpty()) {
            tipoTrabajo = "Mantenimiento";
        }

        try {
            ParteTrabajoCreadoDTO resultado = procedureRepository.crearParteTrabajo(
                    contratoId,
                    descripcion.trim(),
                    tipoTrabajo.trim()
            );

            if (resultado.isExitoso()) {
                log.info("✓ Parte creado exitosamente: {} (ID: {})",
                        resultado.getNumero(),
                        resultado.getParteId());
            } else {
                log.warn("✗ Error creando parte [{}]: {}",
                        resultado.getCodigoError(),
                        resultado.getDescripcionError());
            }

            return resultado;

        } catch (RuntimeException e) {
            log.error("Error al crear parte de trabajo", e);
            throw e;
        }
    }

    /**
     * Versión completa de crear parte con más parámetros
     *
     * @param contratoId ID del contrato
     * @param descripcion Descripción
     * @param tipoTrabajo Tipo de trabajo
     * @param horasTrabajadas Horas dedicadas al trabajo
     * @param usuarioCreacion Usuario que crea el registro
     * @return DTO con resultado
     */
    @Transactional
    public ParteTrabajoCreadoDTO crearParteTrabajoCompleto(
            Integer contratoId,
            String descripcion,
            String tipoTrabajo,
            Double horasTrabajadas,
            String usuarioCreacion) {

        log.info("=== Creando parte de trabajo completo para contrato {} ===", contratoId);

        // VALIDACIONES DE ENTRADA
        if (contratoId == null || contratoId <= 0) {
            throw new IllegalArgumentException("Contrato ID inválido");
        }

        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("Descripción requerida");
        }

        if (tipoTrabajo == null || tipoTrabajo.trim().isEmpty()) {
            tipoTrabajo = "Mantenimiento";
        }

        if (horasTrabajadas != null && horasTrabajadas <= 0) {
            throw new IllegalArgumentException("Horas trabajadas debe ser > 0");
        }

        if (usuarioCreacion == null || usuarioCreacion.trim().isEmpty()) {
            usuarioCreacion = "SYSTEM";
        }

        try {
            ParteTrabajoCreadoDTO resultado = procedureRepository.crearParteTrabajoCompleto(
                    contratoId,
                    descripcion.trim(),
                    tipoTrabajo.trim(),
                    horasTrabajadas,
                    usuarioCreacion.trim()
            );

            if (resultado.isExitoso()) {
                log.info("✓ Parte creado exitosamente: {} (ID: {}, Horas: {})",
                        resultado.getNumero(),
                        resultado.getParteId(),
                        horasTrabajadas);
            } else {
                log.warn("✗ Error creando parte [{}]: {}",
                        resultado.getCodigoError(),
                        resultado.getDescripcionError());
            }

            return resultado;

        } catch (RuntimeException e) {
            log.error("Error al crear parte de trabajo completo", e);
            throw e;
        }
    }

    // ============= DTO AUXILIAR: Resumen Cartera =============

    /**
     * DTO para resumen ejecutivo de la cartera
     */
    @lombok.Data
    @lombok.Builder
    public static class ResumenCarteraDTO {
        private Integer totalClientes;
        private Long totalContratos;
        private Long totalContratosVigentes;
        private java.math.BigDecimal volumenTotal;
        private java.math.BigDecimal volumenVigentes;
        private Long clientesEnRiesgo;
        private Long clientesEnAlerta;

        public String getResumen() {
            return String.format(
                    "Cartera: %d clientes, %d contratos vigentes, Volumen: €%.2f, En riesgo: %d",
                    totalClientes,
                    totalContratosVigentes,
                    volumenVigentes,
                    clientesEnRiesgo
            );
        }
    }
}