# 📚 HELP - Native Queries con jOOQ

## 🎯 Qué son las Native Queries en esta POC

Las Native Queries son consultas SQL escritas en texto plano que se ejecutan directamente desde Java usando jOOQ. Son flexibles y permiten SQL complejo sin estar limitados por el DSL builder.

### 27 Queries implementadas en 8 categorías

1. **Queries simples** (3) - SELECT básicos
2. **Queries complejas** (4) - JOINs y agregados
3. **Filtros** (4) - Búsqueda y filtrado
4. **CTEs y Window Functions** (3) - Análisis avanzado
5. **Análisis y clasificación** (4) - Reportes
6. **Paginación** (3) - Manejo de grandes datasets
7. **Reporting** (2) - Dashboards
8. **CRUD** (3) - INSERT, UPDATE, DELETE

---

## 📂 Estructura de archivos

```
src/
├── main/
│   ├── java/com/entelgy/
│   │   ├── presentation/
│   │   │   └── NativeQueriesController.java      ← Endpoints HTTP
│   │   └── infrastructure/repository/
│   │       └── NativeQueriesRepository.java      ← Ejecución de queries
```

---

## 🔧 Componentes principales

### 1. NativeQueriesController.java
**Responsabilidad:** Mapear HTTP a métodos del repository

```java
@RestController
@RequestMapping("/api/native-queries")
@CrossOrigin(origins = "*")
public class NativeQueriesController {
    
    private final NativeQueriesRepository repository;
    
    @GetMapping("/contratos/vigentes")
    public ResponseEntity<?> obtenerVigentes() { ... }
    
    @GetMapping("/top-clientes")
    public ResponseEntity<?> topClientes(@RequestParam(defaultValue = "5") Integer limit) { ... }
}
```

**Tips:**
- ✅ Usa `@RequestParam` para parámetros opcionales con `defaultValue`
- ✅ Usa `@PathVariable` para parámetros en URL
- ✅ Usa `@CrossOrigin` para CORS
- ✅ Retorna `ResponseEntity<?>` para flexibilidad

---

### 2. NativeQueriesRepository.java
**Responsabilidad:** Ejecutar SQL nativo y retornar datos

```java
@Repository
public class NativeQueriesRepository {
    
    private final DSLContext dsl;
    
    public List<Map<String, Object>> obtenerContratosVigentes() {
        String sql = "SELECT id, numero, estado, precio_anual FROM contratos WHERE estado = ?";
        return dsl.fetch(sql, "VIGENTE").intoMaps();
    }
}
```

**Tips:**
- ✅ Inyecta `DSLContext` de Spring
- ✅ Usa bind parameters `?` para seguridad (SQL injection)
- ✅ Usa `.intoMaps()` para convertir `Result<Record>` a `List<Map>`
- ✅ Usa `.fetchOne()` para una sola fila
- ✅ Usa `.fetchAny()` si no sabes si hay resultado

---

## 💡 Buenas prácticas aplicadas

### 1. Seguridad: Bind Parameters
```java
// ❌ MAL - vulnerable a SQL injection
String sql = "SELECT * FROM contratos WHERE id = " + id;
dsl.fetch(sql);

// ✅ BIEN - seguro con bind parameters
String sql = "SELECT * FROM contratos WHERE id = ?";
dsl.fetch(sql, id);
```

### 2. Conversión de tipos
```java
// Método fetch retorna Result<Record>
Result<Record> result = dsl.fetch(sql);

// Convertir a List<Map>
List<Map<String, Object>> maps = result.intoMaps();

// Acceder a valores
for (Map<String, Object> row : maps) {
    Integer id = (Integer) row.get("id");
    String nombre = (String) row.get("nombre");
}
```

### 3. Obtener un valor único
```java
// fetchOne() retorna Record o null
Record record = dsl.fetchOne(sql, id);

// Convertir a Map con Optional
Optional<Map<String, Object>> result = 
    Optional.ofNullable(dsl.fetchOne(sql, id))
            .map(Record::intoMap);
```

