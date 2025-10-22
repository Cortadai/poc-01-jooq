package com.entelgy.infrastructure.repository;

import com.entelgy.application.dto.ClienteConContratosDTO;
import com.entelgy.application.dto.ContratoActivoClienteEmpresaDTO;
import com.entelgy.application.dto.ContratoClienteDTO;
import com.entelgy.application.dto.ContratoDetalleDTO;
import com.entelgy.jooq.generated.tables.Clientes;
import com.entelgy.jooq.generated.tables.Contratos;
import com.entelgy.jooq.generated.tables.Instalaciones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository con JOINs type-safe usando JOOQ
 *
 * Responsabilidades:
 * - Construir queries complejas con JOINs
 * - Mapear resultados a DTOs
 * - Acceder a la base de datos
 * - NO contiene lógica de negocio
 *
 * Todos los métodos usan .into(DTO.class) para mapeo automático:
 * - Los nombres de columnas (con alias) deben coincidir con propiedades del DTO
 * - JOOQ mapea automáticamente usando reflexión
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JoinExamplesRepository {

    private final DSLContext dsl;

    // Tablas generadas por JOOQ (type-safe)
    private static final Contratos CONTRATOS = Contratos.CONTRATOS;
    private static final Clientes CLIENTES = Clientes.CLIENTES;
    private static final Instalaciones INSTALACIONES = Instalaciones.INSTALACIONES;

    // ============= INNER JOINs (Solo registros con coincidencia) =============

    /**
     * INNER JOIN: Contratos + Clientes
     *
     * Retorna solo contratos que tienen cliente asociado
     *
     * SQL generado:
     * SELECT c.id, c.numero, c.estado, cl.nombre, cl.email
     * FROM contratos c
     * INNER JOIN clientes cl ON c.cliente_id = cl.id
     * ORDER BY c.numero
     *
     * Mapeo automático (nombres de alias → propiedades de DTO):
     * - id → id
     * - numero → numero
     * - estado → estado
     * - nombre → clienteNombre (porque alias es "clienteNombre")
     * - email → clienteEmail (porque alias es "clienteEmail")
     *
     * @return Lista de ContratoClienteDTO (nunca null, puede estar vacía)
     */
    public List<ContratoClienteDTO> getContratosConClientes() {
        log.debug("Ejecutando query: INNER JOIN Contratos + Clientes");

        return dsl
                .select(
                        CONTRATOS.ID,
                        CONTRATOS.NUMERO,
                        CONTRATOS.ESTADO,
                        CLIENTES.NOMBRE.as("clienteNombre"),      // ← Alias importante
                        CLIENTES.EMAIL.as("clienteEmail")         // ← Alias importante
                )
                .from(CONTRATOS)
                .innerJoin(CLIENTES)
                .on(CONTRATOS.CLIENTE_ID.eq(CLIENTES.ID))
                .orderBy(CONTRATOS.NUMERO)
                .fetch()
                .into(ContratoClienteDTO.class);  // ← Mapeo automático
    }

    /**
     * Múltiples INNER JOINs: Contratos + Clientes + Instalaciones
     *
     * Retorna contratos con información completa del cliente e instalación
     * Solo retorna registros donde existan TODOS los datos (cliente e instalación)
     *
     * SQL generado:
     * SELECT c.id, c.numero, c.estado, c.precio_anual,
     *        cl.nombre, cl.telefono,
     *        inst.ubicacion, inst.tipo
     * FROM contratos c
     * INNER JOIN clientes cl ON c.cliente_id = cl.id
     * INNER JOIN instalaciones inst ON c.instalacion_id = inst.id
     * WHERE c.estado = 'VIGENTE'
     * ORDER BY cl.nombre, inst.ubicacion
     *
     * @return Lista de ContratoDetalleDTO con información completa
     */
    public List<ContratoDetalleDTO> getContratosConClientesEInstalaciones() {
        log.debug("Ejecutando query: INNER JOIN múltiple (Contratos + Clientes + Instalaciones)");

        return dsl
                .select(
                        CONTRATOS.ID,
                        CONTRATOS.NUMERO,
                        CONTRATOS.ESTADO,
                        CONTRATOS.PRECIO_ANUAL,
                        CLIENTES.NOMBRE.as("clienteNombre"),
                        CLIENTES.TELEFONO.as("clienteTelefono"),
                        INSTALACIONES.UBICACION.as("instalacionUbicacion"),
                        INSTALACIONES.TIPO.as("instalacionTipo")
                )
                .from(CONTRATOS)
                .innerJoin(CLIENTES)
                .on(CONTRATOS.CLIENTE_ID.eq(CLIENTES.ID))
                .innerJoin(INSTALACIONES)
                .on(CONTRATOS.INSTALACION_ID.eq(INSTALACIONES.ID))
                .where(CONTRATOS.ESTADO.eq("VIGENTE"))
                .orderBy(CLIENTES.NOMBRE, INSTALACIONES.UBICACION)
                .fetch()
                .into(ContratoDetalleDTO.class);
    }

    /**
     * INNER JOIN con WHERE complejo: Contratos activos + Clientes empresariales
     *
     * Filtra:
     * - Solo contratos VIGENTES
     * - Solo clientes con emails que contienen 'empresa'
     *
     * SQL generado:
     * SELECT c.numero, c.precio_anual, cl.nombre, cl.email
     * FROM contratos c
     * INNER JOIN clientes cl ON c.cliente_id = cl.id
     * WHERE c.estado = 'VIGENTE' AND cl.email LIKE '%empresa%'
     * ORDER BY c.precio_anual DESC
     *
     * @return Lista de ContratoActivoClienteEmpresaDTO ordenados por precio (mayor primero)
     */
    public List<ContratoActivoClienteEmpresaDTO> getContratosActivosConClientesEmpresa() {
        log.debug("Ejecutando query: INNER JOIN con WHERE complejo (Contratos activos + Clientes empresariales)");

        return dsl
                .select(
                        CONTRATOS.NUMERO.as("numeroContrato"),
                        CONTRATOS.PRECIO_ANUAL,
                        CLIENTES.NOMBRE.as("clienteNombre"),
                        CLIENTES.EMAIL.as("clienteEmail")
                )
                .from(CONTRATOS)
                .innerJoin(CLIENTES)
                .on(CONTRATOS.CLIENTE_ID.eq(CLIENTES.ID))
                .where(
                        CONTRATOS.ESTADO.eq("VIGENTE")
                                .and(CLIENTES.EMAIL.like("%empresa%"))
                )
                .orderBy(CONTRATOS.PRECIO_ANUAL.desc())
                .fetch()
                .into(ContratoActivoClienteEmpresaDTO.class);
    }

    // ============= LEFT JOINs (Todos los registros de la tabla izquierda) =============

    /**
     * LEFT JOIN: Todos los clientes con sus contratos
     *
     * IMPORTANTE:
     * - Retorna TODOS los clientes, aunque no tengan contratos
     * - Clientes sin contratos tienen NULL en los campos de contrato
     * - Un cliente puede aparecer múltiples veces si tiene múltiples contratos
     *
     * Útil para:
     * - Identificar clientes sin contratos
     * - Análisis de penetración de cartera
     * - Estrategias de venta/retención
     *
     * SQL generado:
     * SELECT cl.id, cl.nombre, cl.email,
     *        c.numero, c.estado
     * FROM clientes cl
     * LEFT JOIN contratos c ON cl.id = c.cliente_id
     * ORDER BY cl.nombre, c.numero
     *
     * Ejemplo de resultados:
     * id | nombre    | email        | numero      | estado
     * 1  | Cliente A | a@empresa.es | CTR-001     | VIGENTE
     * 1  | Cliente A | a@empresa.es | CTR-002     | VENCIDO
     * 2  | Cliente B | b@empresa.es | NULL        | NULL      ← Sin contratos
     * 3  | Cliente C | c@empresa.es | CTR-003     | VIGENTE
     *
     * @return Lista de ClienteConContratosDTO (puede haber NULL en campos de contrato)
     */
    public List<ClienteConContratosDTO> getTodosClientesConSusContratos() {
        log.debug("Ejecutando query: LEFT JOIN Clientes + Contratos");

        return dsl
                .select(
                        CLIENTES.ID.as("clienteId"),
                        CLIENTES.NOMBRE.as("clienteNombre"),
                        CLIENTES.EMAIL.as("clienteEmail"),
                        CONTRATOS.NUMERO.as("contratoNumero"),   // ← Puede ser NULL
                        CONTRATOS.ESTADO.as("contratoEstado")    // ← Puede ser NULL
                )
                .from(CLIENTES)
                .leftJoin(CONTRATOS)
                .on(CLIENTES.ID.eq(CONTRATOS.CLIENTE_ID))
                .orderBy(CLIENTES.NOMBRE, CONTRATOS.NUMERO)
                .fetch()
                .into(ClienteConContratosDTO.class);
    }

    // ============= UTILIDADES Y ANÁLISIS =============

    /**
     * Obtener número total de contratos
     *
     * @return Cantidad total de contratos en la BD
     */
    public long contarTotalContratos() {
        log.debug("Contando total de contratos");

        return dsl
                .selectCount()
                .from(CONTRATOS)
                .fetchOne(0, Long.class);
    }

    /**
     * Obtener número de contratos por estado
     *
     * @param estado Estado a contar (ej: "VIGENTE", "VENCIDO")
     * @return Cantidad de contratos en ese estado
     */
    public long contarContratosPorEstado(String estado) {
        log.debug("Contando contratos con estado: {}", estado);

        return dsl
                .selectCount()
                .from(CONTRATOS)
                .where(CONTRATOS.ESTADO.eq(estado))
                .fetchOne(0, Long.class);
    }
}