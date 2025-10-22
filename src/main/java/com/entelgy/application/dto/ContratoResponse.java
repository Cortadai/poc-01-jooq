package com.entelgy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ContratoResponse {
    private Long id;
    private String numero;
    private Long clienteId;
    private Long instalacionId;
    private String tipoContrato;
    private String estado;
    private Boolean coberturaMaterial;
    private Boolean coberturaManoObra;
    private Boolean coberturaFinSemana;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal precioAnual;
    private Integer porcentajeCentral;
    private Integer empresaId;
    private Boolean proximoAVencer;
    private Boolean vencido;
}