### 4. Paginación eficiente
```java
// Query 1: obtener página
String sql = "SELECT * FROM contratos LIMIT ? OFFSET ?";
List<Map<String, Object>> data = dsl.fetch(sql, pageSize, offset).intoMaps();

// Query 2: contar total
String countSql = "SELECT COUNT(*) FROM contratos";
Long total = dsl.fetch(countSql).get(0).getValue(0, Long.class);

// Calcular total páginas en Java
Integer totalPages = (int) Math.ceil((double) total / pageSize);
```

### 5. CTEs (WITH clause) para complejidad
```java
String sql = """
    WITH stats AS (
        SELECT estado, COUNT(*) as cantidad
        FROM contratos
        GROUP BY estado
    )
    SELECT * FROM stats
    """;
return dsl.fetch(sql).intoMaps();
```

### 6. Window Functions para análisis
```java
String sql = """
    SELECT numero, precio_anual,
           ROW_NUMBER() OVER (ORDER BY precio_anual DESC) as ranking,
           SUM(precio_anual) OVER (ORDER BY id) as running_total
    FROM contratos
    """;
return dsl.fetch(sql).intoMaps();
```

---

## 🧪 CÓMO PROBAR - Curls por categoría

### CATEGORÍA 1: QUERIES SIMPLES

#### Test 1: Obtener contratos vigentes
```bash
curl http://localhost:8080/api/native-queries/contratos/vigentes | jq .
```

**Response:**
```json
{
  "cantidad": 3,
  "datos": [
    {"id": 1, "numero": "CONT-001", "estado": "VIGENTE", "precio_anual": 12000},
    {"id": 2, "numero": "CONT-002", "estado": "VIGENTE", "precio_anual": 15000}
  ]
}
```

**SQL ejecutado:**
```sql
SELECT id, numero, estado, precio_anual FROM contratos WHERE estado = ?
```

#### Test 2: Obtener contrato por ID
```bash
curl http://localhost:8080/api/native-queries/contratos/1 | jq .
```

**Response:**
```json
{
  "id": 1,
  "numero": "CONT-001",
  "estado": "VIGENTE",
  "precio_anual": 12000
}
```

**SQL ejecutado:**
```sql
SELECT * FROM contratos WHERE id = ?
```

#### Test 3: Contar vigentes
```bash
curl http://localhost:8080/api/native-queries/contratos/contar/vigentes | jq .
```

**Response:**
```json
{"total_vigentes": 3}
```

---

### CATEGORÍA 2: QUERIES COMPLEJAS

#### Test 4: Análisis de cartera
```bash
curl http://localhost:8080/api/native-queries/analisis/cartera | jq .
```

**Response:**
```json
{
  "total_clientes": 3,
  "datos": [
    {
      "cliente_id": 1,
      "cliente_nombre": "Cliente A",
      "total_contratos": 3,
      "vigentes": 1,
      "vencidos": 2,
      "volumen_total": 40000,
      "volumen_vigentes": 12000,
      "precio_promedio": 13333.33
    }
  ]
}
```

**SQL - Usa JOINs y agregados:**
```sql
SELECT c.id, c.nombre, COUNT(*) as total_contratos,
       SUM(con.precio_anual) as volumen_total
FROM clientes c
LEFT JOIN contratos con ON c.id = con.cliente_id
GROUP BY c.id, c.nombre
```

#### Test 5: Top 5 clientes por volumen
```bash
curl "http://localhost:8080/api/native-queries/top-clientes?limit=5" | jq .
```

**Response:**
```json
{
  "top": 5,
  "clientes": [
    {
      "nombre": "Cliente A",
      "cantidad_contratos": 3,
      "volumen": 40000,
      "precio_medio": 13333.33
    }
  ]
}
```

#### Test 6: Próximos a vencer (30 días)
```bash
curl "http://localhost:8080/api/native-queries/contratos/proximos-vencer?dias=30" | jq .
```

