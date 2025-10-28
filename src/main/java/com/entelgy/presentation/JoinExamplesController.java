package com.entelgy.presentation;

import com.entelgy.application.JoinExamplesApplicationService;
import com.entelgy.application.dto.ClienteConContratosDTO;
import com.entelgy.application.dto.ContratoActivoClienteEmpresaDTO;
import com.entelgy.application.dto.ContratoClienteDTO;
import com.entelgy.application.dto.ContratoDetalleDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Reportes con JOINs", description = "Reportes avanzados y análisis de cartera usando JOINs complejos")
public class JoinExamplesController {

    private final JoinExamplesApplicationService joinExamplesService;

    @Operation(
            summary = "Obtener contratos activos con información del cliente",
            description = "Retorna todos los contratos VIGENTES con información del cliente. Incluye lógica de filtrado y análisis de cartera"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contratos activos obtenidos exitosamente")
    })
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

    @Operation(
            summary = "Obtener clientes sin contratos vigentes",
            description = "Retorna clientes que NO tienen contratos vigentes. Útil para identificar oportunidades de venta"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda de clientes sin contratos completada")
    })
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

    @Operation(
            summary = "Obtener contratos con detalle completo",
            description = """
                    Retorna contratos vigentes con detalles completos: información del cliente (nombre, teléfono), 
                    información de la instalación (ubicación, tipo) y precio anual. 
                    Incluye análisis de cartera y detección de anomalías
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte detallado de contratos obtenido con análisis de cartera")
    })
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

    @Operation(
            summary = "Obtener cartera empresarial",
            description = "Retorna contratos vigentes de clientes empresariales (emails que contienen 'empresa'). Incluye análisis TOP 3 por facturación"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cartera empresarial obtenida exitosamente con análisis de segmento")
    })
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

    @Operation(
            summary = "Obtener contrato específico con información del cliente",
            description = "Retorna un contrato específico con información del cliente asociado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contrato encontrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "ID de contrato inválido"),
            @ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    @GetMapping("/contrato/{id}")
    public ResponseEntity<?> getContratoEspecifico(
            @Parameter(description = "ID del contrato a buscar", example = "1", required = true)
            @PathVariable Integer id) {

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

    @Operation(
            summary = "Obtener estadísticas de contratos",
            description = "Retorna estadísticas de contratos agrupadas por estado (VIGENTE, VENCIDO, CANCELADO, etc)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente")
    })
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

    private Map<String, Object> crearRespuestaError(HttpStatus status, String error, String detalle) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("timestamp", LocalDateTime.now());
        respuesta.put("status", status.value());
        respuesta.put("error", error);
        respuesta.put("detalle", detalle);
        return respuesta;
    }
}
