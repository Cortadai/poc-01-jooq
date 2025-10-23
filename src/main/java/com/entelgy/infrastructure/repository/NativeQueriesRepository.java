package com.entelgy.infrastructure.repository;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ejemplos de Native Queries con jOOQ
 * Diferentes patrones y casos de uso
 */
@Repository
public class NativeQueriesRepository {

    private final DSLContext dsl;

    public NativeQueriesRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // ============================================================================
    // 1. QUERIES SIMPLES CON MAPEO A MAPS
    // ============================================================================

    /**
     * Obtener todos los contratos vigentes (retorna Maps)
     */
    public List<Map<String, Object>> obtenerContratosVigentes() {
        String sql = "SELECT id, numero, estado, precio_anual FROM contratos WHERE estado = ?";
        return dsl.fetch(sql, "VIGENTE").intoMaps();
    }

    /**
     * Obtener un contrato específico
     */
    public Optional<Map<String, Object>> obtenerContratoPorId(Integer id) {
        String sql = "SELECT * FROM contratos WHERE id = ?";
        return Optional.ofNullable(dsl.fetchOne(sql, id))
                .map(Record::intoMap);
    }

    /**
     * Contar registros (un solo valor)
     */
    public Long contarContratosVigentes() {
        String sql = "SELECT COUNT(*) as total FROM contratos WHERE estado = ?";
        return dsl.fetch(sql, "VIGENTE")
                .get(0)
                .getValue("total", Long.class);
    }

    // ============================================================================
    // 2. QUERIES COMPLEJAS CON JOINS Y AGGREGATES
    // ============================================================================

    /**
     * Análisis de cartera: clientes con estadísticas
     */
    public List<Map<String, Object>> analisCarteraCompleto() {
        String sql = """
            SELECT 
                c.id as cliente_id,
                c.nombre as cliente_nombre,
                COUNT(con.id) as total_contratos,
                COUNT(CASE WHEN con.estado = 'VIGENTE' THEN 1 END) as vigentes,
                COUNT(CASE WHEN con.estado = 'VENCIDO' THEN 1 END) as vencidos,
                SUM(con.precio_anual) as volumen_total,
                SUM(CASE WHEN con.estado = 'VIGENTE' THEN con.precio_anual ELSE 0 END) as volumen_vigente,
                ROUND(AVG(con.precio_anual), 2) as precio_promedio
            FROM clientes c
            LEFT JOIN contratos con ON c.id = con.cliente_id
            GROUP BY c.id, c.nombre
            ORDER BY volumen_total DESC NULLS LAST
            """;
        return dsl.fetch(sql).intoMaps();
    }

    /**
     * Top 5 clientes por volumen de contratación
     */
    public List<Map<String, Object>> topClientesPorVolumen(Integer limit) {
        String sql = """
            SELECT c.nombre, COUNT(*) as cantidad_contratos, 
                   SUM(con.precio_anual) as volumen,
                   ROUND(AVG(con.precio_anual), 2) as precio_medio
            FROM clientes c
            JOIN contratos con ON c.id = con.cliente_id
            WHERE con.estado = 'VIGENTE'
            GROUP BY c.id, c.nombre
            HAVING COUNT(*) > 0
            ORDER BY volumen DESC
            LIMIT ?
            """;
        return dsl.fetch(sql, limit).intoMaps();
    }

    /**
     * Contratos que vencen en los próximos N días
     */
    public List<Map<String, Object>> contratosProximosAVencer(Integer diasAdelante) {
        String sql = """
            SELECT c.nombre as cliente, con.numero, con.precio_anual,
                   con.fecha_fin, (con.fecha_fin - CURRENT_DATE) as dias_para_vencer
            FROM clientes c
            JOIN contratos con ON c.id = con.cliente_id
            WHERE con.estado = 'VIGENTE'
              AND con.fecha_fin BETWEEN CURRENT_DATE AND CURRENT_DATE + ?
            ORDER BY con.fecha_fin ASC
            """;
        return dsl.fetch(sql, diasAdelante).intoMaps();
    }

    // ============================================================================
    // 3. QUERIES CON PARÁMETROS MÚLTIPLES
    // ============================================================================

    /**
     * Filtrar contratos por múltiples criterios
     */
    public List<Map<String, Object>> filtrarContratos(
            String estado,
            BigDecimal precioMinimo,
            LocalDate fechaDesde) {
        String sql = """
            SELECT * FROM contratos 
            WHERE estado = ? 
              AND precio_anual >= ? 
              AND fecha_inicio >= ?
            ORDER BY precio_anual DESC
            """;
        return dsl.fetch(sql, estado, precioMinimo, fechaDesde).intoMaps();
    }

