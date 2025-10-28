package com.entelgy.presentation;

import com.entelgy.infrastructure.repository.NativeQueriesRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para probar Native Queries con jOOQ
 * Endpoints para diferentes tipos de queries
 */
@RestController
@RequestMapping("/api/native-queries")
@CrossOrigin(origins = "*")
@Tag(name = "Consultas Nativas", description = "API para consultas SQL nativas con jOOQ - queries avanzadas, CTEs, window functions y análisis")
public class NativeQueriesController {

    private final NativeQueriesRepository repository;

    public NativeQueriesController(NativeQueriesRepository repository) {
        this.repository = repository;
    }

    // ============================================================================
    // 1. QUERIES SIMPLES
    // ============================================================================

    @Operation(
            summary = "Obtener contratos vigentes",
            description = "Retorna todos los contratos con estado VIGENTE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de contratos vigentes")
    })
    @GetMapping("/contratos/vigentes")
    public ResponseEntity<?> obtenerVigentes() {
        List<Map<String, Object>> resultado = repository.obtenerContratosVigentes();
        return ResponseEntity.ok(Map.of(
                "cantidad", resultado.size(),
                "datos", resultado
        ));
    }

    @Operation(
            summary = "Obtener contrato por ID",
            description = "Retorna un contrato específico por su ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contrato encontrado"),
            @ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    @GetMapping("/contratos/{id}")
    public ResponseEntity<?> obtenerPorId(
            @Parameter(description = "ID del contrato", example = "1", required = true)
            @PathVariable Integer id) {
        var resultado = repository.obtenerContratoPorId(id);
        return resultado.isPresent()
                ? ResponseEntity.ok(resultado.get())
                : ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Contar contratos vigentes",
            description = "Retorna el número total de contratos vigentes"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de contratos vigentes")
    })
    @GetMapping("/contratos/contar/vigentes")
    public ResponseEntity<?> contarVigentes() {
        Long total = repository.contarContratosVigentes();
        return ResponseEntity.ok(Map.of("total_vigentes", total));
    }

    // ============================================================================
    // 2. QUERIES COMPLEJAS
    // ============================================================================

    @Operation(
            summary = "Análisis completo de cartera",
            description = "Retorna análisis completo de la cartera de contratos por cliente con totales y estadísticas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Análisis de cartera completado")
    })
    @GetMapping("/analisis/cartera")
    public ResponseEntity<?> analisCartera() {
        List<Map<String, Object>> resultado = repository.analisCarteraCompleto();
        return ResponseEntity.ok(Map.of(
                "total_clientes", resultado.size(),
                "datos", resultado
        ));
    }

    @Operation(
            summary = "Top clientes por volumen",
            description = "Retorna los clientes con mayor volumen de contratos (ordenados por facturación)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Top clientes obtenidos")
    })
    @GetMapping("/top-clientes")
    public ResponseEntity<?> topClientes(
            @Parameter(description = "Número de clientes a retornar", example = "5")
            @RequestParam(defaultValue = "5") Integer limit) {
        List<Map<String, Object>> resultado = repository.topClientesPorVolumen(limit);
        return ResponseEntity.ok(Map.of(
                "top", limit,
                "clientes", resultado
        ));
    }

    @Operation(
            summary = "Contratos próximos a vencer",
            description = "Retorna contratos que vencerán dentro de X días"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contratos próximos a vencer obtenidos")
    })
    @GetMapping("/contratos/proximos-vencer")
    public ResponseEntity<?> proximosAVencer(
            @Parameter(description = "Días hacia adelante", example = "30")
            @RequestParam(defaultValue = "30") Integer dias) {
        List<Map<String, Object>> resultado = repository.contratosProximosAVencer(dias);
        return ResponseEntity.ok(Map.of(
                "dias_adelante", dias,
                "cantidad", resultado.size(),
                "datos", resultado
        ));
    }

    // ============================================================================
    // 3. QUERIES CON FILTROS
    // ============================================================================

    @Operation(
            summary = "Filtrar contratos",
            description = "Filtra contratos por estado, precio mínimo y fecha desde"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contratos filtrados exitosamente")
    })
    @PostMapping("/contratos/filtrar")
    public ResponseEntity<?> filtrar(
            @Parameter(description = "Estado del contrato", example = "VIGENTE", required = true)
            @RequestParam String estado,

            @Parameter(description = "Precio mínimo", example = "1000.00", required = true)
            @RequestParam BigDecimal precioMinimo,

            @Parameter(description = "Fecha desde", example = "2024-01-01", required = true)
            @RequestParam LocalDate fechaDesde) {

        List<Map<String, Object>> resultado = repository.filtrarContratos(estado, precioMinimo, fechaDesde);
        return ResponseEntity.ok(Map.of(
                "filtros", Map.of(
                        "estado", estado,
                        "precio_minimo", precioMinimo,
                        "fecha_desde", fechaDesde
                ),
                "total", resultado.size(),
                "datos", resultado
        ));
    }

    @Operation(
            summary = "Buscar clientes",
            description = "Busca clientes por término de búsqueda (nombre o email)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos")
    })
    @GetMapping("/clientes/buscar")
    public ResponseEntity<?> buscar(
            @Parameter(description = "Término de búsqueda", example = "Juan", required = true)
            @RequestParam String termino) {
        List<Map<String, Object>> resultado = repository.buscarClientes(termino);
        return ResponseEntity.ok(Map.of(
                "termino", termino,
                "resultados", resultado.size(),
                "datos", resultado
        ));
    }

    // ============================================================================
    // 4. CTEs Y WINDOW FUNCTIONS
    // ============================================================================

    @Operation(
            summary = "Estadísticas por estado con CTE",
            description = "Retorna estadísticas de contratos agrupados por estado usando Common Table Expressions (CTE)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas calculadas con CTE")
    })
    @GetMapping("/estadisticas/por-estado")
    public ResponseEntity<?> estadisticasPorEstado() {
        List<Map<String, Object>> resultado = repository.estadisticasConCTE();
        return ResponseEntity.ok(Map.of(
                "total_grupos", resultado.size(),
                "datos", resultado
        ));
    }

    @Operation(
            summary = "Contratos con ranking",
            description = "Retorna contratos con ranking por precio usando Window Functions"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contratos con ranking calculado")
    })
    @GetMapping("/contratos/con-ranking")
    public ResponseEntity<?> contratosConRanking() {
        List<Map<String, Object>> resultado = repository.contratosConRanking();
        return ResponseEntity.ok(Map.of(
                "total", resultado.size(),
                "datos", resultado
        ));
    }

    @Operation(
            summary = "Contratos con total acumulado",
            description = "Retorna contratos con running total (total acumulado) usando Window Functions"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contratos con running total calculado")
    })
    @GetMapping("/contratos/running-total")
    public ResponseEntity<?> runningTotal() {
        List<Map<String, Object>> resultado = repository.contratosConRunningTotal();
        return ResponseEntity.ok(Map.of(
                "datos", resultado
        ));
    }

    // ============================================================================
    // 5. ANÁLISIS Y CLASIFICACIÓN
    // ============================================================================

    @Operation(
            summary = "Clasificación de riesgo de contratos",
            description = "Clasifica contratos en categorías de riesgo (ALTO, MEDIO, BAJO) según criterios de negocio"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clasificación de riesgo completada")
    })
    @GetMapping("/contratos/riesgo")
    public ResponseEntity<?> clasificacionRiesgo() {
        List<Map<String, Object>> resultado = repository.clasificacionRiesgo();
        return ResponseEntity.ok(Map.of(
                "total_analizado", resultado.size(),
                "datos", resultado
        ));
    }

    @Operation(
            summary = "Contratos por encima del promedio",
            description = "Retorna contratos cuyo precio está por encima del promedio general"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contratos por encima del promedio obtenidos")
    })
    @GetMapping("/contratos/encima-promedio")
    public ResponseEntity<?> contratosEncimaProm() {
        List<Map<String, Object>> resultado = repository.contratosEncimaProm();
        return ResponseEntity.ok(Map.of(
                "cantidad", resultado.size(),
                "datos", resultado
        ));
    }

    @Operation(
            summary = "Clientes con múltiples contratos",
            description = "Retorna clientes que tienen un número mínimo de contratos especificado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clientes con múltiples contratos obtenidos")
    })
    @GetMapping("/clientes/multiples-contratos")
    public ResponseEntity<?> clientesMultiplesContratos(
            @Parameter(description = "Número mínimo de contratos", example = "2")
            @RequestParam(defaultValue = "2") Integer minimo) {
        List<Map<String, Object>> resultado = repository.clientesConMultiplesContratos(minimo);
        return ResponseEntity.ok(Map.of(
                "minimo_requerido", minimo,
                "cantidad", resultado.size(),
                "datos", resultado
        ));
    }

    // ============================================================================
    // 6. PAGINACIÓN
    // ============================================================================

    @Operation(
            summary = "Paginar contratos",
            description = "Retorna contratos con paginación (útil para mostrar en tablas)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de contratos obtenida")
    })
    @GetMapping("/contratos/paginar")
    public ResponseEntity<?> paginar(
            @Parameter(description = "Número de página (comienza en 1)", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {

        List<Map<String, Object>> datos = repository.obtenerContratosPageable(page, size);
        Long total = repository.contarTotalContratos();
        Integer totalPages = (int) Math.ceil((double) total / size);

        return ResponseEntity.ok(Map.of(
                "page", page,
                "page_size", size,
                "total_elementos", total,
                "total_pages", totalPages,
                "datos", datos
        ));
    }

    // ============================================================================
    // 7. REPORTING
    // ============================================================================

    @Operation(
            summary = "Reporte mensual",
            description = "Genera reporte de contratos agrupados por mes"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte mensual generado")
    })
    @GetMapping("/reporte/mensual")
    public ResponseEntity<?> reporteMensual() {
        List<Map<String, Object>> resultado = repository.reporteMensual();
        return ResponseEntity.ok(Map.of(
                "meses", resultado.size(),
                "datos", resultado
        ));
    }

    @Operation(
            summary = "Métricas principales del dashboard",
            description = "Retorna las métricas principales para mostrar en el dashboard (KPIs)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas principales obtenidas")
    })
    @GetMapping("/dashboard/metricas")
    public ResponseEntity<?> metricasPrincipales() {
        Map<String, Object> resultado = repository.obtenerMetricasPrincipales();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "metricas", resultado
        ));
    }

    // ============================================================================
    // 8. OPERACIONES (INSERT/UPDATE/DELETE)
    // ============================================================================

    @Operation(
            summary = "Insertar nuevo contrato",
            description = "Inserta un nuevo contrato en la base de datos"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contrato insertado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping("/contratos/insertar")
    public ResponseEntity<?> insertar(
            @Parameter(description = "ID del cliente", example = "1", required = true)
            @RequestParam Integer clienteId,

            @Parameter(description = "Número del contrato", example = "CONT-001", required = true)
            @RequestParam String numero,

            @Parameter(description = "Estado del contrato", example = "VIGENTE", required = true)
            @RequestParam String estado,

            @Parameter(description = "Precio del contrato", example = "5000.00", required = true)
            @RequestParam BigDecimal precio) {

        int resultado = repository.insertarContrato(clienteId, numero, estado, precio);
        return ResponseEntity.ok(Map.of(
                "status", resultado > 0 ? "success" : "error",
                "registros_insertados", resultado
        ));
    }

    @Operation(
            summary = "Actualizar estado de contratos",
            description = "Actualiza el estado de múltiples contratos (de un estado a otro)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contratos actualizados exitosamente")
    })
    @PutMapping("/contratos/actualizar-estado")
    public ResponseEntity<?> actualizarEstado(
            @Parameter(description = "Estado actual", example = "VIGENTE", required = true)
            @RequestParam String estadoActual,

            @Parameter(description = "Estado nuevo", example = "VENCIDO", required = true)
            @RequestParam String estadoNuevo) {

        int resultado = repository.actualizarMultiplesContratos(estadoActual, estadoNuevo);
        return ResponseEntity.ok(Map.of(
                "status", resultado > 0 ? "success" : "error",
                "registros_actualizados", resultado
        ));
    }

    @Operation(
            summary = "Eliminar contratos antiguos",
            description = "Elimina contratos cuya fecha de inicio es anterior a la fecha límite especificada"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contratos eliminados exitosamente")
    })
    @DeleteMapping("/contratos/eliminar-antiguos")
    public ResponseEntity<?> eliminarAntiguos(
            @Parameter(description = "Fecha límite", example = "2020-01-01", required = true)
            @RequestParam LocalDate fechaLimite) {

        int resultado = repository.eliminarContratosAntiguos(fechaLimite);
        return ResponseEntity.ok(Map.of(
                "status", resultado > 0 ? "success" : "error",
                "registros_eliminados", resultado
        ));
    }

    // ============================================================================
    // 9. ERROR HANDLING
    // ============================================================================

    @Operation(
            summary = "Ejecutar query SQL personalizado",
            description = "Ejecuta una query SQL personalizada con manejo de errores (SOLO PARA DESARROLLO/TESTING)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Query ejecutado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en la query SQL")
    })
    @PostMapping("/query-custom")
    public ResponseEntity<?> ejecutarQueryCustom(
            @Parameter(description = "Query SQL a ejecutar", required = true)
            @RequestBody String sql) {

        List<Map<String, Object>> resultado = repository.obtenerConConErrorHandling(sql);
        return ResponseEntity.ok(Map.of(
                "registros", resultado.size(),
                "datos", resultado
        ));
    }
}
