package com.entelgy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parte {
    private Long id;
    private String numeroParte;
    private Long contratoId;
    private Long clienteId;
    private Long instalacionId;
    private String tipoParte; // AVERIA, REVISION, INSTALACION, MANTENIMIENTO
    private String estado; // ABIERTO, CERRADO, CANCELADO
    private String descripcion;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private Long tecnicoId;
    private String fechaCreacion;
    private String usuarioCreacion;

    /**
     * Calcula duración en minutos
     */
    public Long duracionMinutos() {
        if (horaInicio != null && horaFin != null) {
            return java.time.temporal.ChronoUnit.MINUTES.between(horaInicio, horaFin);
        }
        return null;
    }
}