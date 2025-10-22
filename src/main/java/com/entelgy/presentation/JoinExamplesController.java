package com.entelgy.presentation;

import com.entelgy.application.JoinExamplesApplicationService;
import com.entelgy.application.dto.ClienteConContratosDTO;
import com.entelgy.application.dto.ContratoActivoClienteEmpresaDTO;
import com.entelgy.application.dto.ContratoClienteDTO;
import com.entelgy.application.dto.ContratoDetalleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para reportes y análisis con JOINs
 *
 * Responsabilidades:
 * - Exponer endpoints REST
 * - Delegar a Service (sin lógica)
 * - Retornar respuestas HTTP
 *
 * Las excepciones son manejadas por GlobalExceptionHandler
 * (no necesita try-catch en el controller)
 */
@Slf4j
@RestController
@RequestMapping("/api/reportes/joins")
@RequiredArgsConstructor
public class JoinExamplesController {

    private final JoinExamplesApplicationService joinExamplesService;

    // ============= REPORTES GENERALES =============

    /**
     * GET /api/reportes/joins/contratos-activos
     *
     * Retorna todos los contratos VIGENTES con información del cliente
     * Incluye lógica de filtrado y análisis de cartera
     *
     * @return 200 OK con Lista de ContratoClienteDTO (puede estar vacía)
     */
    @GetMapping("/contratos-activos")
    public ResponseEntity<?> getContratosActivos() {
        log.debug("GET /api/reportes/joins/contratos-activos");

        List<ContratoClienteDTO> resultados = joinExamplesService.reporteContratosActivos();

        return ResponseEntity.ok(crearRespuestaExitosa(
                "Contratos activos obtenidos exitosamente",
                resultados.size(),
                resultados
        ));
    }

    /**
     * GET /api/reportes/joins/clientes-sin-contratos
     *
     * Retorna clientes que NO tienen contratos vigentes
     * Útil para identificar oportunidades de venta
     *
     * @return 200 OK con Lista de ClienteConContratosDTO (puede estar vacía)
     */
    @GetMapping("/clientes-sin-contratos")
    public ResponseEntity<?> getClientesSinContratos() {
        log.debug("GET /api/reportes/joins/clientes-sin-contratos");

        List<ClienteConContratosDTO> resultados =
                joinExamplesService.reporteClientesSinContratosActivos();

        Map<String, Object> respuesta = crearRespuestaExitosa(
                "Búsqueda de clientes sin contratos completada",
                resultados.size(),
                resultados
        );

        if (!resultados.isEmpty()) {
            respuesta.put("accion_recomendada", "Contactar clientes para renovación/nuevos servicios");
        } else {
            respuesta.put("info", "Todos los clientes tienen contratos vigentes");
        }

        return ResponseEntity.ok(respuesta);
    }

    /**
     * GET /api/reportes/joins/contratos-detalle
     *
     * Retorna contratos vigentes con detalles completos:
     * - Información del cliente (nombre, teléfono)
     * - Información de la instalación (ubicación, tipo)
     * - Precio anual
     *
     * Incluye análisis de cartera y detección de anomalías
     *
     * @return 200 OK con Lista de ContratoDetalleDTO con análisis
     */
    @GetMapping("/contratos-detalle")
    public ResponseEntity<?> getContratosDetalle() {
        log.debug("GET /api/reportes/joins/contratos-detalle");

        List<ContratoDetalleDTO> resultados =
                joinExamplesService.reporteContratosConDetalleCompleto();

        Map<String, Object> respuesta = crearRespuestaExitosa(
                "Reporte detallado de contratos obtenido",
                resultados.size(),
                resultados
        );

        if (!resultados.isEmpty()) {
            // Análisis: Calcular totales
            double totalCartera = resultados.stream()
                    .mapToDouble(dto -> dto.getPrecioAnual().doubleValue())
                    .sum();

            double promedioCartera = totalCartera / resultados.size();

            Map<String, Object> analisisCartera = new HashMap<>();
            analisisCartera.put("precioAnualTotal", totalCartera);
            analisisCartera.put("precioAnualPromedio", promedioCartera);
            analisisCartera.put("precioAnualMinimo", resultados.stream()
                    .mapToDouble(dto -> dto.getPrecioAnual().doubleValue())
                    .min().orElse(0));
            analisisCartera.put("precioAnualMaximo", resultados.stream()
                    .mapToDouble(dto -> dto.getPrecioAnual().doubleValue())
                    .max().orElse(0));

            respuesta.put("analisisCartera", analisisCartera);
        } else {
            respuesta.put("info", "No hay contratos con instalaciones registrados");
        }

        return ResponseEntity.ok(respuesta);
    }

