package com.entelgy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para el resultado del procedure sp_generar_reporte_cartera()
 *
 * Mapea los resultados del procedure a objeto Java para fácil manipulación
 * Incluye: estadísticas por cliente, volúmenes, coberturas, vencimientos
 *
 * Estado cartera puede ser:
 * - SIN_CONTRATOS: Cliente sin contratos activos
 * - SIN_VIGENTES: No tiene contratos vigentes
 * - CRÍTICO: Próximo vencimiento < 30 días
 * - VENCIDO: Ya ha vencido
 * - ALERTA: Próximo vencimiento 30-90 días
 * - NORMAL: Próximo vencimiento > 90 días
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarteraReporteDTO {

    // ============= Identidad del Cliente =============
    private Integer clienteId;
    private String clienteNombre;

    // ============= Conteos por Estado =============
    private Long totalContratos;
    private Long contratosVigentes;
    private Long contratosVencidos;
    private Long contratosCancelados;
    private Long contratosSuspendidos;

    // ============= Volúmenes Económicos =============
    private BigDecimal volumenTotal;
    private BigDecimal volumenVigentes;
    private BigDecimal volumenVencidos;
    private BigDecimal precioPromedio;

    // ============= Información de Coberturas =============
    private BigDecimal porcentajeCoberturasMaterial;
    private BigDecimal porcentajeCoberturaManoObra;

    // ============= Análisis de Vencimientos =============
    private LocalDate fechaProximoVencimiento;
    private Integer diasParaVencer;

    // ============= Estado General de la Cartera =============
    private String estadoCartera;

    // ============= Métodos Helper =============

    /**
     * Indica si el cliente tiene cartera en riesgo (CRÍTICO o VENCIDO)
     */
    public boolean tieneRiesgo() {
        return "CRÍTICO".equals(estadoCartera) || "VENCIDO".equals(estadoCartera);
    }

    /**
     * Indica si el cliente está en situación de alerta
     */
    public boolean tieneAlerta() {
        return "ALERTA".equals(estadoCartera) || tieneRiesgo();
    }

    /**
     * Calcula el porcentaje de ocupación (contratos vigentes vs total)
     */
    public BigDecimal getPorcentajeOcupacion() {
        if (totalContratos == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(contratosVigentes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalContratos), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Retorna resumen textual del estado
     */
    public String getResumen() {
        return String.format(
                "%s: %d contratos (%d vigentes), Volumen: €%.2f, Estado: %s",
                clienteNombre,
                totalContratos,
                contratosVigentes,
                volumenVigentes,
                estadoCartera
        );
    }

    @Override
    public String toString() {
        return "CarteraReporteDTO{" +
                "clienteId=" + clienteId +
                ", clienteNombre='" + clienteNombre + '\'' +
                ", totalContratos=" + totalContratos +
                ", contratosVigentes=" + contratosVigentes +
                ", volumenVigentes=" + volumenVigentes +
                ", estadoCartera='" + estadoCartera + '\'' +
                ", diasParaVencer=" + diasParaVencer +
                '}';
    }
}