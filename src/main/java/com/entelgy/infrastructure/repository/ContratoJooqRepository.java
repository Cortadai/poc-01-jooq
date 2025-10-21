package com.entelgy.infrastructure.repository;

import com.entelgy.domain.model.Contrato;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * Repository JOOQ para Contratos
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ContratoJooqRepository {

    private final DSLContext dsl;
    private static final String TABLE_NAME = "contratos";
    private static final String SCHEMA = "dbo";

    // ============= LECTURAS (SELECT) =============

    /**
     * Obtiene contrato por ID
     */
    public Optional<Contrato> findById(Long id) {
        log.debug("Buscando contrato con ID: {}", id);

        Record record = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "id")).eq(id))
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
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "estado")).eq("VIGENTE"))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "numero")))
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
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "cliente_id")).eq(clienteId))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "fecha_inicio")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    /**
     * Busca contratos próximos a vencer (próximos 30 días)
     * Ejemplo de query compleja con múltiples condiciones
     */
    public List<Contrato> findProximosAVencer() {
        log.debug("Buscando contratos próximos a vencer");

        LocalDate hoy = LocalDate.now();
        LocalDate futuro = hoy.plusDays(30);

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(
                        field(name(SCHEMA, TABLE_NAME, "estado")).eq("VIGENTE")
                                .and(field(name(SCHEMA, TABLE_NAME, "fecha_fin")).greaterOrEqual(hoy))
                                .and(field(name(SCHEMA, TABLE_NAME, "fecha_fin")).lessOrEqual(futuro))
                )
                .orderBy(field(name(SCHEMA, TABLE_NAME, "fecha_fin")).asc())
                .fetch();

        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    /**
     * Busca contratos vencidos (fecha_fin < hoy)
     */
    public List<Contrato> findVencidos() {
        log.debug("Buscando contratos vencidos");

        LocalDate hoy = LocalDate.now();

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "fecha_fin")).lessThan(hoy))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "fecha_fin")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    /**
     * Búsqueda avanzada con múltiples filtros
     * Demuestra cómo construir WHERE dinámicos
     *
     * @param clienteId ID del cliente (opcional)
     * @param tipoContrato Tipo de contrato (opcional)
     * @param estado Estado del contrato (opcional)
     * @return Lista de contratos que coinciden con los filtros
     */
    public List<Contrato> findByFiltros(Long clienteId, String tipoContrato, String estado) {
        log.debug("Buscando contratos con filtros: clienteId={}, tipo={}, estado={}",
                clienteId, tipoContrato, estado);

        var condition = DSL.noCondition();

        if (clienteId != null) {
            condition = condition.and(field(name(SCHEMA, TABLE_NAME, "cliente_id")).eq(clienteId));
        }

        if (tipoContrato != null && !tipoContrato.isEmpty()) {
            condition = condition.and(field(name(SCHEMA, TABLE_NAME, "tipo_contrato")).eq(tipoContrato));
        }

        if (estado != null && !estado.isEmpty()) {
            condition = condition.and(field(name(SCHEMA, TABLE_NAME, "estado")).eq(estado));
        }

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(condition)
                .orderBy(field(name(SCHEMA, TABLE_NAME, "fecha_inicio")).desc())
                .fetch();

        return records.stream()
                .map(this::recordToContrato)
                .toList();
    }

    // ============= ESCRITURAS (INSERT, UPDATE) =============

    /**
     * Inserta nuevo contrato
     *
     * Nota: Los campos fecha_creacion y usuario_creacion se establecen
     * con valores por defecto en la BD (GETDATE() y 'SYSTEM')
     */
    public Contrato save(Contrato contrato) {
        log.debug("Guardando nuevo contrato: {}", contrato.getNumero());

        int result = dsl
                .insertInto(
                        table(name(SCHEMA, TABLE_NAME)),
                        field(name(SCHEMA, TABLE_NAME, "numero")),
                        field(name(SCHEMA, TABLE_NAME, "cliente_id")),
                        field(name(SCHEMA, TABLE_NAME, "instalacion_id")),
                        field(name(SCHEMA, TABLE_NAME, "tipo_contrato")),
                        field(name(SCHEMA, TABLE_NAME, "estado")),
                        field(name(SCHEMA, TABLE_NAME, "cobertura_material")),
                        field(name(SCHEMA, TABLE_NAME, "cobertura_mano_obra")),
                        field(name(SCHEMA, TABLE_NAME, "cobertura_fin_semana")),
                        field(name(SCHEMA, TABLE_NAME, "fecha_inicio")),
                        field(name(SCHEMA, TABLE_NAME, "fecha_fin")),
                        field(name(SCHEMA, TABLE_NAME, "precio_anual")),
                        field(name(SCHEMA, TABLE_NAME, "porcentaje_central")),
                        field(name(SCHEMA, TABLE_NAME, "empresa_id")),
                        field(name(SCHEMA, TABLE_NAME, "usuario_creacion"))
                )
                .values(
                        contrato.getNumero(),
                        contrato.getClienteId(),
                        contrato.getInstalacionId(),
                        contrato.getTipoContrato(),
                        contrato.getEstado() != null ? contrato.getEstado() : "VIGENTE",
                        contrato.getCoberturaMaterial() != null ? contrato.getCoberturaMaterial() : true,
                        contrato.getCoberturaManoObra() != null ? contrato.getCoberturaManoObra() : true,
                        contrato.getCoberturaFinSemana() != null ? contrato.getCoberturaFinSemana() : false,
                        contrato.getFechaInicio(),
                        contrato.getFechaFin(),
                        contrato.getPrecioAnual(),
                        contrato.getPorcentajeCentral() != null ? contrato.getPorcentajeCentral() : 70,
                        contrato.getEmpresaId() != null ? contrato.getEmpresaId() : 1,
                        "SYSTEM"
                )
                .execute();

        if (result > 0) {
            log.debug("Contrato guardado exitosamente");

            // Recuperar el contrato inserado por su número
            Record record = dsl
                    .select()
                    .from(table(name(SCHEMA, TABLE_NAME)))
                    .where(field(name(SCHEMA, TABLE_NAME, "numero")).eq(contrato.getNumero()))
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
     * Actualiza: estado, fecha_modificacion, usuario_modificacion
     */
    public Contrato update(Contrato contrato) {
        log.debug("Actualizando contrato: {}", contrato.getId());

        int result = dsl
                .update(table(name(SCHEMA, TABLE_NAME)))
                .set(field(name(SCHEMA, TABLE_NAME, "estado")), contrato.getEstado())
                .set(field(name(SCHEMA, TABLE_NAME, "fecha_modificacion")), now())
                .set(field(name(SCHEMA, TABLE_NAME, "usuario_modificacion")), "SYSTEM")
                .where(field(name(SCHEMA, TABLE_NAME, "id")).eq(contrato.getId()))
                .execute();

        if (result > 0) {
            log.debug("Contrato actualizado exitosamente");
        }

        return contrato;
    }

    // ============= UTILIDADES =============

    /**
     * Convierte un Record de JOOQ a objeto Contrato
     *
     * Esta es la "columna vertebral" del mapping manual.
     * Mapea cada columna de la BD a su correspondiente propiedad en la entidad.
     */
    private Contrato recordToContrato(Record record) {
        return Contrato.builder()
                .id(record.get("id", Long.class))
                .numero(record.get("numero", String.class))
                .clienteId(record.get("cliente_id", Long.class))
                .instalacionId(record.get("instalacion_id", Long.class))
                .tipoContrato(record.get("tipo_contrato", String.class))
                .estado(record.get("estado", String.class))
                .coberturaMaterial(record.get("cobertura_material", Boolean.class))
                .coberturaManoObra(record.get("cobertura_mano_obra", Boolean.class))
                .coberturaFinSemana(record.get("cobertura_fin_semana", Boolean.class))
                .fechaInicio(record.get("fecha_inicio", LocalDate.class))
                .fechaFin(record.get("fecha_fin", LocalDate.class))
                .precioAnual(record.get("precio_anual", java.math.BigDecimal.class))
                .porcentajeCentral(record.get("porcentaje_central", Integer.class))
                .empresaId(record.get("empresa_id", Integer.class))
                .fechaCreacion(record.get("fecha_creacion", String.class))
                .usuarioCreacion(record.get("usuario_creacion", String.class))
                .fechaModificacion(record.get("fecha_modificacion", String.class))
                .usuarioModificacion(record.get("usuario_modificacion", String.class))
                .build();
    }
}