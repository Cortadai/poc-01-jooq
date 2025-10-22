package com.entelgy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {
    private Integer id;                    // ← CAMBIO: Long → Integer
    private String numero;
    private Integer clienteId;            // ← CAMBIO: Long → Integer
    private Integer instalacionId;        // ← CAMBIO: Long → Integer
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
    private LocalDateTime fechaCreacion;  // ← CAMBIO: String → LocalDateTime
    private String usuarioCreacion;
    private LocalDateTime fechaModificacion; // ← CAMBIO: String → LocalDateTime
    private String usuarioModificacion;

    /**
     * Valida reglas de negocio básicas
     */
    public boolean esValido() {
        return numero != null && !numero.isEmpty()
                && clienteId != null && clienteId > 0
                && instalacionId != null && instalacionId > 0
                && fechaInicio != null && fechaFin != null
                && fechaInicio.isBefore(fechaFin)
                && precioAnual != null && precioAnual.signum() > 0;
    }

    /**
     * Valida si el contrato está próximo a vencer (próximos 30 días)
     */
    public boolean proximoAVencer() {
        LocalDate hoy = LocalDate.now();
        LocalDate limiteAlerta = hoy.plusDays(30);
        return !fechaFin.isBefore(hoy) && fechaFin.isBefore(limiteAlerta);
    }

    /**
     * Valida si el contrato está vencido
     */
    public boolean estaVencido() {
        return LocalDate.now().isAfter(fechaFin);
    }
}