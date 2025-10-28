package com.entelgy.presentation;

import com.entelgy.application.ContratoApplicationService;
import com.entelgy.application.dto.ContratoResponse;
import com.entelgy.application.dto.CrearContratoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Contratos", description = "API para gestión de contratos (CRUD)")
public class ContratoController {

    private final ContratoApplicationService contratoService;

    @Operation(
            summary = "Crear un nuevo contrato",
            description = "Crea un nuevo contrato en el sistema con los datos proporcionados"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Contrato creado exitosamente",
                    content = @Content(schema = @Schema(implementation = ContratoResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un contrato con ese número")
    })
    @PostMapping
    public ResponseEntity<ContratoResponse> crear(
            @Parameter(description = "Datos del contrato a crear", required = true)
            @Valid @RequestBody CrearContratoRequest request) {

        log.info("POST /api/contratos - Creando nuevo contrato: {}", request.getNumero());

        ContratoResponse response = contratoService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Obtener contrato por ID",
            description = "Retorna los detalles de un contrato específico"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contrato encontrado",
                    content = @Content(schema = @Schema(implementation = ContratoResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponse> obtenerPorId(
            @Parameter(description = "ID del contrato a buscar", example = "1", required = true)
            @PathVariable Long id) {

        log.debug("GET /api/contratos/{} - Obteniendo contrato", id);

        ContratoResponse response = contratoService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Listar contratos con filtros opcionales",
            description = "Retorna una lista de contratos. Si no se especifican filtros, retorna todos los contratos activos. Permite filtrar por clienteId, tipoContrato y estado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de contratos (puede estar vacía)")
    })
    @GetMapping
    public ResponseEntity<List<ContratoResponse>> listar(
            @Parameter(description = "ID del cliente para filtrar", example = "1")
            @RequestParam(required = false) Long clienteId,

            @Parameter(description = "Tipo de contrato para filtrar", example = "MANTENIMIENTO")
            @RequestParam(required = false) String tipoContrato,

            @Parameter(description = "Estado del contrato", example = "VIGENTE")
            @RequestParam(required = false) String estado) {

        log.debug("GET /api/contratos - Listando contratos con filtros: cliente={}, tipo={}, estado={}",
                clienteId, tipoContrato, estado);

        List<ContratoResponse> contratos;

        if (clienteId != null || tipoContrato != null || estado != null) {
            contratos = contratoService.buscar(clienteId, tipoContrato, estado);
        } else {
            contratos = contratoService.listarActivos();
        }

        return ResponseEntity.ok(contratos);
    }

    @Operation(
            summary = "Listar contratos de un cliente",
            description = "Retorna todos los contratos asociados a un cliente específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de contratos del cliente (puede estar vacía)"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ContratoResponse>> listarPorCliente(
            @Parameter(description = "ID del cliente", example = "1", required = true)
            @PathVariable Long clienteId) {

        log.debug("GET /api/contratos/cliente/{} - Listando contratos por cliente", clienteId);

        List<ContratoResponse> contratos = contratoService.listarPorCliente(clienteId);
        return ResponseEntity.ok(contratos);
    }

    @Operation(
            summary = "Obtener contratos próximos a vencer",
            description = "Retorna contratos que vencerán en los próximos 30 días"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de contratos próximos a vencer")
    })
    @GetMapping("/proximos-a-vencer")
    public ResponseEntity<List<ContratoResponse>> proximosAVencer() {
        log.debug("GET /api/contratos/proximos-a-vencer - Obteniendo contratos próximos a vencer");

        List<ContratoResponse> contratos = contratoService.obtenerProximosAVencer();
        return ResponseEntity.ok(contratos);
    }

    @Operation(
            summary = "Obtener contratos vencidos",
            description = "Retorna todos los contratos que ya han vencido"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de contratos vencidos")
    })
    @GetMapping("/vencidos")
    public ResponseEntity<List<ContratoResponse>> vencidos() {
        log.debug("GET /api/contratos/vencidos - Obteniendo contratos vencidos");

        List<ContratoResponse> contratos = contratoService.obtenerVencidos();
        return ResponseEntity.ok(contratos);
    }

    @Operation(
            summary = "Health check",
            description = "Verifica que el servicio de contratos está funcionando"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio funcionando correctamente")
    })
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("POC 1 - JOOQ is UP");
    }
}