    /**
     * Búsqueda con operador LIKE
     */
    public List<Map<String, Object>> buscarClientes(String termino) {
        String sql = "SELECT * FROM clientes WHERE nombre ILIKE ? ORDER BY nombre";
        return dsl.fetch(sql, "%" + termino + "%").intoMaps();
    }

    // ============================================================================
    // 4. QUERIES CON CTEs (WITH)
    // ============================================================================

    /**
     * Estadísticas usando CTE (Common Table Expression)
     */
    public List<Map<String, Object>> estadisticasConCTE() {
        String sql = """
            WITH contratos_por_estado AS (
                SELECT 
                    estado,
                    COUNT(*) as cantidad,
                    SUM(precio_anual) as volumen,
                    ROUND(AVG(precio_anual), 2) as promedio
                FROM contratos
                GROUP BY estado
            )
            SELECT * FROM contratos_por_estado
            ORDER BY cantidad DESC
            """;
        return dsl.fetch(sql).intoMaps();
    }

    /**
     * Análisis por estado con rankings
     */
    public List<Map<String, Object>> contratosConRanking() {
        String sql = """
            SELECT 
                numero,
                estado,
                precio_anual,
                ROW_NUMBER() OVER (PARTITION BY estado ORDER BY precio_anual DESC) as ranking,
                RANK() OVER (ORDER BY precio_anual DESC) as ranking_general
            FROM contratos
            ORDER BY estado, ranking
            """;
        return dsl.fetch(sql).intoMaps();
    }

    // ============================================================================
    // 5. INSERTS/UPDATES/DELETES NATIVOS
    // ============================================================================

    /**
     * Insertar un nuevo contrato
     */
    public int insertarContrato(Integer clienteId, String numero, String estado, BigDecimal precio) {
        String sql = """
            INSERT INTO contratos (cliente_id, numero, estado, precio_anual, tipo_contrato, 
                                  fecha_inicio, fecha_fin, fecha_creacion)
            VALUES (?, ?, ?, ?, 'Estándar', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 year', CURRENT_TIMESTAMP)
            """;
        return dsl.execute(sql, clienteId, numero, estado, precio);
    }

    /**
     * Actualizar estado de múltiples contratos
     */
    public int actualizarMultiplesContratos(String estadoActual, String estadoNuevo) {
        String sql = """
            UPDATE contratos 
            SET estado = ?, fecha_modificacion = CURRENT_TIMESTAMP
            WHERE estado = ?
            """;
        return dsl.execute(sql, estadoNuevo, estadoActual);
    }

    /**
     * Eliminar registros antiguos
     */
    public int eliminarContratosAntiguos(LocalDate fechaLimite) {
        String sql = "DELETE FROM contratos WHERE fecha_fin < ? AND estado = ?";
        return dsl.execute(sql, fechaLimite, "CANCELADO");
    }

    // ============================================================================
    // 6. BATCH OPERATIONS
    // ============================================================================

    /**
     * Insertar múltiples contratos en batch
     */
    public void insertarContratosEnBatch(List<Map<String, Object>> contratos) {
        contratos.forEach(contrato -> {
            String sql = """
                INSERT INTO contratos (cliente_id, numero, estado, precio_anual)
                VALUES (?, ?, ?, ?)
                """;
            dsl.execute(sql,
                    contrato.get("cliente_id"),
                    contrato.get("numero"),
                    contrato.get("estado"),
                    contrato.get("precio_anual")
            );
        });
    }

    // ============================================================================
    // 7. QUERIES AGREGADAS CON HAVING
    // ============================================================================

    /**
     * Clientes con más de N contratos vigentes
     */
    public List<Map<String, Object>> clientesConMultiplesContratos(Integer minimoContratos) {
        String sql = """
            SELECT c.nombre, COUNT(*) as cantidad
            FROM clientes c
            JOIN contratos con ON c.id = con.cliente_id
            WHERE con.estado = 'VIGENTE'
            GROUP BY c.id, c.nombre
            HAVING COUNT(*) >= ?
            ORDER BY cantidad DESC
            """;
        return dsl.fetch(sql, minimoContratos).intoMaps();
    }

    // ============================================================================
    // 8. QUERIES CON CASE WHEN
    // ============================================================================

