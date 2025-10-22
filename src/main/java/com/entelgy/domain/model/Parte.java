package com.entelgy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parte {
    private Integer id;                  // ← CAMBIO: Long → Integer
    private String numero;
    private Integer contratoId;          // ← CAMBIO: Long → Integer
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String descripcion;
    private String tipoTrabajo;
    private String estado;
    private BigDecimal horasTrabajadas;
    private Integer empresaId;
    private LocalDateTime fechaCreacion; // ← CAMBIO: String → LocalDateTime
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion; // ← CAMBIO: String → LocalDateTime
    private String usuarioModificacion;
}