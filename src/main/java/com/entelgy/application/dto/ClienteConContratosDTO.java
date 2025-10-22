package com.entelgy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para LEFT JOIN: Clientes con sus Contratos
 * Un cliente puede tener múltiples registros si tiene múltiples contratos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteConContratosDTO {

    private Integer clienteId;               // clientes.id
    private String clienteNombre;            // clientes.nombre
    private String clienteEmail;             // clientes.email
    private String contratoNumero;           // contratos.numero (puede ser null)
    private String contratoEstado;           // contratos.estado (puede ser null)
}