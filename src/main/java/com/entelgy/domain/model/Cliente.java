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
public class Cliente {
    private Integer id;                  // ← CAMBIO: Long → Integer
    private String nombre;
    private String email;
    private String telefono;
    private Integer empresaId;
    private LocalDateTime fechaCreacion; // ← CAMBIO: String → LocalDateTime
    private String usuarioCreacion;
}