package com.entelgy.presentation;

import com.entelgy.application.ContratoApplicationService;
import com.entelgy.application.dto.ContratoResponse;
import com.entelgy.application.dto.CrearContratoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller para Contratos
 *
 * Responsabilidades:
 * - Mapear HTTP a métodos
 * - Validar formato (JSR-303)
 * - Llamar a application service
 * - Devolver respuestas HTTP
 *
 * Nota: Las excepciones son capturadas por GlobalExceptionHandler
 */
@Slf4j
@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoApplicationService contratoService;

    /**
     * POST /api/contratos
     * Crea un nuevo contrato
     */
    @PostMapping
    public ResponseEntity<ContratoResponse> crear(
            @Valid @RequestBody CrearContratoRequest request) {

        log.info("POST /api/contratos - Creando nuevo contrato: {}", request.getNumero());

        ContratoResponse response = contratoService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * GET /api/contratos/{id}
     * Obtiene contrato por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponse> obtenerPorId(@PathVariable Long id) {
        log.debug("GET /api/contratos/{} - Obteniendo contrato", id);

        ContratoResponse response = contratoService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/contratos
     * Lista todos los contratos activos
     * Query params opcionales: clienteId, tipoContrato, estado
     *
     * Ejemplos:
     * - GET /api/contratos
     * - GET /api/contratos?clienteId=1
     * - GET /api/contratos?clienteId=1&estado=VIGENTE
     */
    @GetMapping
    public ResponseEntity<List<ContratoResponse>> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String tipoContrato,
            @RequestParam(required = false) String estado) {

        log.debug("GET /api/contratos - Listando contratos con filtros: cliente={}, tipo={}, estado={}",
                clienteId, tipoContrato, estado);

        List<ContratoResponse> contratos;

        // Si hay filtros, hacer búsqueda avanzada
        if (clienteId != null || tipoContrato != null || estado != null) {
            contratos = contratoService.buscar(clienteId, tipoContrato, estado);
        } else {
            contratos = contratoService.listarActivos();
        }

        return ResponseEntity.ok(contratos);
    }

    /**
     * GET /api/contratos/cliente/{clienteId}
     * Lista contratos de un cliente específico
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ContratoResponse>> listarPorCliente(
            @PathVariable Long clienteId) {

        log.debug("GET /api/contratos/cliente/{} - Listando contratos por cliente", clienteId);

        List<ContratoResponse> contratos = contratoService.listarPorCliente(clienteId);
        return ResponseEntity.ok(contratos);
    }

    /**
     * GET /api/contratos/proximos-a-vencer
     * Obtiene contratos próximos a vencer (próximos 30 días)
     */
    @GetMapping("/proximos-a-vencer")
    public ResponseEntity<List<ContratoResponse>> proximosAVencer() {
        log.debug("GET /api/contratos/proximos-a-vencer - Obteniendo contratos próximos a vencer");

        List<ContratoResponse> contratos = contratoService.obtenerProximosAVencer();
        return ResponseEntity.ok(contratos);
    }

    /**
     * GET /api/contratos/vencidos
     * Obtiene contratos vencidos
     */
    @GetMapping("/vencidos")
    public ResponseEntity<List<ContratoResponse>> vencidos() {
        log.debug("GET /api/contratos/vencidos - Obteniendo contratos vencidos");

        List<ContratoResponse> contratos = contratoService.obtenerVencidos();
        return ResponseEntity.ok(contratos);
    }

    /**
     * GET /api/contratos/health
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("POC 1 - JOOQ is UP");
    }
}