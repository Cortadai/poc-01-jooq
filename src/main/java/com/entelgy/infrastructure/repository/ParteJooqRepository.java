package com.entelgy.infrastructure.repository;

import com.entelgy.domain.model.Parte;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * Repository JOOQ para Partes (intervenciones técnicas)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ParteJooqRepository {

    private final DSLContext dsl;
    private static final String TABLE_NAME = "partes";
    private static final String SCHEMA = "dbo";

    // ============= LECTURAS (SELECT) =============

    /**
     * Obtiene parte por ID
     */
    public Optional<Parte> findById(Long id) {
        log.debug("Buscando parte con ID: {}", id);

        Record record = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "id")).eq(id))
                .fetchOne();

        return record != null ? Optional.of(recordToParte(record)) : Optional.empty();
    }

    /**
     * Obtiene todos los partes abiertos
     */
    public List<Parte> findAllAbiertos() {
        log.debug("Obteniendo todos los partes abiertos");

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "estado")).eq("ABIERTO"))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "numero_parte")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToParte)
                .toList();
    }

    /**
     * Obtiene partes abiertos para un cliente
     */
    public List<Parte> findAbiertosByClienteId(Long clienteId) {
        log.debug("Buscando partes abiertos para cliente: {}", clienteId);

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "cliente_id")).eq(clienteId))
                .and(field(name(SCHEMA, TABLE_NAME, "estado")).eq("ABIERTO"))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "numero_parte")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToParte)
                .toList();
    }

    /**
     * Obtiene partes por contrato
     */
    public List<Parte> findByContratoId(Long contratoId) {
        log.debug("Buscando partes para contrato: {}", contratoId);

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "contrato_id")).eq(contratoId))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "fecha_creacion")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToParte)
                .toList();
    }

    /**
     * Obtiene partes asignados a un técnico
     */
    public List<Parte> findByTecnicoId(Long tecnicoId) {
        log.debug("Buscando partes para técnico: {}", tecnicoId);

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "tecnico_id")).eq(tecnicoId))
                .and(field(name(SCHEMA, TABLE_NAME, "estado")).eq("ABIERTO"))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "numero_parte")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToParte)
                .toList();
    }

    /**
     * Obtiene todos los partes abiertos de mantenimiento
     */
    public List<Parte> findAllMantenimientosAbiertos() {
        log.debug("Obteniendo todos los partes abiertos en mantenimiento");

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "estado")).eq("ABIERTO"))
                .and(field(name(SCHEMA, TABLE_NAME, "tipo_parte")).eq("MANTENIMIENTO"))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "numero_parte")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToParte)
                .toList();
    }

    /**
     * Obtiene partes cerrados
     */
    public List<Parte> findCerrados() {
        log.debug("Obteniendo todos los partes cerrados");

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "estado")).eq("CERRADO"))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "fecha_creacion")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToParte)
                .toList();
    }

    /**
     * Búsqueda avanzada con múltiples filtros
     */
    public List<Parte> findByFiltros(Long clienteId, String estado, String tipoParte, Long tecnicoId) {
        log.debug("Buscando partes con filtros: cliente={}, estado={}, tipo={}, tecnico={}",
                clienteId, estado, tipoParte, tecnicoId);

        var condition = DSL.noCondition();

        if (clienteId != null) {
            condition = condition.and(
                    field(name(SCHEMA, TABLE_NAME, "cliente_id")).eq(clienteId)
            );
        }

        if (estado != null && !estado.isEmpty()) {
            condition = condition.and(
                    field(name(SCHEMA, TABLE_NAME, "estado")).eq(estado)
            );
        }

        if (tipoParte != null && !tipoParte.isEmpty()) {
            condition = condition.and(
                    field(name(SCHEMA, TABLE_NAME, "tipo_parte")).eq(tipoParte)
            );
        }

        if (tecnicoId != null) {
            condition = condition.and(
                    field(name(SCHEMA, TABLE_NAME, "tecnico_id")).eq(tecnicoId)
            );
        }

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(condition)
                .orderBy(field(name(SCHEMA, TABLE_NAME, "fecha_creacion")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToParte)
                .toList();
    }

    // ============= ESCRITURAS (INSERT, UPDATE) =============

    /**
     * Inserta nuevo parte
     */
    public Parte save(Parte parte) {
        log.debug("Guardando nuevo parte: {}", parte.getNumeroParte());

        int result = dsl
                .insertInto(
                        table(name(SCHEMA, TABLE_NAME)),
                        field(name(SCHEMA, TABLE_NAME, "numero_parte")),
                        field(name(SCHEMA, TABLE_NAME, "contrato_id")),
                        field(name(SCHEMA, TABLE_NAME, "cliente_id")),
                        field(name(SCHEMA, TABLE_NAME, "instalacion_id")),
                        field(name(SCHEMA, TABLE_NAME, "tipo_parte")),
                        field(name(SCHEMA, TABLE_NAME, "estado")),
                        field(name(SCHEMA, TABLE_NAME, "descripcion")),
                        field(name(SCHEMA, TABLE_NAME, "hora_inicio")),
                        field(name(SCHEMA, TABLE_NAME, "tecnico_id")),
                        field(name(SCHEMA, TABLE_NAME, "usuario_creacion"))
                )
                .values(
                        parte.getNumeroParte(),
                        parte.getContratoId(),
                        parte.getClienteId(),
                        parte.getInstalacionId(),
                        parte.getTipoParte(),
                        parte.getEstado() != null ? parte.getEstado() : "ABIERTO",
                        parte.getDescripcion(),
                        parte.getHoraInicio() != null ? parte.getHoraInicio() : LocalDateTime.now(),
                        parte.getTecnicoId(),
                        "SYSTEM"
                )
                .execute();

        if (result > 0) {
            log.debug("Parte guardado exitosamente");

            // Recuperar el parte insertado por número
            Record record = dsl
                    .select()
                    .from(table(name(SCHEMA, TABLE_NAME)))
                    .where(field(name(SCHEMA, TABLE_NAME, "numero_parte")).eq(parte.getNumeroParte()))
                    .fetchOne();

            if (record != null) {
                return recordToParte(record);
            }
        }

        return parte;
    }

    /**
     * Actualiza un parte existente
     */
    public Parte update(Parte parte) {
        log.debug("Actualizando parte: {}", parte.getId());

        int result = dsl
                .update(table(name(SCHEMA, TABLE_NAME)))
                .set(field(name(SCHEMA, TABLE_NAME, "estado")), parte.getEstado())
                .set(field(name(SCHEMA, TABLE_NAME, "hora_fin")), parte.getHoraFin())
                .set(field(name(SCHEMA, TABLE_NAME, "tecnico_id")), parte.getTecnicoId())
                .where(field(name(SCHEMA, TABLE_NAME, "id")).eq(parte.getId()))
                .execute();

        if (result > 0) {
            log.debug("Parte actualizado exitosamente");
        }

        return parte;
    }

    // ============= UTILIDADES =============

    /**
     * Convierte un Record de JOOQ a objeto Parte
     */
    private Parte recordToParte(Record record) {
        return Parte.builder()
                .id(record.get("id", Long.class))
                .numeroParte(record.get("numero_parte", String.class))
                .contratoId(record.get("contrato_id", Long.class))
                .clienteId(record.get("cliente_id", Long.class))
                .instalacionId(record.get("instalacion_id", Long.class))
                .tipoParte(record.get("tipo_parte", String.class))
                .estado(record.get("estado", String.class))
                .descripcion(record.get("descripcion", String.class))
                .horaInicio(record.get("hora_inicio", LocalDateTime.class))
                .horaFin(record.get("hora_fin", LocalDateTime.class))
                .tecnicoId(record.get("tecnico_id", Long.class))
                .fechaCreacion(record.get("fecha_creacion", String.class))
                .usuarioCreacion(record.get("usuario_creacion", String.class))
                .build();
    }
}