**Response:**
```json
{
  "dias_adelante": 30,
  "cantidad": 2,
  "datos": [
    {
      "cliente": "Cliente A",
      "numero": "CONT-001",
      "precio_anual": 12000,
      "fecha_fin": "2025-12-31",
      "dias_para_vencer": 69
    }
  ]
}
```

---

### CATEGORÍA 3: FILTROS

#### Test 7: Filtrar con múltiples criterios
```bash
curl -X POST "http://localhost:8080/api/native-queries/contratos/filtrar?estado=VIGENTE&precioMinimo=10000&fechaDesde=2025-01-01" | jq .
```

**Response:**
```json
{
  "filtros": {
    "estado": "VIGENTE",
    "precio_minimo": 10000,
    "fecha_desde": "2025-01-01"
  },
  "total": 2,
  "datos": [...]
}
```

**SQL - Múltiples WHERE:**
```sql
SELECT * FROM contratos 
WHERE estado = ? AND precio_anual >= ? AND fecha_inicio >= ?
ORDER BY precio_anual DESC
```

#### Test 8: Buscar clientes
```bash
curl "http://localhost:8080/api/native-queries/clientes/buscar?termino=Empresa" | jq .
```

**Response:**
```json
{
  "termino": "Empresa",
  "resultados": 2,
  "datos": [
    {"id": 1, "nombre": "Cliente A - Empresa X"},
    {"id": 2, "nombre": "Cliente B - Empresa Y"}
  ]
}
```

**SQL - ILIKE (case-insensitive):**
```sql
SELECT * FROM clientes WHERE nombre ILIKE ? ORDER BY nombre
```

---

### CATEGORÍA 4: CTEs Y WINDOW FUNCTIONS

#### Test 9: Estadísticas por estado (CTE)
```bash
curl http://localhost:8080/api/native-queries/estadisticas/por-estado | jq .
```

**Response:**
```json
{
  "total_grupos": 2,
  "datos": [
    {"estado": "VIGENTE", "cantidad": 1, "volumen": 12000, "promedio": 12000},
    {"estado": "VENCIDO", "cantidad": 2, "volumen": 28000, "promedio": 14000}
  ]
}
```

**SQL - WITH clause (CTE):**
```sql
WITH contratos_por_estado AS (
    SELECT estado, COUNT(*) as cantidad, SUM(precio_anual) as volumen
    FROM contratos
    GROUP BY estado
)
SELECT * FROM contratos_por_estado ORDER BY cantidad DESC
```

#### Test 10: Contratos con ranking
```bash
curl http://localhost:8080/api/native-queries/contratos/con-ranking | jq .
```

**Response:**
```json
{
  "total": 3,
  "datos": [
    {"numero": "CONT-001", "estado": "VIGENTE", "precio_anual": 15000, "ranking": 1},
    {"numero": "CONT-002", "estado": "VIGENTE", "precio_anual": 12000, "ranking": 2}
  ]
}
```

**SQL - Window Function ROW_NUMBER():**
```sql
SELECT numero, precio_anual,
       ROW_NUMBER() OVER (ORDER BY precio_anual DESC) as ranking
FROM contratos
```

---

### CATEGORÍA 5: ANÁLISIS Y CLASIFICACIÓN

#### Test 11: Clasificación de riesgo
```bash
curl http://localhost:8080/api/native-queries/contratos/riesgo | jq .
```

**Response:**
```json
{
  "total_analizado": 3,
  "datos": [
    {
      "numero": "CONT-001",
      "estado": "VIGENTE",
      "dias_restantes": 69,
      "nivel_riesgo": "BAJO"
    },
    {
      "numero": "CONT-002",
      "estado": "VENCIDO",
      "dias_restantes": -50,
      "nivel_riesgo": "CRÍTICO"
    }
  ]
}
```

**SQL - CASE WHEN para clasificación:**
```sql
SELECT numero, estado, (fecha_fin - CURRENT_DATE) as dias_restantes,
       CASE 
           WHEN estado = 'VENCIDO' THEN 'CRÍTICO'
           WHEN (fecha_fin - CURRENT_DATE) < 30 THEN 'ALTO'
           ELSE 'BAJO'
       END as nivel_riesgo
FROM contratos
```