    /**
     * GET /api/reportes/joins/cartera-empresarial
     *
     * Retorna contratos vigentes de clientes empresariales
     * (emails que contienen 'empresa')
     *
     * Incluye análisis TOP 3 por facturación
     *
     * @return 200 OK con Lista de ContratoActivoClienteEmpresaDTO
     */
    @GetMapping("/cartera-empresarial")
    public ResponseEntity<?> getCarteraEmpresarial() {
        log.debug("GET /api/reportes/joins/cartera-empresarial");

        List<ContratoActivoClienteEmpresaDTO> resultados =
                joinExamplesService.reporteCarteraEmpresarial();

        Map<String, Object> respuesta = crearRespuestaExitosa(
                "Cartera empresarial obtenida exitosamente",
                resultados.size(),
                resultados
        );

        if (!resultados.isEmpty()) {
            // Análisis: Ingresos por segmento
            double ingresosTotales = resultados.stream()
                    .mapToDouble(dto -> dto.getPrecioAnual().doubleValue())
                    .sum();

            long clientesUnicos = resultados.stream()
                    .map(ContratoActivoClienteEmpresaDTO::getClienteNombre)
                    .distinct()
                    .count();

            Map<String, Object> analisisSegmento = new HashMap<>();
            analisisSegmento.put("clientesUnicos", clientesUnicos);
            analisisSegmento.put("ingresosTotales", ingresosTotales);
            analisisSegmento.put("ingresoPromedioPorContrato", ingresosTotales / resultados.size());
            analisisSegmento.put("segmento", "EMPRESARIAL");

            respuesta.put("analisisSegmento", analisisSegmento);
        } else {
            respuesta.put("info", "No hay contratos empresariales activos");
        }

        return ResponseEntity.ok(respuesta);
    }

    // ============= BÚSQUEDAS ESPECÍFICAS =============

    /**
     * GET /api/reportes/joins/contrato/{id}
     *
     * Retorna un contrato específico con información del cliente
     *
     * @param id ID del contrato a buscar
     * @return 200 OK con ContratoClienteDTO
     *         400 BAD REQUEST si ID es inválido
     *         404 NOT FOUND si no existe el contrato
     */
    @GetMapping("/contrato/{id}")
    public ResponseEntity<?> getContratoEspecifico(@PathVariable Integer id) {
        log.debug("GET /api/reportes/joins/contrato/{}", id);

        if (id == null || id <= 0) {
            log.warn("ID de contrato inválido: {}", id);
            throw new IllegalArgumentException("ID de contrato debe ser mayor que 0");
        }

        ContratoClienteDTO resultado = joinExamplesService.obtenerContratoConCliente(id);

        if (resultado == null) {
            log.debug("Contrato no encontrado con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(crearRespuestaError(
                            HttpStatus.NOT_FOUND,
                            "Contrato no encontrado",
                            "No existe contrato con ID: " + id
                    ));
        }

        return ResponseEntity.ok(crearRespuestaExitosa(
                "Contrato encontrado exitosamente",
                1,
                resultado
        ));
    }

    // ============= ESTADÍSTICAS =============

    /**
     * GET /api/reportes/joins/estadisticas
     *
     * Retorna estadísticas de contratos:
     * - Conteo por estado (VIGENTE, VENCIDO, CANCELADO, etc)
     *
     * @return 200 OK con Map de estadísticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<?> getEstadisticas() {
        log.debug("GET /api/reportes/joins/estadisticas");

        Map<String, Long> estadisticas = joinExamplesService.estadisticasContratoPorEstado();

        long totalContratos = estadisticas.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        Map<String, Object> respuesta = crearRespuestaExitosa(
                "Estadísticas obtenidas exitosamente",
                estadisticas.size(),
                null
        );
        respuesta.put("totalContratos", totalContratos);
        respuesta.put("porEstado", estadisticas);

        return ResponseEntity.ok(respuesta);
    }

    // ============= UTILIDADES PRIVADAS PARA FORMATEAR RESPUESTAS =============

    /**
     * Crea respuesta exitosa estándar
     */
    private Map<String, Object> crearRespuestaExitosa(String mensaje, int cantidad, Object datos) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("timestamp", LocalDateTime.now());
        respuesta.put("status", HttpStatus.OK.value());
        respuesta.put("mensaje", mensaje);
        respuesta.put("cantidad", cantidad);
        if (datos != null) {
            respuesta.put("datos", datos);
        }
        return respuesta;
    }

    /**
     * Crea respuesta de error (para errores no controlados)
     */
    private Map<String, Object> crearRespuestaError(HttpStatus status, String error, String detalle) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("timestamp", LocalDateTime.now());
        respuesta.put("status", status.value());
        respuesta.put("error", error);
        respuesta.put("detalle", detalle);
        return respuesta;
    }
}