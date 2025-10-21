package com.entelgy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instalacion {
    private Long id;
    private Long clienteId;
    private String codigo;
    private String descripcion;
    private String direccion;
    private String codigoPostal;
    private String zona;
    private String estado;
    private String fechaCreacion;
    private String usuarioCreacion;
}