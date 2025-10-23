package com.entelgy.presentation;

import com.entelgy.infrastructure.repository.NativeQueriesRepository;
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
public class NativeQueriesController {

    private final NativeQueriesRepository repository;

    public NativeQueriesController(NativeQueriesRepository repository) {
        this.repository = repository;
    }

    // ============================================================================
    // 1. QUERIES SIMPLES
    // ============================================================================

    @GetMapping("/contratos/vigentes")
    public ResponseEntity<?> obtenerVigentes() {
        List<Map<String, Object>> resultado = repository.obtenerContratosVigentes();
        return ResponseEntity.ok(Map.of(
                "cantidad", resultado.size(),
                "datos", resultado
        ));
    }

    @GetMapping("/contratos/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        var resultado = repository.obtenerContratoPorId(id);
        return resultado.isPresent()
                ? ResponseEntity.ok(resultado.get())
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/contratos/contar/vigentes")
    public ResponseEntity<?> contarVigentes() {
        Long total = repository.contarContratosVigentes();
        return ResponseEntity.ok(Map.of("total_vigentes", total));
    }

    // ============================================================================
    // 2. QUERIES COMPLEJAS
    // ============================================================================

    @GetMapping("/analisis/cartera")
    public ResponseEntity<?> analisCartera() {
        List<Map<String, Object>> resultado = repository.analisCarteraCompleto();
        return ResponseEntity.ok(Map.of(
                "total_clientes", resultado.size(),
                "datos", resultado
        ));
    }

    @GetMapping("/top-clientes")
    public ResponseEntity<?> topClientes(@RequestParam(defaultValue = "5") Integer limit) {
        List<Map<String, Object>> resultado = repository.topClientesPorVolumen(limit);
        return ResponseEntity.ok(Map.of(
                "top", limit,
                "clientes", resultado
        ));
    }

    @GetMapping("/contratos/proximos-vencer")
    public ResponseEntity<?> proximosAVencer(@RequestParam(defaultValue = "30") Integer dias) {
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

    @PostMapping("/contratos/filtrar")
    public ResponseEntity<?> filtrar(
            @RequestParam String estado,
            @RequestParam BigDecimal precioMinimo,
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

    @GetMapping("/clientes/buscar")
    public ResponseEntity<?> buscar(@RequestParam String termino) {
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

    @GetMapping("/estadisticas/por-estado")
    public ResponseEntity<?> estadisticasPorEstado() {
        List<Map<String, Object>> resultado = repository.estadisticasConCTE();
        return ResponseEntity.ok(Map.of(
                "total_grupos", resultado.size(),
                "datos", resultado
        ));
    }

    @GetMapping("/contratos/con-ranking")
    public ResponseEntity<?> contratosConRanking() {
        List<Map<String, Object>> resultado = repository.contratosConRanking();
        return ResponseEntity.ok(Map.of(
                "total", resultado.size(),
                "datos", resultado
        ));
    }

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

    @GetMapping("/contratos/riesgo")
    public ResponseEntity<?> clasificacionRiesgo() {
        List<Map<String, Object>> resultado = repository.clasificacionRiesgo();
        return ResponseEntity.ok(Map.of(
                "total_analizado", resultado.size(),
                "datos", resultado
        ));
    }

    @GetMapping("/contratos/encima-promedio")
    public ResponseEntity<?> contratosEncimaProm() {
        List<Map<String, Object>> resultado = repository.contratosEncimaProm();
        return ResponseEntity.ok(Map.of(
                "cantidad", resultado.size(),
                "datos", resultado
        ));
    }

    @GetMapping("/clientes/multiples-contratos")
    public ResponseEntity<?> clientesMultiplesContratos(
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

    @GetMapping("/contratos/paginar")
    public ResponseEntity<?> paginar(
            @RequestParam(defaultValue = "1") Integer page,
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

    @GetMapping("/reporte/mensual")
    public ResponseEntity<?> reporteMensual() {
        List<Map<String, Object>> resultado = repository.reporteMensual();
        return ResponseEntity.ok(Map.of(
                "meses", resultado.size(),
                "datos", resultado
        ));
    }

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

    @PostMapping("/contratos/insertar")
    public ResponseEntity<?> insertar(
            @RequestParam Integer clienteId,
            @RequestParam String numero,
            @RequestParam String estado,
            @RequestParam BigDecimal precio) {
        int resultado = repository.insertarContrato(clienteId, numero, estado, precio);
        return ResponseEntity.ok(Map.of(
                "status", resultado > 0 ? "success" : "error",
                "registros_insertados", resultado
        ));
    }

    @PutMapping("/contratos/actualizar-estado")
    public ResponseEntity<?> actualizarEstado(
            @RequestParam String estadoActual,
            @RequestParam String estadoNuevo) {
        int resultado = repository.actualizarMultiplesContratos(estadoActual, estadoNuevo);
        return ResponseEntity.ok(Map.of(
                "status", resultado > 0 ? "success" : "error",
                "registros_actualizados", resultado
        ));
    }

    @DeleteMapping("/contratos/eliminar-antiguos")
    public ResponseEntity<?> eliminarAntiguos(@RequestParam LocalDate fechaLimite) {
        int resultado = repository.eliminarContratosAntiguos(fechaLimite);
        return ResponseEntity.ok(Map.of(
                "status", resultado > 0 ? "success" : "error",
                "registros_eliminados", resultado
        ));
    }

    // ============================================================================
    // 9. ERROR HANDLING
    // ============================================================================

    @PostMapping("/query-custom")
    public ResponseEntity<?> ejecutarQueryCustom(@RequestBody String sql) {
        List<Map<String, Object>> resultado = repository.obtenerConConErrorHandling(sql);
        return ResponseEntity.ok(Map.of(
                "registros", resultado.size(),
                "datos", resultado
        ));
    }

}