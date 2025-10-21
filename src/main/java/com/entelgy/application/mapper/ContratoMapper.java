package com.entelgy.application.mapper;

import com.entelgy.application.dto.ContratoResponse;
import com.entelgy.application.dto.CrearContratoRequest;
import com.entelgy.domain.model.Contrato;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContratoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", constant = "VIGENTE")
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "usuarioCreacion", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    @Mapping(target = "usuarioModificacion", ignore = true)
    @Mapping(target = "empresaId", constant = "1")
    Contrato toDomain(CrearContratoRequest request);

    @Mapping(target = "proximoAVencer", expression = "java(contrato.proximoAVencer())")
    @Mapping(target = "vencido", expression = "java(contrato.estaVencido())")
    ContratoResponse toResponse(Contrato contrato);
}