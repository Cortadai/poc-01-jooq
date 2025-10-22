package com.entelgy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para resultados de JOIN: Contratos + Clientes + Instalaciones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratoDetalleDTO {

    private Integer id;                      // contratos.id
    private String numero;                   // contratos.numero
    private String estado;                   // contratos.estado
    private BigDecimal precioAnual;          // contratos.precio_anual
    private String clienteNombre;            // clientes.nombre
    private String clienteTelefono;          // clientes.telefono
    private String instalacionUbicacion;     // instalaciones.ubicacion
    private String instalacionTipo;          // instalaciones.tipo
}