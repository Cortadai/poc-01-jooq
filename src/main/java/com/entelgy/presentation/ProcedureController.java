package com.entelgy.presentation;

import com.entelgy.application.ProcedureApplicationService;
import com.entelgy.application.dto.CarteraReporteDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para llamar a procedimientos almacenados de PostgreSQL
 */
@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
@Tag(name = "Procedimientos Almacenados", description = "API para ejecutar procedimientos almacenados de PostgreSQL con jOOQ")
public class ProcedureController {

    private final ProcedureApplicationService procedureService;

    @Operation(
            summary = "Actualizar contratos vencidos",
            description = "Ejecuta el procedimiento almacenado que actualiza el estado de los contratos vencidos a 'VENCIDO'"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Procedimiento ejecutado exitosamente con resultados")
    })
    @GetMapping("/actualizar-vencidos")
    public ResponseEntity<?> actualizarVencidos() {
        var resultado = procedureService.actualizarContratosvencidos();
        return ResponseEntity.ok(resultado);
    }

    @Operation(
            summary = "Obtener reporte de cartera",
            description = "Ejecuta el procedimiento almacenado que genera un reporte completo de la cartera de contratos"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte de cartera generado exitosamente")
    })
    @GetMapping("/reporte-cartera")
    public ResponseEntity<List<CarteraReporteDTO>> obtenerReporte() {
        var reportes = procedureService.obtenerReporteCartera();
        return ResponseEntity.ok(reportes);
    }

    @Operation(
            summary = "Crear parte de trabajo",
            description = "Ejecuta el procedimiento almacenado para crear un nuevo parte de trabajo asociado a un contrato"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parte de trabajo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "404", description = "Contrato no encontrado")
    })
    @PostMapping("/crear-parte")
    public ResponseEntity<?> crearParte(
            @Parameter(description = "ID del contrato", example = "1", required = true)
            @RequestParam Integer contratoId,

            @Parameter(description = "Descripción del parte de trabajo", example = "Reparación urgente", required = true)
            @RequestParam String descripcion,

            @Parameter(description = "Tipo de trabajo", example = "MANTENIMIENTO", required = true)
            @RequestParam String tipoTrabajo) {

        var resultado = procedureService.crearParteTrabajo(contratoId, descripcion, tipoTrabajo);
        return ResponseEntity.ok(resultado);
    }
}
