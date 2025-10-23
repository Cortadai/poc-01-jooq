# POC - jOOQ + PostgreSQL + Spring Boot

[![Java](https://img.shields.io/badge/Java-21+-green)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-green)]()
[![jOOQ](https://img.shields.io/badge/jOOQ-3.19.0-blue)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16.10-blue)]()

Una Proof of Concept completa mostrando patrones avanzados con **jOOQ** en **Spring Boot** con **PostgreSQL**.

---

## 🎯 ¿Qué es esta POC?

Una demostración práctica de cómo usar **jOOQ** de tres formas diferentes:

1. **Stored Procedures** (3) - Lógica compleja en la BD
2. **Native Queries** (27) - SQL directo desde Java
3. **DSL Builder** (30+) - Type-safe queries sin SQL crudo

**Total: 60+ endpoints REST + 3 procedures SQL + documentación completa**

---

## 🚀 Quick Start

### 1. Requisitos previos

```bash
# Verificar versiones instaladas
java -version           # Debe ser 21+
mvn --version          # Debe ser 3.9+
docker --version       # PostgreSQL en contenedor
```

### 2. Levanta PostgreSQL

```bash
docker-compose up -d
```

Asegurate de conectarte y crear la base de datos correcta antes de arrancar la app.

### 3. Compila y ejecuta

```bash
# Clean build
mvn clean compile

# Ejecuta la app
mvn spring-boot:run

# La app inicia en http://localhost:8080
```

### 4. Verifica que funciona

```bash
# GET - Simple
curl http://localhost:8080/api/contratos/health
```

✅ **¡Listo!** La POC está funcionando.

---

## 📚 Documentación

Esta POC incluye 4 archivos de ayuda completos:

### 📖 [HELP_INDICE.md](HELP_INDICE.md) - **EMPIEZA AQUÍ**
Índice maestro con mapa de rutas. Sabrás dónde buscar cualquier cosa.

### 🏗️ [HELP_ARQUITECTURA.md](HELP_ARQUITECTURA.md) - Estructura del proyecto
Entiende cómo está organizado todo:
- Capas (Presentation, Application, Infrastructure)
- Patrón Controller → Service → Repository
- Flujo de una request HTTP
- DTOs, Mappers, Exception Handling

**Ideal para:** Entender la estructura general

### 🗄️ [HELP_STORED_PROCEDURES.md](HELP_STORED_PROCEDURES.md) - Los 3 Procedures
Cómo funcionan los Stored Procedures:
- `sp_actualizar_estado_contratos()` - Actualiza contratos vencidos
- `sp_generar_reporte_cartera()` - Análisis de cartera
- `sp_crear_parte_trabajo()` - Crea partes de trabajo

**Incluye:** Curls para probar + errores comunes

### 🔍 [HELP_NATIVE_QUERIES.md](HELP_NATIVE_QUERIES.md) - Las 27 Queries
Todas las Native Queries agrupadas por categoría:
1. Queries simples (3)
2. Queries complejas (4)
3. Filtros (4)
4. CTEs y Window Functions (3)
5. Análisis y clasificación (4)
6. Paginación (3)
7. Reporting (2)
8. CRUD (3)

**Incluye:** Curls para probar cada una + SQL ejecutado

---

## 🗂️ Estructura del proyecto

```
poc-01-jooq/
├── src/
│   ├── main/
│   │   ├── java/com/entelgy/
│   │   │   ├── presentation/          ← Controllers REST
│   │   │   │   ├── ProcedureController.java
│   │   │   │   ├── NativeQueriesController.java
│   │   │   │   ├── ContratoController.java
│   │   │   │   ├── JoinExamplesController.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── application/           ← Lógica de negocio
│   │   │   │   ├── ProcedureApplicationService.java
│   │   │   │   ├── ContratoApplicationService.java
│   │   │   │   ├── JoinExamplesApplicationService.java
│   │   │   │   ├── dto/               ← Request/Response DTOs
│   │   │   │   └── mapper/            ← Conversiones Entity ↔ DTO
│   │   │   ├── infrastructure/        ← Acceso a datos
│   │   │   │   ├── repository/
│   │   │   │   │   ├── ProcedureRepository.java
│   │   │   │   │   ├── NativeQueriesRepository.java
│   │   │   │   │   ├── ContratoJooqRepository.java
│   │   │   │   │   └── ... (5 repositorios más)
│   │   │   │   └── exception/
│   │   │   ├── jooq/generated/        ← jOOQ generado (no editar)
│   │   │   └── Poc01JooqApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/migration/          ← Flyway migrations
│   │           ├── V1__Initial_Schema.sql
│   │           └── V2__Stored_Procedures.sql
│   └── test/                          ← Tests unitarios e integración
│
├── pom.xml                            ← Dependencias Maven
├── docker-compose.yaml                ← PostgreSQL en Docker
└── README.md                          ← Este archivo
```

---

## 🔌 Endpoints principales

### Stored Procedures (3)

```bash
GET  /api/procedures/actualizar-vencidos      # Actualiza contratos vencidos
GET  /api/procedures/reporte-cartera          # Reporte de cartera
POST /api/procedures/crear-parte              # Crea parte de trabajo
```

### Native Queries (27)

```bash
# Queries simples
GET  /api/native-queries/contratos/vigentes
GET  /api/native-queries/contratos/{id}
GET  /api/native-queries/contratos/contar/vigentes

# Queries complejas
GET  /api/native-queries/analisis/cartera
GET  /api/native-queries/top-clientes?limit=5
GET  /api/native-queries/contratos/proximos-vencer?dias=30

# Análisis y reportes
GET  /api/native-queries/contratos/riesgo
GET  /api/native-queries/dashboard/metricas
GET  /api/native-queries/reporte/mensual

# Paginación
GET  /api/native-queries/contratos/paginar?page=1&size=10

# CRUD
POST   /api/native-queries/contratos/insertar
PUT    /api/native-queries/contratos/actualizar-estado
DELETE /api/native-queries/contratos/eliminar-antiguos

# ... y 11 endpoints más
```

### Contrato Controller (5)

```bash
POST   /api/contratos                  # Crear
GET    /api/contratos                  # Listar
GET    /api/contratos/{id}             # Obtener uno
PUT    /api/contratos/{id}             # Actualizar
DELETE /api/contratos/{id}             # Eliminar
```

---

## 🧪 Probar rápidamente

### Opción 1: Curls simples

```bash
# Test 1: Actualizar contratos vencidos
curl http://localhost:8080/api/procedures/actualizar-vencidos | jq .

# Test 2: Obtener contratos vigentes
curl http://localhost:8080/api/native-queries/contratos/vigentes | jq .

# Test 3: Dashboard - Métricas
curl http://localhost:8080/api/native-queries/dashboard/metricas | jq .
```
### Opción 2: Postman/Insomnia

Crea requests basándote en los endpoints.

---

## 💡 Conceptos clave

### 1. Stored Procedures vs Native Queries

**Stored Procedures (BD-centric):**
- Lógica compleja en SQL
- Reutilizable entre apps
- Mejor para operaciones críticas
- Ejemplo: `sp_actualizar_estado_contratos()`

**Native Queries (App-centric):**
- SQL directo desde Java
- Flexible y fácil de debuguear
- Mejor para reportes/análisis
- Ejemplo: `SELECT * FROM contratos WHERE estado = ?`

### 2. Patrón Controller → Service → Repository

```
HTTP Request
    ↓
[Controller]        ← Valida HTTP, mapea entrada
    ↓
[ApplicationService] ← Orquesta lógica, valida reglas
    ↓
[Repository]        ← Ejecuta SQL, retorna datos
    ↓
PostgreSQL BD
    ↓
Response JSON
```

### 3. jOOQ - Type-safe queries

```java
// DSL builder - Type-safe
List<ContratoRecord> records = dsl
    .selectFrom(CONTRATOS)
    .where(CONTRATOS.ESTADO.eq("VIGENTE"))
    .fetch();

// Native queries - Flexible
List<Map<String, Object>> results = dsl
    .fetch("SELECT * FROM contratos WHERE estado = ?", "VIGENTE")
    .intoMaps();
```

---

## 🔧 Configuración

### application.yaml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/poc_jooq
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
    
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQL16Dialect
    hibernate:
      ddl-auto: validate  # NO borra datos - solo valida
      
logging:
  level:
    root: INFO
    com.entelgy: DEBUG          # Ver logs de la app
    org.jooq: DEBUG             # Ver queries SQL
    org.springframework.web: DEBUG
```

---

## ⚠️ Errores comunes

### Error: "relation 'contratos' does not exist"
**Causa:** Flyway no ejecutó las migraciones

**Solución:**
```sql
-- En pgAdmin, ejecuta:
DELETE FROM flyway_schema_history;
-- Luego reinicia la app
```

### Error: "bad SQL grammar"
**Causa:** SQL tiene error de sintaxis

**Solución:**
1. Activa logging: `org.jooq: DEBUG`
2. Copia el SQL del log
3. Ejecuta directamente en pgAdmin
4. Ves el error exacto de Postgres

### Error: "Required type List<Map>, Provided Result<Record>"
**Causa:** Olvidas `.intoMaps()`

**Solución:**
```java
// ❌ MAL
return dsl.fetch(sql);

// ✅ BIEN
return dsl.fetch(sql).intoMaps();
```

---

## 📊 Estadísticas

| Métrica | Cantidad |
|---------|----------|
| Controllers | 5 |
| Services | 3 |
| Repositories | 6 |
| DTOs | 7 |
| Endpoints REST | 30+ |
| Stored Procedures | 3 |
| Native Queries | 27 |
| Tests | 8+ |
| Migraciones Flyway | 2 |
| Líneas de documentación | 2000+ |

---

## 📚 Recursos de ayuda

### En este proyecto
- `HELP_INDICE.md` - Índice con referencias cruzadas
- `HELP_ARQUITECTURA.md` - Arquitectura y patrones
- `HELP_STORED_PROCEDURES.md` - Los 3 procedures
- `HELP_NATIVE_QUERIES.md` - Las 27 queries

### Externo
- [jOOQ Documentation](https://www.jooq.org/doc/latest/manual/)
- [PostgreSQL 16 Manual](https://www.postgresql.org/docs/16/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Flyway Getting Started](https://flywaydb.org/getstarted/firststeps/start)

---

## 🎯 Resumen

Esta POC demuestra:

✅ Tres formas de usar jOOQ:
- Stored Procedures
- Native Queries
- DSL Builder

✅ Arquitectura limpia:
- Controller → Service → Repository
- Separación de capas
- DTOs y Mappers
- Manejo centralizado de excepciones

✅ 60+ ejemplos en código vivo:
- 30+ endpoints REST
- 3 Stored Procedures
- 27 Native Queries
- Tests

✅ Documentación completa:
- 2000+ líneas en HELP_*.md
- Curls para cada endpoint
- Explicación de cada concepto
- Errores comunes y soluciones

---