#### Test 12: Dashboard - Métricas principales
```bash
curl http://localhost:8080/api/native-queries/dashboard/metricas | jq .
```

**Response:**
```json
{
  "status": "ok",
  "metricas": {
    "total_contratos": 6,
    "vigentes": 2,
    "vencidos": 4,
    "volumen_total": 70000,
    "precio_promedio": 11666.67
  }
}
```

**SQL - Agregados sin GROUP BY:**
```sql
SELECT COUNT(*) as total, COUNT(CASE WHEN estado='VIGENTE' THEN 1 END) as vigentes,
       SUM(precio_anual) as volumen_total
FROM contratos
```

---

### CATEGORÍA 6: PAGINACIÓN

#### Test 13: Página 1 (10 registros)
```bash
curl "http://localhost:8080/api/native-queries/contratos/paginar?page=1&size=10" | jq .
```

**Response:**
```json
{
  "page": 1,
  "page_size": 10,
  "total_elementos": 30,
  "total_pages": 3,
  "datos": [...]
}
```

**SQL - LIMIT OFFSET:**
```sql
SELECT * FROM contratos ORDER BY id LIMIT 10 OFFSET 0
```

#### Test 14: Página 2 (5 registros por página)
```bash
curl "http://localhost:8080/api/native-queries/contratos/paginar?page=2&size=5" | jq .
```

**Response:**
```json
{
  "page": 2,
  "page_size": 5,
  "total_elementos": 30,
  "total_pages": 6,
  "datos": [...]
}
```

---

### CATEGORÍA 7: REPORTING

#### Test 15: Reporte mensual
```bash
curl http://localhost:8080/api/native-queries/reporte/mensual | jq .
```

**Response:**
```json
{
  "meses": 2,
  "datos": [
    {
      "mes": "2025-10",
      "cantidad": 5,
      "total_volumen": 50000,
      "promedio": 10000
    },
    {
      "mes": "2025-09",
      "cantidad": 1,
      "total_volumen": 20000,
      "promedio": 20000
    }
  ]
}
```

**SQL - Groupby mes:**
```sql
SELECT TO_CHAR(fecha_inicio, 'YYYY-MM') as mes, COUNT(*) as cantidad,
       SUM(precio_anual) as total_volumen
FROM contratos
WHERE EXTRACT(YEAR FROM fecha_inicio) = EXTRACT(YEAR FROM CURRENT_DATE)
GROUP BY TO_CHAR(fecha_inicio, 'YYYY-MM')
```

---

### CATEGORÍA 8: CRUD

#### Test 16: Insertar contrato
```bash
curl -X POST "http://localhost:8080/api/native-queries/contratos/insertar?clienteId=1&numero=CONT-2025-NEW&estado=VIGENTE&precio=25000" | jq .
```

**Response:**
```json
{
  "status": "success",
  "registros_insertados": 1
}
```

**SQL - INSERT:**
```sql
INSERT INTO contratos (cliente_id, numero, estado, precio_anual, tipo_contrato, 
                      fecha_inicio, fecha_fin, fecha_creacion)
VALUES (?, ?, ?, ?, 'Estándar', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 year', CURRENT_TIMESTAMP)
```

#### Test 17: Actualizar estado
```bash
curl -X PUT "http://localhost:8080/api/native-queries/contratos/actualizar-estado?estadoActual=VIGENTE&estadoNuevo=SUSPENDIDO" | jq .
```

**Response:**
```json
{
  "status": "success",
  "registros_actualizados": 2
}
```

**SQL - UPDATE:**
```sql
UPDATE contratos 
SET estado = ?, fecha_modificacion = CURRENT_TIMESTAMP
WHERE estado = ?
```

---

## ⚠️ Errores comunes y cómo evitarlos

### Error 1: "Required type: List<Map<String, Object>>, Provided: Result<Record>"
**Causa:** Olvidas `.intoMaps()` en el fetch

