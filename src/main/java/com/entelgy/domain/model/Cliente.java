package com.entelgy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {
    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private Integer empresaId;
    private Integer delegacionId;
    private String estado; // ACTIVO, INACTIVO
    private String fechaCreacion;
    private String usuarioCreacion;
    private String fechaModificacion;
    private String usuarioModificacion;
}
