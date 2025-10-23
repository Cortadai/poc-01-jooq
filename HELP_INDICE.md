# 📚 ÍNDICE MAESTRO - Documentación de la POC

Bienvenido a la Proof of Concept (POC) con **jOOQ** y **PostgreSQL**.

Esta documentación está organizada en 3 archivos HELP principales + este índice.

---

## 📖 Archivos de ayuda

### 1. 🏗️ [HELP_ARQUITECTURA.md](HELP_ARQUITECTURA.md)
**Leer primero si eres nuevo**

Entiende cómo está organizado el proyecto:
- Estructura general (capas)
- Patrón Controller → Service → Repository
- Flujo de una request HTTP
- DTOs y Mappers
- Manejo centralizado de excepciones

**Perfecto para:**
- Entender la arquitectura general
- Aprender el flujo de datos
- Saber dónde modificar qué
- Debugging y troubleshooting

---

### 2. 🗄️ [HELP_STORED_PROCEDURES.md](HELP_STORED_PROCEDURES.md)
**Si trabajas con Stored Procedures**

Todo sobre los 3 Procedures SQL de la POC:
- `sp_actualizar_estado_contratos()` - Actualiza contratos vencidos
- `sp_generar_reporte_cartera()` - Análisis de cartera
- `sp_crear_parte_trabajo()` - Crea partes de trabajo

**Contiene:**
- Cómo funcionan los procedures
- Repository + Service + Controller
- Curls para probar cada uno
- Errores comunes y soluciones

**Endpoints:**
```
GET  /api/procedures/actualizar-vencidos
GET  /api/procedures/reporte-cartera
POST /api/procedures/crear-parte
```

---

### 3. 🔍 [HELP_NATIVE_QUERIES.md](HELP_NATIVE_QUERIES.md)
**Si trabajas con Native Queries**

Todo sobre las 27 Native Queries implementadas:

**8 categorías:**
1. Queries simples (3) - SELECT básicos
2. Queries complejas (4) - JOINs, agregados
3. Filtros (4) - Búsqueda, ILIKE
4. CTEs y Window Functions (3) - Análisis avanzado
5. Análisis y clasificación (4) - CASE WHEN, rankings
6. Paginación (3) - LIMIT OFFSET
7. Reporting (2) - Dashboards, métricas
8. CRUD (3) - INSERT, UPDATE, DELETE

**Contiene:**
- Explicación de cada query
- Curls para probar (con response esperado)
- SQL ejecutado para cada endpoint
- Errores comunes

**Endpoints (27 total):**
```
GET  /api/native-queries/contratos/vigentes
GET  /api/native-queries/contratos/{id}
GET  /api/native-queries/analisis/cartera
GET  /api/native-queries/top-clientes
GET  /api/native-queries/contratos/proximos-vencer
GET  /api/native-queries/estadisticas/por-estado
GET  /api/native-queries/contratos/con-ranking
GET  /api/native-queries/contratos/riesgo
GET  /api/native-queries/dashboard/metricas
POST /api/native-queries/contratos/filtrar
GET  /api/native-queries/clientes/buscar
GET  /api/native-queries/contratos/paginar
... y 14 más
```

---

## 🗺️ Mapa de rutas - Dónde encontrar cada cosa

### Quiero entender...

| Pregunta | Archivo | Sección |
|----------|---------|---------|
| "¿Cómo está organizado el proyecto?" | HELP_ARQUITECTURA | Estructura de capas |
| "¿Cómo fluye una request HTTP?" | HELP_ARQUITECTURA | Flujo de una request HTTP |
| "¿Qué es Controller → Service → Repository?" | HELP_ARQUITECTURA | Patrón Controller → Service → Repository |
| "¿Cómo funcionan los Stored Procedures?" | HELP_STORED_PROCEDURES | Qué son los SP |
| "¿Cómo se usan Native Queries?" | HELP_NATIVE_QUERIES | Qué son las NQ |
| "¿Cuál es la diferencia entre SP y NQ?" | Este archivo | Diferencias SP vs NQ |
| "¿Cómo debugueo errores?" | HELP_ARQUITECTURA + Cada HELP | Sección Debugging |

### Quiero probar...

| Qué probar | Archivo | Curls |
|-----------|---------|-------|
| Actualizar contratos vencidos | HELP_STORED_PROCEDURES | Test 1 |
| Generar reporte de cartera | HELP_STORED_PROCEDURES | Test 2 |
| Crear parte de trabajo | HELP_STORED_PROCEDURES | Test 3 |
| Obtener contratos vigentes | HELP_NATIVE_QUERIES | Test 1 |
| Análisis de cartera | HELP_NATIVE_QUERIES | Test 4 |
| Clientes con más contratos | HELP_NATIVE_QUERIES | Test 18 |
| Paginación | HELP_NATIVE_QUERIES | Tests 20-22 |