    /**
     * Clasificación de contratos por riesgo
     */
    public List<Map<String, Object>> clasificacionRiesgo() {
        String sql = """
            SELECT 
                numero,
                estado,
                (fecha_fin - CURRENT_DATE) as dias_restantes,
                CASE 
                    WHEN estado = 'VENCIDO' THEN 'CRÍTICO'
                    WHEN (fecha_fin - CURRENT_DATE) < 30 THEN 'ALTO'
                    WHEN (fecha_fin - CURRENT_DATE) < 90 THEN 'MEDIO'
                    ELSE 'BAJO'
                END as nivel_riesgo
            FROM contratos
            ORDER BY dias_restantes ASC
            """;
        return dsl.fetch(sql).intoMaps();
    }

    // ============================================================================
    // 9. QUERIES CON SUBCONSULTAS
    // ============================================================================

    /**
     * Contratos por encima del promedio
     */
    public List<Map<String, Object>> contratosEncimaProm() {
        String sql = """
            SELECT numero, precio_anual
            FROM contratos
            WHERE precio_anual > (SELECT AVG(precio_anual) FROM contratos)
            ORDER BY precio_anual DESC
            """;
        return dsl.fetch(sql).intoMaps();
    }

    // ============================================================================
    // 10. QUERIES PAGINATED
    // ============================================================================

    /**
     * Obtener contratos con paginación
     */
    public List<Map<String, Object>> obtenerContratosPageable(Integer pageNumber, Integer pageSize) {
        Integer offset = (pageNumber - 1) * pageSize;
        String sql = """
            SELECT * FROM contratos 
            ORDER BY id 
            LIMIT ? OFFSET ?
            """;
        return dsl.fetch(sql, pageSize, offset).intoMaps();
    }

    /**
     * Total de registros para cálculo de páginas
     */
    public Long contarTotalContratos() {
        String sql = "SELECT COUNT(*) as total FROM contratos";
        return dsl.fetch(sql)
                .get(0)
                .getValue("total", Long.class);
    }

    // ============================================================================
    // 11. QUERIES CON WINDOW FUNCTIONS
    // ============================================================================

    /**
     * Análisis con running total
     */
    public List<Map<String, Object>> contratosConRunningTotal() {
        String sql = """
            SELECT 
                numero,
                precio_anual,
                SUM(precio_anual) OVER (ORDER BY id) as running_total,
                LAG(precio_anual) OVER (ORDER BY id) as precio_anterior,
                LEAD(precio_anual) OVER (ORDER BY id) as precio_siguiente
            FROM contratos
            ORDER BY id
            """;
        return dsl.fetch(sql).intoMaps();
    }

    // ============================================================================
    // 12. QUERIES PARA REPORTING
    // ============================================================================

    /**
     * Reporte mensual de contratación
     */
    public List<Map<String, Object>> reporteMensual() {
        String sql = """
            SELECT 
                TO_CHAR(fecha_inicio, 'YYYY-MM') as mes,
                COUNT(*) as cantidad,
                SUM(precio_anual) as total_volumen,
                ROUND(AVG(precio_anual), 2) as promedio
            FROM contratos
            WHERE EXTRACT(YEAR FROM fecha_inicio) = EXTRACT(YEAR FROM CURRENT_DATE)
            GROUP BY TO_CHAR(fecha_inicio, 'YYYY-MM')
            ORDER BY mes DESC
            """;
        return dsl.fetch(sql).intoMaps();
    }

    /**
     * Dashboard: métricas principales
     */
    public Map<String, Object> obtenerMetricasPrincipales() {
        String sql = """
            SELECT 
                COUNT(*) as total_contratos,
                COUNT(CASE WHEN estado = 'VIGENTE' THEN 1 END) as vigentes,
                COUNT(CASE WHEN estado = 'VENCIDO' THEN 1 END) as vencidos,
                SUM(precio_anual) as volumen_total,
                ROUND(AVG(precio_anual), 2) as precio_promedio
            FROM contratos
            """;
        return dsl.fetch(sql).get(0).intoMap();
    }

    // ============================================================================
    // 13. ERROR HANDLING
    // ============================================================================

    /**
     * Query con error handling
     */
    public List<Map<String, Object>> obtenerConConErrorHandling(String sql) {
        try {
            return dsl.fetch(sql).intoMaps();
        } catch (org.jooq.exception.DataAccessException e) {
            System.err.println("Error SQL: " + e.getMessage());
            return List.of();
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            return List.of();
        }
    }

}