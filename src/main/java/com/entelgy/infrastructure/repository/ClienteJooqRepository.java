package com.entelgy.infrastructure.repository;

import com.entelgy.domain.model.Cliente;
import com.entelgy.jooq.generated.tables.Clientes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JOOQ para Clientes - REFACTORIZADO CON CODE GENERATION
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ClienteJooqRepository {

    private final DSLContext dsl;
    private static final Clientes CLIENTES = Clientes.CLIENTES;

    /**
     * Obtiene cliente por ID
     */
    public Optional<Cliente> findById(Long id) {
        log.debug("Buscando cliente con ID: {}", id);

        Record record = dsl
                .select()
                .from(CLIENTES)
                .where(CLIENTES.ID.eq(Math.toIntExact(id)))
                .fetchOne();

        return record != null ? Optional.of(recordToCliente(record)) : Optional.empty();
    }

    /**
     * Obtiene todos los clientes
     */
    public List<Cliente> findAll() {
        log.debug("Obteniendo todos los clientes");

        Result<Record> records = dsl
                .select()
                .from(CLIENTES)
                .orderBy(CLIENTES.NOMBRE)
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
                .from(CLIENTES)
                .where(CLIENTES.EMAIL.eq(email))
                .fetchOne();

        return record != null ? Optional.of(recordToCliente(record)) : Optional.empty();
    }

    /**
     * Guarda nuevo cliente
     */
    public Cliente save(Cliente cliente) {
        log.debug("Guardando nuevo cliente: {}", cliente.getNombre());

        int result = dsl
                .insertInto(CLIENTES)
                .set(CLIENTES.NOMBRE, cliente.getNombre())
                .set(CLIENTES.EMAIL, cliente.getEmail())
                .set(CLIENTES.TELEFONO, cliente.getTelefono())
                .set(CLIENTES.USUARIO_CREACION, "SYSTEM")
                .execute();

        if (result > 0) {
            log.debug("Cliente guardado exitosamente");

            // Recuperar cliente insertado
            Record record = dsl
                    .selectFrom(CLIENTES)
                    .where(CLIENTES.EMAIL.eq(cliente.getEmail()))
                    .fetchOne();

            if (record != null) {
                return recordToCliente(record);
            }
        }

        return cliente;
    }

    /**
     * Elimina todos los clientes (usado en tests)
     */
    public void delete() {
        log.debug("Eliminando todos los clientes");
        int result = dsl
                .deleteFrom(CLIENTES)
                .execute();
        log.debug("Se eliminaron {} clientes", result);
    }

    /**
     * Elimina un cliente por ID
     *
     * @param id ID del cliente a eliminar
     */
    public void deleteById(Integer id) {
        log.debug("Eliminando cliente con ID: {}", id);
        int result = dsl
                .deleteFrom(CLIENTES)
                .where(CLIENTES.ID.eq(id))
                .execute();
        if (result > 0) {
            log.debug("Cliente eliminado exitosamente");
        } else {
            log.warn("No se encontró cliente con ID: {}", id);
        }
    }

    /**
     * Convierte Record a Cliente
     */
    private Cliente recordToCliente(Record record) {
        return Cliente.builder()
                .id(record.get(CLIENTES.ID))
                .nombre(record.get(CLIENTES.NOMBRE))
                .email(record.get(CLIENTES.EMAIL))
                .telefono(record.get(CLIENTES.TELEFONO))
                .empresaId(record.get(CLIENTES.EMPRESA_ID))
                .fechaCreacion(record.get(CLIENTES.FECHA_CREACION))
                .usuarioCreacion(record.get(CLIENTES.USUARIO_CREACION))
                .build();
    }
}