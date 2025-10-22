package com.entelgy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para contratos activos con clientes empresariales
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratoActivoClienteEmpresaDTO {

    private String numeroContrato;           // contratos.numero
    private BigDecimal precioAnual;          // contratos.precio_anual
    private String clienteNombre;            // clientes.nombre
    private String clienteEmail;             // clientes.email
}