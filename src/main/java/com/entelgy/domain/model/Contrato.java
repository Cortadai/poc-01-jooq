package com.entelgy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {
    private Long id;
    private String numero;
    private Long clienteId;
    private Long instalacionId;
    private String tipoContrato; // MANTENIMIENTO, REPARACION, REVISION
    private String estado; // VIGENTE, VENCIDO, CANCELADO, RENOVADO
    private Boolean coberturaMaterial;
    private Boolean coberturaManoObra;
    private Boolean coberturaFinSemana;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal precioAnual;
    private Integer porcentajeCentral;
    private Integer empresaId;
    private String fechaCreacion;
    private String usuarioCreacion;
    private String fechaModificacion;
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