### Quiero aprender...

| Tema | Archivo | Sección |
|------|---------|---------|
| DTOs y validación | HELP_ARQUITECTURA | DTOs - Mapeo de datos |
| Manejo de excepciones | HELP_ARQUITECTURA | GlobalExceptionHandler |
| Binding parameters (SQL injection) | HELP_NATIVE_QUERIES | Seguridad: Bind Parameters |
| CTEs (WITH clause) | HELP_NATIVE_QUERIES | CTEs para complejidad |
| Window Functions | HELP_NATIVE_QUERIES | Window Functions para análisis |
| Transacciones | HELP_ARQUITECTURA | Transacciones |
| Logging | HELP_ARQUITECTURA | Logging |

---

## 🎯 Diferencias: Stored Procedures vs Native Queries

| Aspecto | Stored Procedures | Native Queries |
|--------|-------------------|-----------------|
| **Dónde se define** | En BD (SQL) | En Java (String SQL) |
| **Compilación** | En BD | En aplicación |
| **Complejidad** | Alta (lógica compleja) | Media (queries) |
| **Reutilización** | Entre múltiples apps | Solo esta app |
| **Casos de uso** | Operaciones complejas | Reportes, análisis |
| **Performance** | Muy rápido (en BD) | Rápido (en app) |
| **Ejemplos en POC** | 3 procedures | 27 queries |
| **Controllers** | ProcedureController | NativeQueriesController |

**Usar Stored Procedures cuando:**
- ✅ Lógica muy compleja
- ✅ Múltiples aplicaciones necesitan lo mismo
- ✅ Operación crítica para performance
- ✅ Transacciones complejas

**Usar Native Queries cuando:**
- ✅ Reportes y análisis
- ✅ Queries específicas de esta app
- ✅ Necesitas flexibilidad
- ✅ Debugging fácil (ves el SQL)

---

## 🔄 Comparativa de ejemplos

### Actualizar contratos vencidos

**Con Stored Procedure:**
```java
// Java
dsl.fetch("SELECT * FROM sp_actualizar_estado_contratos()");

// SQL (en BD)
CREATE FUNCTION sp_actualizar_estado_contratos() RETURNS TABLE (...) AS
BEGIN
  UPDATE contratos SET estado = 'VENCIDO' WHERE estado = 'VIGENTE' AND fecha_fin < CURRENT_DATE;
  RETURN QUERY SELECT ...;
END;
```

**Con Native Query:**
```java
// Java - Todo en la aplicación
String sql = """
    UPDATE contratos
    SET estado = 'VENCIDO', fecha_modificacion = CURRENT_TIMESTAMP
    WHERE estado = 'VIGENTE' AND fecha_fin < CURRENT_DATE
    """;
int actualizados = dsl.execute(sql);
```

---

## 📊 Estadísticas de la POC

| Métrica | Cantidad |
|---------|----------|
| **Stored Procedures** | 3 |
| **Native Queries** | 27 |
| **Controllers** | 5 |
| **Services** | 3 |
| **Repositories** | 6 |
| **DTOs** | 7 |
| **Endpoints REST** | 30+ |
| **Migraciones Flyway** | 2 |
| **Líneas de código** | ~3000+ |

---

## 🚀 Quick Start

### 1. Primero, entiende la arquitectura
```
Abre: HELP_ARQUITECTURA.md
Lee: Secciones de "Arquitectura general" y "Patrón Controller"
Tiempo: 10 minutos
```

### 2. Luego, prueba Stored Procedures
```
Abre: HELP_STORED_PROCEDURES.md
Ejecuta: Los 3 curls de los Tests
Tiempo: 5 minutos
```

### 3. Luego, prueba Native Queries
```
Abre: HELP_NATIVE_QUERIES.md
Ejecuta: Curls de cada categoría (27 total)
Tiempo: 20 minutos
```

### 4. Finalmente, experimenta
```
Modifica queries en NativeQueriesRepository.java
Ejecuta nuevamente
Ve los cambios en tiempo real
```

---

## 🔧 Herramientas necesarias

Para trabajar con esta POC necesitas:

| Herramienta | Versión | Uso |
|-------------|---------|-----|
| Java | 21+ | Compilar código |
| Maven | 3.9+ | Build tool |
| PostgreSQL | 16.10 | Base de datos |
| Spring Boot | 3.5.6 | Framework |
| jOOQ | 3.19.0 | ORM/SQL builder |
| Flyway | 11.7.2 | Migraciones BD |
| cURL | Cualquiera | Probar endpoints |
| jq | Opcional | Formatear JSON |

---

## 📋 Checklist para nuevo desarrollador

