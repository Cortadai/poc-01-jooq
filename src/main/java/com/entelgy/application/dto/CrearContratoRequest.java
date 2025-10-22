package com.entelgy.application.dto;

import jakarta.validation.constraints.*;
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
public class CrearContratoRequest {

    @NotBlank(message = "El número de contrato es obligatorio")
    private String numero;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser positivo")
    private Long clienteId;

    @NotNull(message = "El ID de la instalación es obligatorio")
    @Positive(message = "El ID de la instalación debe ser positivo")
    private Long instalacionId;

    @NotBlank(message = "El tipo de contrato es obligatorio")
    private String tipoContrato; // MANTENIMIENTO, REPARACION, REVISION

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    @NotNull(message = "El precio anual es obligatorio")
    @Positive(message = "El precio anual debe ser positivo")
    private BigDecimal precioAnual;

    @Min(value = 0, message = "El porcentaje central no puede ser menor a 0")
    @Max(value = 100, message = "El porcentaje central no puede ser mayor a 100")
    private Integer porcentajeCentral = 70;

    private Boolean coberturaMaterial = true;
    private Boolean coberturaManoObra = true;
    private Boolean coberturaFinSemana = false;
}
