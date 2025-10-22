package com.entelgy.infrastructure.repository;

import com.entelgy.domain.model.Parte;
import com.entelgy.jooq.generated.tables.Partes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JOOQ para Partes - REFACTORIZADO CON CODE GENERATION
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ParteJooqRepository {

    private final DSLContext dsl;
    private static final Partes PARTES = Partes.PARTES;

    /**
     * Obtiene parte por ID
     */
    public Optional<Parte> findById(Long id) {
        log.debug("Buscando parte con ID: {}", id);

        Record record = dsl
                .select()
                .from(PARTES)
                .where(PARTES.ID.eq(Math.toIntExact(id)))
                .fetchOne();

        return record != null ? Optional.of(recordToPartes(record)) : Optional.empty();
    }

    /**
     * Busca partes por contrato
     */
    public List<Parte> findByContratoId(Long contratoId) {
        log.debug("Buscando partes para contrato: {}", contratoId);

        Result<Record> records = dsl
                .select()
                .from(PARTES)
                .where(PARTES.CONTRATO_ID.eq(Math.toIntExact(contratoId)))
                .orderBy(PARTES.FECHA_INICIO.desc())
                .fetch();

        return records.stream()
                .map(this::recordToPartes)
                .toList();
    }

    /**
     * Busca partes abiertos (sin cerrar)
     */
    public List<Parte> findAbiertos() {
        log.debug("Obteniendo partes abiertos");

        Result<Record> records = dsl
                .select()
                .from(PARTES)
                .where(PARTES.ESTADO.eq("ABIERTO"))
                .orderBy(PARTES.FECHA_INICIO.desc())
                .fetch();

        return records.stream()
                .map(this::recordToPartes)
                .toList();
    }

    /**
     * Guarda nuevo parte
     */
    public Parte save(Parte parte) {
        log.debug("Guardando nuevo parte: {}", parte.getNumero());

        int result = dsl
                .insertInto(PARTES)
                .set(PARTES.NUMERO, parte.getNumero())
                .set(PARTES.CONTRATO_ID, parte.getContratoId())
                .set(PARTES.FECHA_INICIO, parte.getFechaInicio())
                .set(PARTES.FECHA_FIN, parte.getFechaFin())
                .set(PARTES.DESCRIPCION, parte.getDescripcion())
                .set(PARTES.TIPO_TRABAJO, parte.getTipoTrabajo())
                .set(PARTES.ESTADO, parte.getEstado() != null ? parte.getEstado() : "ABIERTO")
                .set(PARTES.HORAS_TRABAJADAS, parte.getHorasTrabajadas())
                .set(PARTES.USUARIO_CREACION, "SYSTEM")
                .execute();

        if (result > 0) {
            log.debug("Parte guardado exitosamente");

            Record record = dsl
                    .selectFrom(PARTES)
                    .where(PARTES.NUMERO.eq(parte.getNumero()))
                    .fetchOne();

            if (record != null) {
                return recordToPartes(record);
            }
        }

        return parte;
    }

    /**
     * Actualiza parte
     */
    public Parte update(Parte parte) {
        log.debug("Actualizando parte: {}", parte.getId());

        dsl
                .update(PARTES)
                .set(PARTES.ESTADO, parte.getEstado())
                .set(PARTES.FECHA_FIN, parte.getFechaFin())
                .set(PARTES.USUARIO_MODIFICACION, "SYSTEM")
                .where(PARTES.ID.eq(parte.getId()))
                .execute();

        return parte;
    }

    /**
     * Convierte Record a Parte
     */
    private Parte recordToPartes(Record record) {
        return Parte.builder()
                .id(record.get(PARTES.ID))
                .numero(record.get(PARTES.NUMERO))
                .contratoId(record.get(PARTES.CONTRATO_ID))
                .fechaInicio(record.get(PARTES.FECHA_INICIO))
                .fechaFin(record.get(PARTES.FECHA_FIN))
                .descripcion(record.get(PARTES.DESCRIPCION))
                .tipoTrabajo(record.get(PARTES.TIPO_TRABAJO))
                .estado(record.get(PARTES.ESTADO))
                .horasTrabajadas(record.get(PARTES.HORAS_TRABAJADAS))
                .empresaId(record.get(PARTES.EMPRESA_ID))
                .fechaCreacion(record.get(PARTES.FECHA_CREACION))
                .usuarioCreacion(record.get(PARTES.USUARIO_CREACION))
                .fechaModificacion(record.get(PARTES.FECHA_MODIFICACION))
                .usuarioModificacion(record.get(PARTES.USUARIO_MODIFICACION))
                .build();
    }
}