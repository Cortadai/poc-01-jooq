package com.entelgy.infrastructure.repository;

import com.entelgy.domain.model.Contrato;
import com.entelgy.infrastructure.exception.DuplicateEntryException;
import com.entelgy.jooq.generated.tables.Contratos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JOOQ para Contratos - REFACTORIZADO CON CODE GENERATION
 *
 * Responsabilidades:
 * - Acceso a datos mediante JOOQ
 * - Mapeo de Records a Contratos
 * - Validación de duplicados en INSERT/UPDATE
 * - Manejo de excepciones de integridad
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ContratoJooqRepository {

    private final DSLContext dsl;
    private static final Contratos CONTRATOS = Contratos.CONTRATOS;

    // ============= LECTURAS (SELECT) =============

    /**
     * Obtiene contrato por ID
     */
    public Optional<Contrato> findById(Long id) {
        log.debug("Buscando contrato con ID: {}", id);
        Record record = dsl
                .select()
                .from(CONTRATOS)
                .where(CONTRATOS.ID.eq(Math.toIntExact(id)))
                .fetchOne();
        return record != null ? Optional.of(recordToContrato(record)) : Optional.empty();
    }

    /**
     * Obtiene todos los contratos activos (VIGENTE)
     */
    public List<Contrato> findAllActivos() {
        log.debug("Obteniendo todos los contratos activos");
        Result<Record> records = dsl
                .select()
                .from(CONTRATOS)
                .where(CONTRATOS.ESTADO.eq("VIGENTE"))
                .orderBy(CONTRATOS.NUMERO)
                .fetch();
        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    /**
     * Busca contratos por cliente
     */
    public List<Contrato> findByClienteId(Long clienteId) {
        log.debug("Buscando contratos para cliente: {}", clienteId);
        Result<Record> records = dsl
                .select()
                .from(CONTRATOS)
                .where(CONTRATOS.CLIENTE_ID.eq(Math.toIntExact(clienteId)))
                .orderBy(CONTRATOS.FECHA_INICIO.desc())
                .fetch();
        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    /**
     * Busca contratos próximos a vencer (próximos 30 días)
     */
    public List<Contrato> findProximosAVencer() {
        log.debug("Buscando contratos próximos a vencer");
        LocalDate hoy = LocalDate.now();
        LocalDate futuro = hoy.plusDays(30);
        Result<Record> records = dsl
                .select()
                .from(CONTRATOS)
                .where(
                        CONTRATOS.ESTADO.eq("VIGENTE")
                                .and(CONTRATOS.FECHA_FIN.greaterOrEqual(hoy))
                                .and(CONTRATOS.FECHA_FIN.lessOrEqual(futuro))
                )
                .orderBy(CONTRATOS.FECHA_FIN.asc())
                .fetch();
        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    /**
     * Busca contratos vencidos
     */
    public List<Contrato> findVencidos() {
        log.debug("Buscando contratos vencidos");
        LocalDate hoy = LocalDate.now();
        Result<Record> records = dsl
                .select()
                .from(CONTRATOS)
                .where(CONTRATOS.FECHA_FIN.lessThan(hoy))
                .orderBy(CONTRATOS.FECHA_FIN.desc())
                .fetch();
        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    /**
     * Búsqueda avanzada con múltiples filtros
     */
    public List<Contrato> findByFiltros(Long clienteId, String tipoContrato, String estado) {
        log.debug("Buscando contratos con filtros: clienteId={}, tipo={}, estado={}",
                clienteId, tipoContrato, estado);
        var condition = DSL.noCondition();
        if (clienteId != null) {
            condition = condition.and(CONTRATOS.CLIENTE_ID.eq(Math.toIntExact(clienteId)));
        }
        if (tipoContrato != null && !tipoContrato.isEmpty()) {
            condition = condition.and(CONTRATOS.TIPO_CONTRATO.eq(tipoContrato));
        }
        if (estado != null && !estado.isEmpty()) {
            condition = condition.and(CONTRATOS.ESTADO.eq(estado));
        }
        Result<Record> records = dsl
                .select()
                .from(CONTRATOS)
                .where(condition)
                .orderBy(CONTRATOS.FECHA_INICIO.desc())
                .fetch();
        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    // ============= ESCRITURAS (INSERT, UPDATE) =============

    /**
     * Inserta nuevo contrato
     *
     * Valida que no exista ya un contrato con el mismo número
     *
     * @param contrato Contrato a guardar
     * @return Contrato guardado con ID generado
     * @throws DuplicateEntryException si ya existe un contrato con ese número
     */
    public Contrato save(Contrato contrato) {
        log.debug("Guardando nuevo contrato: {}", contrato.getNumero());

        // VALIDACIÓN: Verificar si ya existe contrato con ese número
        long countExistentes = dsl
                .selectCount()
                .from(CONTRATOS)
                .where(CONTRATOS.NUMERO.eq(contrato.getNumero()))
                .fetchOne(0, Long.class);

        if (countExistentes > 0) {
            log.warn("Intento de crear contrato duplicado con número: {}", contrato.getNumero());
            throw new DuplicateEntryException(
                    "Ya existe un contrato con número: " + contrato.getNumero()
            );
        }

        // INSERT
        int result = dsl
                .insertInto(CONTRATOS)
                .set(CONTRATOS.NUMERO, contrato.getNumero())
                .set(CONTRATOS.CLIENTE_ID, contrato.getClienteId())
                .set(CONTRATOS.INSTALACION_ID, contrato.getInstalacionId())
                .set(CONTRATOS.TIPO_CONTRATO, contrato.getTipoContrato())
                .set(CONTRATOS.ESTADO, contrato.getEstado() != null ? contrato.getEstado() : "VIGENTE")
                .set(CONTRATOS.COBERTURA_MATERIAL, contrato.getCoberturaMaterial() != null ? contrato.getCoberturaMaterial() : true)
                .set(CONTRATOS.COBERTURA_MANO_OBRA, contrato.getCoberturaManoObra() != null ? contrato.getCoberturaManoObra() : true)
                .set(CONTRATOS.COBERTURA_FIN_SEMANA, contrato.getCoberturaFinSemana() != null ? contrato.getCoberturaFinSemana() : false)
                .set(CONTRATOS.FECHA_INICIO, contrato.getFechaInicio())
                .set(CONTRATOS.FECHA_FIN, contrato.getFechaFin())
                .set(CONTRATOS.PRECIO_ANUAL, contrato.getPrecioAnual())
                .set(CONTRATOS.PORCENTAJE_CENTRAL, contrato.getPorcentajeCentral() != null ? contrato.getPorcentajeCentral() : 70)
                .set(CONTRATOS.EMPRESA_ID, contrato.getEmpresaId() != null ? contrato.getEmpresaId() : 1)
                .set(CONTRATOS.USUARIO_CREACION, "SYSTEM")
                .execute();

        if (result > 0) {
            log.debug("Contrato guardado exitosamente");

            // Recuperar contrato insertado (con ID generado)
            Record record = dsl
                    .select()
                    .from(CONTRATOS)
                    .where(CONTRATOS.NUMERO.eq(contrato.getNumero()))
                    .fetchOne();

            if (record != null) {
                return recordToContrato(record);
            }
        }

        return contrato;
    }

    /**
     * Actualiza un contrato existente
     *
     * @param contrato Contrato con datos actualizados
     * @return Contrato actualizado
     */
    public Contrato update(Contrato contrato) {
        log.debug("Actualizando contrato: {}", contrato.getId());

        int result = dsl
                .update(CONTRATOS)
                .set(CONTRATOS.ESTADO, contrato.getEstado())
                .set(CONTRATOS.FECHA_MODIFICACION, LocalDateTime.now())
                .set(CONTRATOS.USUARIO_MODIFICACION, "SYSTEM")
                .where(CONTRATOS.ID.eq(contrato.getId()))
                .execute();

        if (result > 0) {
            log.debug("Contrato actualizado exitosamente");
        } else {
            log.warn("No se actualizó contrato con ID: {}", contrato.getId());
        }

        return contrato;
    }

    // ============= UTILIDADES =============

    /**
     * Convierte un Record de JOOQ a objeto Contrato
     */
    private Contrato recordToContrato(Record record) {
        return Contrato.builder()
                .id(record.get(CONTRATOS.ID))
                .numero(record.get(CONTRATOS.NUMERO))
                .clienteId(record.get(CONTRATOS.CLIENTE_ID))
                .instalacionId(record.get(CONTRATOS.INSTALACION_ID))
                .tipoContrato(record.get(CONTRATOS.TIPO_CONTRATO))
                .estado(record.get(CONTRATOS.ESTADO))
                .coberturaMaterial(record.get(CONTRATOS.COBERTURA_MATERIAL))
                .coberturaManoObra(record.get(CONTRATOS.COBERTURA_MANO_OBRA))
                .coberturaFinSemana(record.get(CONTRATOS.COBERTURA_FIN_SEMANA))
                .fechaInicio(record.get(CONTRATOS.FECHA_INICIO))
                .fechaFin(record.get(CONTRATOS.FECHA_FIN))
                .precioAnual(record.get(CONTRATOS.PRECIO_ANUAL))
                .porcentajeCentral(record.get(CONTRATOS.PORCENTAJE_CENTRAL))
                .empresaId(record.get(CONTRATOS.EMPRESA_ID))
                .fechaCreacion(record.get(CONTRATOS.FECHA_CREACION))
                .usuarioCreacion(record.get(CONTRATOS.USUARIO_CREACION))
                .fechaModificacion(record.get(CONTRATOS.FECHA_MODIFICACION))
                .usuarioModificacion(record.get(CONTRATOS.USUARIO_MODIFICACION))
                .build();
    }
}