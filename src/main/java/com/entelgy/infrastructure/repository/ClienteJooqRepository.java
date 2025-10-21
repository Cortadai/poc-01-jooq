package com.entelgy.infrastructure.repository;

import com.entelgy.domain.model.Cliente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * Repository JOOQ para Clientes
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ClienteJooqRepository {

    private final DSLContext dsl;
    private static final String TABLE_NAME = "clientes";
    private static final String SCHEMA = "dbo";

    // ============= LECTURAS (SELECT) =============

    /**
     * Obtiene cliente por ID
     */
    public Optional<Cliente> findById(Long id) {
        log.debug("Buscando cliente con ID: {}", id);

        Record record = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "id")).eq(id))
                .fetchOne();

        return record != null ? Optional.of(recordToCliente(record)) : Optional.empty();
    }

    /**
     * Obtiene todos los clientes activos
     */
    public List<Cliente> findAllActivos() {
        log.debug("Obteniendo todos los clientes activos");

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "estado")).eq("ACTIVO"))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "nombre")).asc())
                .fetch();

        return records.stream()
                .map(this::recordToCliente)
                .toList();
    }

    /**
     * Busca cliente por email
     */
    public Optional<Cliente> findByEmail(String email) {
        log.debug("Buscando cliente por email: {}", email);

        Record record = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "email")).eq(email))
                .fetchOne();

        return record != null ? Optional.of(recordToCliente(record)) : Optional.empty();
    }

    /**
     * Busca clientes por empresa
     */
    public List<Cliente> findByEmpresaId(Integer empresaId) {
        log.debug("Buscando clientes para empresa: {}", empresaId);

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "empresa_id")).eq(empresaId))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "nombre")).asc())
                .fetch();

        return records.stream()
                .map(this::recordToCliente)
                .toList();
    }

    /**
     * Busca clientes por delegación
     */
    public List<Cliente> findByDelegacionId(Integer delegacionId) {
        log.debug("Buscando clientes para delegación: {}", delegacionId);

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(field(name(SCHEMA, TABLE_NAME, "delegacion_id")).eq(delegacionId))
                .orderBy(field(name(SCHEMA, TABLE_NAME, "nombre")).asc())
                .fetch();

        return records.stream()
                .map(this::recordToCliente)
                .toList();
    }

    /**
     * Búsqueda avanzada con múltiples filtros
     *
     * @param nombre Nombre del cliente (opcional, búsqueda parcial)
     * @param empresaId ID de empresa (opcional)
     * @param estado Estado del cliente (opcional)
     * @return Lista de clientes que coinciden con los filtros
     */
    public List<Cliente> findByFiltros(String nombre, Integer empresaId, String estado) {
        log.debug("Buscando clientes con filtros: nombre={}, empresa={}, estado={}",
                nombre, empresaId, estado);

        var condition = DSL.noCondition();

        if (nombre != null && !nombre.isEmpty()) {
            // Búsqueda LIKE para nombres (parcial)
            condition = condition.and(
                    field(name(SCHEMA, TABLE_NAME, "nombre")).like("%" + nombre + "%")
            );
        }

        if (empresaId != null) {
            condition = condition.and(
                    field(name(SCHEMA, TABLE_NAME, "empresa_id")).eq(empresaId)
            );
        }

        if (estado != null && !estado.isEmpty()) {
            condition = condition.and(
                    field(name(SCHEMA, TABLE_NAME, "estado")).eq(estado)
            );
        }

        Result<Record> records = dsl
                .select()
                .from(table(name(SCHEMA, TABLE_NAME)))
                .where(condition)
                .orderBy(field(name(SCHEMA, TABLE_NAME, "nombre")).asc())
                .fetch();

        return records.stream()
                .map(this::recordToCliente)
                .toList();
    }

    // ============= ESCRITURAS (INSERT, UPDATE) =============

    /**
     * Inserta nuevo cliente
     */
    public Cliente save(Cliente cliente) {
        log.debug("Guardando nuevo cliente: {}", cliente.getNombre());

        int result = dsl
                .insertInto(
                        table(name(SCHEMA, TABLE_NAME)),
                        field(name(SCHEMA, TABLE_NAME, "nombre")),
                        field(name(SCHEMA, TABLE_NAME, "email")),
                        field(name(SCHEMA, TABLE_NAME, "telefono")),
                        field(name(SCHEMA, TABLE_NAME, "empresa_id")),
                        field(name(SCHEMA, TABLE_NAME, "delegacion_id")),
                        field(name(SCHEMA, TABLE_NAME, "estado")),
                        field(name(SCHEMA, TABLE_NAME, "usuario_creacion"))
                )
                .values(
                        cliente.getNombre(),
                        cliente.getEmail(),
                        cliente.getTelefono(),
                        cliente.getEmpresaId() != null ? cliente.getEmpresaId() : 1,
                        cliente.getDelegacionId() != null ? cliente.getDelegacionId() : 1,
                        cliente.getEstado() != null ? cliente.getEstado() : "ACTIVO",
                        "SYSTEM"
                )
                .execute();

        if (result > 0) {
            log.debug("Cliente guardado exitosamente");

            // Recuperar el cliente insertado por email
            Record record = dsl
                    .select()
                    .from(table(name(SCHEMA, TABLE_NAME)))
                    .where(field(name(SCHEMA, TABLE_NAME, "email")).eq(cliente.getEmail()))
                    .fetchOne();

            if (record != null) {
                return recordToCliente(record);
            }
        }

        return cliente;
    }

    /**
     * Actualiza cliente existente
     */
    public Cliente update(Cliente cliente) {
        log.debug("Actualizando cliente: {}", cliente.getId());

        int result = dsl
                .update(table(name(SCHEMA, TABLE_NAME)))
                .set(field(name(SCHEMA, TABLE_NAME, "nombre")), cliente.getNombre())
                .set(field(name(SCHEMA, TABLE_NAME, "email")), cliente.getEmail())
                .set(field(name(SCHEMA, TABLE_NAME, "telefono")), cliente.getTelefono())
                .set(field(name(SCHEMA, TABLE_NAME, "estado")), cliente.getEstado())
                .set(field(name(SCHEMA, TABLE_NAME, "fecha_modificacion")), now())
                .set(field(name(SCHEMA, TABLE_NAME, "usuario_modificacion")), "SYSTEM")
                .where(field(name(SCHEMA, TABLE_NAME, "id")).eq(cliente.getId()))
                .execute();

        if (result > 0) {
            log.debug("Cliente actualizado exitosamente");
        }

        return cliente;
    }

    // ============= UTILIDADES =============

    /**
     * Convierte un Record de JOOQ a objeto Cliente
     */
    private Cliente recordToCliente(Record record) {
        return Cliente.builder()
                .id(record.get("id", Long.class))
                .nombre(record.get("nombre", String.class))
                .email(record.get("email", String.class))
                .telefono(record.get("telefono", String.class))
                .empresaId(record.get("empresa_id", Integer.class))
                .delegacionId(record.get("delegacion_id", Integer.class))
                .estado(record.get("estado", String.class))
                .fechaCreacion(record.get("fecha_creacion", String.class))
                .usuarioCreacion(record.get("usuario_creacion", String.class))
                .fechaModificacion(record.get("fecha_modificacion", String.class))
                .usuarioModificacion(record.get("usuario_modificacion", String.class))
                .build();
    }
}