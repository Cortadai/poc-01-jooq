package com.entelgy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resultados de JOIN: Contratos + Clientes
 * Mapea automáticamente desde Record de JOOQ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratoClienteDTO {

    private Integer id;                    // contratos.id
    private String numero;                 // contratos.numero
    private String estado;                 // contratos.estado
    private String clienteNombre;          // clientes.nombre (aliased)
    private String clienteEmail;           // clientes.email (aliased)
}