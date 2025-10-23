package com.entelgy.presentation;

import com.entelgy.application.ProcedureApplicationService;
import com.entelgy.application.dto.CarteraReporteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureApplicationService procedureService;

    @GetMapping("/actualizar-vencidos")
    public ResponseEntity<?> actualizarVencidos() {
        var resultado = procedureService.actualizarContratosvencidos();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/reporte-cartera")
    public ResponseEntity<List<CarteraReporteDTO>> obtenerReporte() {
        var reportes = procedureService.obtenerReporteCartera();
        return ResponseEntity.ok(reportes);
    }

    @PostMapping("/crear-parte")
    public ResponseEntity<?> crearParte(@RequestParam Integer contratoId,
                                        @RequestParam String descripcion,
                                        @RequestParam String tipoTrabajo) {
        var resultado = procedureService.crearParteTrabajo(contratoId, descripcion, tipoTrabajo);
        return ResponseEntity.ok(resultado);
    }
}