**Solución:**
```java
// ❌ MAL
return dsl.fetch(sql);

// ✅ BIEN
return dsl.fetch(sql).intoMaps();
```

### Error 2: "NULL reference when accessing map"
**Causa:** Accedes a columna que no existe o es null

**Solución:**
```java
// ❌ MAL
Integer id = (Integer) row.get("id");  // puede ser null

// ✅ BIEN
Integer id = (Integer) row.get("id");
if (id == null) {
    // manejar null
}

// O usar Optional
Integer id = Optional.ofNullable((Integer) row.get("id"))
    .orElse(0);
```

### Error 3: "SQL syntax error" en PostgreSQL
**Causa:** Usas funciones de MySQL (YEAR, DATEDIFF, etc)

**Solución:**
```sql
-- ❌ MAL (MySQL)
WHERE YEAR(fecha) = 2025
AND DATEDIFF(fecha_fin, CURRENT_DATE) < 30

-- ✅ BIEN (PostgreSQL)
WHERE EXTRACT(YEAR FROM fecha) = 2025
AND (fecha_fin - CURRENT_DATE) < 30
```

### Error 4: SQL Injection cuando concatenas
**Causa:** Concatenas valores directamente en SQL

**Solución:**
```java
// ❌ MAL - vulnerable
String sql = "SELECT * FROM contratos WHERE estado = '" + estado + "'";
dsl.fetch(sql);

// ✅ BIEN - bind parameters
String sql = "SELECT * FROM contratos WHERE estado = ?";
dsl.fetch(sql, estado);
```

---

## 📊 Matriz de queries

| Categoría | Endpoint | Método | Parámetros | SQL Type |
|-----------|----------|--------|------------|----------|
| Simples | `/contratos/vigentes` | GET | - | SELECT simple |
| Simples | `/contratos/{id}` | GET | id | SELECT con WHERE |
| Simples | `/contratos/contar/vigentes` | GET | - | COUNT |
| Complejas | `/analisis/cartera` | GET | - | JOIN + GROUP BY |
| Complejas | `/top-clientes` | GET | limit | JOIN + ORDER BY |
| Complejas | `/contratos/proximos-vencer` | GET | dias | DATE math |
| Filtros | `/contratos/filtrar` | POST | estado, precio, fecha | WHERE multiple |
| Filtros | `/clientes/buscar` | GET | termino | ILIKE |
| CTEs | `/estadisticas/por-estado` | GET | - | WITH clause |
| Window | `/contratos/con-ranking` | GET | - | ROW_NUMBER OVER |
| Análisis | `/contratos/riesgo` | GET | - | CASE WHEN |
| Análisis | `/dashboard/metricas` | GET | - | COUNT/SUM |
| Paginación | `/contratos/paginar` | GET | page, size | LIMIT OFFSET |
| Reporting | `/reporte/mensual` | GET | - | TO_CHAR GROUP BY |
| CRUD | `/contratos/insertar` | POST | clienteId, etc | INSERT |
| CRUD | `/contratos/actualizar-estado` | PUT | estadoActual, nuevo | UPDATE |

---

## 🔍 Debugging

### Ver SQL ejecutado
En `application.yaml`:
```yaml
logging:
  level:
    org.jooq: DEBUG
```

Verás en consola:
```
[jooq] Executing query : SELECT id, numero, estado FROM contratos WHERE estado = ?
[jooq] -> with bind values : SELECT id, numero, estado FROM contratos WHERE estado = 'VIGENTE'
```

### Ejecutar query en pgAdmin
1. Copia el SQL del log
2. Pega en pgAdmin
3. Ejecuta y valida

---

## 📝 Resumen

| Aspecto | Detalle |
|--------|---------|
| **Total queries** | 27 |
| **Categorías** | 8 |
| **Endpoints** | 27 |
| **Patrón** | Native SQL + Bind Parameters |
| **ORM** | jOOQ |
| **Conversión** | `.intoMaps()` |
| **Seguridad** | Bind parameters `?` |
| **Paginación** | LIMIT OFFSET |
| **Análisis** | CTEs, Window Functions, CASE WHEN |