- [ ] Leí HELP_ARQUITECTURA.md completamente
- [ ] Entiendo el patrón Controller → Service → Repository
- [ ] Ejecuté los 3 curls de Stored Procedures
- [ ] Ejecuté los 27 curls de Native Queries
- [ ] Ví los logs de SQL ejecutado en consola
- [ ] Modifiqué una query y ví el cambio
- [ ] Entiendo dónde va la lógica de negocio
- [ ] Sé dónde buscar cuando algo rompe
- [ ] Entiendo la diferencia SP vs NQ
- [ ] Puedo crear un nuevo endpoint

---

## ⚠️ Errores comunes por principiante

| Error | Causa | Solución |
|-------|-------|----------|
| "Required type List<Map>, Provided Result" | Olvidas `.intoMaps()` | Lee HELP_NATIVE_QUERIES - Error 1 |
| "NULL pointer exception" | Accedes a campo null | Valida antes de acceder |
| "SQL syntax error" | Usas función MySQL en Postgres | Lee HELP_NATIVE_QUERIES - Error 3 |
| "SQL injection" | Concatenas valores | Usa bind parameters `?` |
| "404 Not Found" | Ruta mal escrita | Verifica @GetMapping |
| "400 Bad Request" | Validación fallida | Agrega @Valid en DTO |

---

## 🔍 Guía de debugging

### Quiero ver qué SQL se ejecuta

**Paso 1:** Abre `application.yaml`
```yaml
logging:
  level:
    org.jooq: DEBUG
```

**Paso 2:** Ejecuta tu request
```bash
curl http://localhost:8080/api/native-queries/contratos/vigentes
```

**Paso 3:** Mira la consola
```
[jooq.tools.LoggerListener] Executing query: SELECT id, numero, estado FROM contratos WHERE estado = ?
[jooq.tools.LoggerListener] -> with bind values: SELECT id, numero, estado FROM contratos WHERE estado = 'VIGENTE'
```

### Quiero entender por qué falla una query

**Paso 1:** Copia el SQL del log
**Paso 2:** Abre pgAdmin o psql
**Paso 3:** Pega y ejecuta la query
**Paso 4:** Ves el error exacto de PostgreSQL
**Paso 5:** Googlea el error de Postgres

---

## 📚 Referencias externas

### jOOQ
- Documentación oficial: https://www.jooq.org/doc/latest/manual/
- SQL Building: https://www.jooq.org/doc/latest/manual/sql-building/
- Best Practices: https://www.jooq.org/doc/latest/manual/best-practices/

### PostgreSQL
- Documentación: https://www.postgresql.org/docs/16/
- Functions: https://www.postgresql.org/docs/16/functions.html
- Window Functions: https://www.postgresql.org/docs/16/functions-window.html

### Spring Boot
- Documentación: https://spring.io/projects/spring-boot
- REST Controllers: https://spring.io/guides/gs/rest-service/
- Data Access: https://spring.io/guides/gs/accessing-data-jpa/

### Flyway
- Documentación: https://flywaydb.org/documentation/
- Getting Started: https://flywaydb.org/getstarted/firststeps/start

---

## 🤝 Contribuir a la POC

Para agregar nuevas queries o procedures:

1. **Native Queries:** Agrega método a `NativeQueriesRepository.java`
2. **Stored Procedure:** Crea V3__New_Procedure.sql en `db/migration/`
3. **Controller:** Agrega endpoint en el controller correspon diente
4. **Test:** Crea curl en HELP_NATIVE_QUERIES.md o HELP_STORED_PROCEDURES.md

---

## 📞 Soporte

**Cuando no entiendas algo:**
1. Busca la sección en el HELP correspondiente
2. Mira los ejemplos de código
3. Ejecuta los curls para ver cómo funciona
4. Debuguea con logs (org.jooq: DEBUG)
5. Mira la BD directamente (pgAdmin)

**Si aún tienes dudas:**
- Lee HELP_ARQUITECTURA.md nuevamente
- Busca la sección "Errores comunes"
- Copia/pega el error exacto de la consola
- Busca en Google el error de PostgreSQL o jOOQ

---

## 🎓 Resumen

Esta POC demuestra:

✅ **3 patrones con jOOQ:**
- Stored Procedures (BD-centric)
- Native Queries (App-centric SQL)
- DSL Builder (Type-safe)

✅ **Arquitectura limpia:**
- Controller → Service → Repository
- DTOs y Mappers
- Manejo centralizado de excepciones

✅ **Operaciones complejas:**
- CTEs (WITH clause)
- Window Functions
- JOINs múltiples
- Paginación
- Análisis y reportes

✅ **30+ endpoints REST**
**3 Stored Procedures**
**27 Native Queries**
**Documentación completa**

---

## 🎯 Próximo paso

👉 **Lee [HELP_ARQUITECTURA.md](HELP_ARQUITECTURA.md)**

Luego vuelve aquí para decidir si quieres profundizar en SP o NQ.

¡Bienvenido a la POC! 🚀