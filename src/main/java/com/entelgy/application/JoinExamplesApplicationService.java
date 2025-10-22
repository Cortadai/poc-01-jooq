package com.entelgy.application;

import com.entelgy.application.dto.ClienteConContratosDTO;
import com.entelgy.application.dto.ContratoActivoClienteEmpresaDTO;
import com.entelgy.application.dto.ContratoClienteDTO;
import com.entelgy.application.dto.ContratoDetalleDTO;
import com.entelgy.infrastructure.repository.JoinExamplesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service para reportes y análisis con JOINs
 *
 * Responsabilidades:
 * - Orquestar llamadas al repositorio
 * - Aplicar lógica de negocio
 * - Filtrar y transformar datos
 * - Validar resultados
 * - Loguear operaciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JoinExamplesApplicationService {

    private final JoinExamplesRepository joinRepository;

    // ============= REPORTES CON LÓGICA DE NEGOCIO =============

    /**
     * Reporte: Contratos activos con información de cliente
     *
     * Lógica de negocio:
     * - Solo contratos VIGENTES
     * - Ordena por nombre de cliente
     * - Loguea cantidad de resultados
     * - Valida si hay resultados
     *
     * @return Lista de contratos activos con datos del cliente
     */
    @Transactional(readOnly = true)
    public List<ContratoClienteDTO> reporteContratosActivos() {
        log.info("Iniciando reporte de contratos activos con clientes");

        try {
            List<ContratoClienteDTO> todosLosContratos = joinRepository.getContratosConClientes();

            // Lógica: Filtrar solo VIGENTES
            List<ContratoClienteDTO> contratosActivos = todosLosContratos.stream()
                    .filter(dto -> "VIGENTE".equals(dto.getEstado()))
                    .sorted((a, b) -> a.getClienteNombre().compareTo(b.getClienteNombre()))
                    .collect(Collectors.toList());

            log.info("Reporte generado: {} contratos activos de {} totales",
                    contratosActivos.size(), todosLosContratos.size());

            if (contratosActivos.isEmpty()) {
                log.warn("No se encontraron contratos activos en el sistema");
            } else {
                contratosActivos.forEach(dto ->
                        log.debug("Contrato activo: {} - Cliente: {} - Email: {}",
                                dto.getNumero(), dto.getClienteNombre(), dto.getClienteEmail())
                );
            }

            return contratosActivos;

        } catch (Exception e) {
            log.error("Error al generar reporte de contratos activos", e);
            throw new RuntimeException("Error generando reporte de contratos activos: " + e.getMessage(), e);
        }
    }

    /**
     * Reporte: Clientes sin contratos activos
     *
     * Lógica de negocio:
     * - Identifica clientes sin contratos vigentes
     * - Útil para estrategia de ventas/retención
     * - Loguea clientes de riesgo
     *
     * @return Lista de clientes sin contratos vigentes
     */
    @Transactional(readOnly = true)
    public List<ClienteConContratosDTO> reporteClientesSinContratosActivos() {
        log.info("Iniciando búsqueda de clientes sin contratos vigentes");

        try {
            List<ClienteConContratosDTO> todosClientesConContratos =
                    joinRepository.getTodosClientesConSusContratos();

            // Lógica 1: Clientes sin ningún contrato
            List<ClienteConContratosDTO> clientesSinContrato = todosClientesConContratos.stream()
                    .filter(dto -> dto.getContratoNumero() == null)
                    .collect(Collectors.toList());

            log.info("Clientes sin contratos: {}", clientesSinContrato.size());

            // Lógica 2: Clientes con contratos pero TODOS vencidos (análisis adicional)
            List<String> clientesConSoloVencidos = todosClientesConContratos.stream()
                    .filter(dto -> dto.getContratoNumero() != null)
                    .filter(dto -> "VENCIDO".equals(dto.getContratoEstado()))
                    .map(ClienteConContratosDTO::getClienteNombre)
                    .distinct()
                    .collect(Collectors.toList());

            if (!clientesConSoloVencidos.isEmpty()) {
                log.warn("Clientes con SOLO contratos vencidos: {}", clientesConSoloVencidos);
            }

            clientesSinContrato.forEach(dto ->
                    log.debug("Cliente sin contrato: {} - Email: {}", dto.getClienteNombre(), dto.getClienteEmail())
            );

            return clientesSinContrato;

        } catch (Exception e) {
            log.error("Error al buscar clientes sin contratos activos", e);
            throw new RuntimeException("Error buscando clientes sin contratos: " + e.getMessage(), e);
        }
    }

    /**
     * Reporte: Detalle completo de contratos vigentes
     *
     * Lógica de negocio:
     * - Contiene info de cliente + instalación
     * - Solo contratos VIGENTES
     * - Calcula valor total de cartera activa
     * - Loguea alertas de precios anomalía
     *
     * @return Lista de contratos con detalles completos
     */
    @Transactional(readOnly = true)
    public List<ContratoDetalleDTO> reporteContratosConDetalleCompleto() {
        log.info("Generando reporte detallado de contratos vigentes");

        try {
            List<ContratoDetalleDTO> contratosDetalle = joinRepository.getContratosConClientesEInstalaciones();

            if (contratosDetalle.isEmpty()) {
                log.warn("No hay contratos con instalaciones registrados");
                return contratosDetalle;
            }

            // Lógica: Análisis de cartera
            var totalCartera = contratosDetalle.stream()
                    .mapToDouble(dto -> dto.getPrecioAnual().doubleValue())
                    .sum();

            var promedio = totalCartera / contratosDetalle.size();

            log.info("Análisis de cartera: Total={}, Contratos={}, Promedio={}",
                    totalCartera, contratosDetalle.size(), promedio);

            // Lógica: Detectar anomalías (precios muy altos o muy bajos)
            contratosDetalle.stream()
                    .filter(dto -> dto.getPrecioAnual().doubleValue() > promedio * 2)
                    .forEach(dto ->
                            log.warn("ALERTA: Contrato {} tiene precio muy alto: {} (vs promedio: {})",
                                    dto.getNumero(), dto.getPrecioAnual(), promedio)
                    );

            contratosDetalle.forEach(dto ->
                    log.debug("Contrato detallado: {} - Cliente: {} - Ubicación: {} - Precio: {}",
                            dto.getNumero(), dto.getClienteNombre(),
                            dto.getInstalacionUbicacion(), dto.getPrecioAnual())
            );

            return contratosDetalle;

        } catch (Exception e) {
            log.error("Error al generar reporte detallado", e);
            throw new RuntimeException("Error generando reporte detallado: " + e.getMessage(), e);
        }
    }

    /**
     * Reporte: Cartera de clientes empresariales
     *
     * Lógica de negocio:
     * - Identifica clientes con emails empresariales
     * - Solo contratos VIGENTES
     * - Ordena por valor (mayor primero)
     * - Calcula ingresos por segmento
     *
     * @return Lista de contratos de clientes empresariales
     */
    @Transactional(readOnly = true)
    public List<ContratoActivoClienteEmpresaDTO> reporteCarteraEmpresarial() {
        log.info("Generando reporte de cartera empresarial");

        try {
            List<ContratoActivoClienteEmpresaDTO> contratosEmpresa =
                    joinRepository.getContratosActivosConClientesEmpresa();

            if (contratosEmpresa.isEmpty()) {
                log.warn("No hay contratos empresariales activos");
                return contratosEmpresa;
            }

            // Lógica: Análisis de ingresos empresariales
            var ingresosTotales = contratosEmpresa.stream()
                    .mapToDouble(dto -> dto.getPrecioAnual().doubleValue())
                    .sum();

            var clientesUnicos = contratosEmpresa.stream()
                    .map(ContratoActivoClienteEmpresaDTO::getClienteNombre)
                    .distinct()
                    .count();

            log.info("Cartera empresarial: {} contratos de {} clientes - Ingresos totales: {}",
                    contratosEmpresa.size(), clientesUnicos, ingresosTotales);

            // Lógica: Detectar clientes empresariales TOP (mayor facturación)
            var top3 = contratosEmpresa.stream()
                    .sorted((a, b) -> b.getPrecioAnual().compareTo(a.getPrecioAnual()))
                    .limit(3)
                    .toList();

            log.info("TOP 3 clientes empresariales por facturación:");
            top3.forEach(dto ->
                    log.info("  -> {} ({}): {}", dto.getClienteNombre(), dto.getClienteEmail(), dto.getPrecioAnual())
            );

            return contratosEmpresa;

        } catch (Exception e) {
            log.error("Error al generar reporte de cartera empresarial", e);
            throw new RuntimeException("Error generando cartera empresarial: " + e.getMessage(), e);
        }
    }

    /**
     * Utilidad: Obtener contrato específico por ID con datos del cliente
     *
     * @param idContrato ID del contrato a buscar
     * @return DTO del contrato con datos del cliente, o null si no existe
     */
    @Transactional(readOnly = true)
    public ContratoClienteDTO obtenerContratoConCliente(Integer idContrato) {
        log.debug("Buscando contrato {} con datos de cliente", idContrato);

        try {
            if (idContrato == null || idContrato <= 0) {
                log.warn("ID de contrato inválido: {}", idContrato);
                return null;
            }

            List<ContratoClienteDTO> resultados = joinRepository.getContratosConClientes()
                    .stream()
                    .filter(dto -> dto.getId().equals(idContrato))
                    .toList();

            if (resultados.isEmpty()) {
                log.debug("No se encontró contrato con ID: {}", idContrato);
                return null;
            }

            ContratoClienteDTO resultado = resultados.get(0);
            log.debug("Contrato encontrado: {} - Cliente: {}", resultado.getNumero(), resultado.getClienteNombre());

            return resultado;

        } catch (Exception e) {
            log.error("Error buscando contrato específico", e);
            throw new RuntimeException("Error buscando contrato: " + e.getMessage(), e);
        }
    }

    /**
     * Utilidad: Contar contratos por estado
     *
     * @return Map con conteos por estado
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> estadisticasContratoPorEstado() {
        log.debug("Calculando estadísticas de contratos por estado");

        try {
            var estadisticas = joinRepository.getContratosConClientes().stream()
                    .collect(Collectors.groupingBy(
                            ContratoClienteDTO::getEstado,
                            Collectors.counting()
                    ));

            log.info("Estadísticas: {}", estadisticas);
            return estadisticas;

        } catch (Exception e) {
            log.error("Error calculando estadísticas", e);
            throw new RuntimeException("Error calculando estadísticas: " + e.getMessage(), e);
        }
    }
}