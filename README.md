# POC 1: JOOQ + SQL Server

## ¿Qué es esta POC?

Esta POC es un ejemplo completo de cómo usar **JOOQ** (Java Object Oriented Querying) con **SQL Server** en Spring Boot.

JOOQ es una librería que proporciona **queries SQL type-safe** en Java, es decir, tus queries se validan en **tiempo de compilación**, no en runtime.

## ¿Por qué JOOQ?

En un sistema legacy se tiene:
- Queries SQL complejas (joins, subconsultas, históricos)
- Necesidad de validaciones en tiempo de compilación
- Control fino sobre el SQL generado
- Rendimiento crítico en consultas masivas

JOOQ es perfecto para esto porque:
- **Type-safe**: errores en compile-time, no en producción
- **SQL explícito**: vez exactamente qué query se ejecuta
- **Flexible**: soporta cualquier SQL que escribas
- **Performance**: sin overhead de ORM (Hibernate)

## Requisitos

```
- Docker y Docker Compose
- Java 21
- Maven 3.8+
- IDE (IntelliJ IDEA recomendado)
```

## Cómo levantar la POC

### Paso 1: Clonar/descargar el código

```bash
cd poc-01-jooq
```

### Paso 2: Levantar SQL Server

```bash
docker-compose up -d
```

Esto levanta:
- **SQL Server** en `localhost:1433` (usuario: `sa`, password: `YourStrong!Passw0rd`)
- **Adminer** (opcional, para debugging en `http://localhost:8080`)

**Esperar 30-40 segundos a que SQL Server esté listo.**

### Paso 3: Ejecutar la aplicación

```bash
mvn spring-boot:run
```

O desde tu IDE: `Poc01JooqApplication.java` → Run.

Verás en los logs que **Flyway** está creando las tablas automáticamente.

### Paso 4: Verificar que funciona

```bash
curl http://localhost:8080/api/contratos/health
```

Respuesta esperada: `POC 1 - JOOQ is UP`

---

## Ejemplos de Uso

### 1. Crear un contrato

```bash
curl -X POST http://localhost:8080/api/contratos \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "CONT-2025-TEST-001",
    "clienteId": 1,
    "instalacionId": 1,
    "tipoContrato": "MANTENIMIENTO",
    "fechaInicio": "2025-01-01",
    "fechaFin": "2025-12-31",
    "precioAnual": 2000.00,
    "porcentajeCentral": 70,
    "coberturaMaterial": true,
    "coberturaManoObra": true,
    "coberturaFinSemana": false
  }'
```

Respuesta: `201 CREATED` + datos del contrato creado.

### 2. Obtener contrato por ID

```bash
curl http://localhost:8080/api/contratos/1
```

### 3. Listar todos los contratos activos

```bash
curl http://localhost:8080/api/contratos
```

### 4. Listar contratos de un cliente

```bash
curl http://localhost:8080/api/contratos/cliente/1
```

### 5. Búsqueda avanzada (con filtros)

```bash
curl "http://localhost:8080/api/contratos?clienteId=1&tipoContrato=MANTENIMIENTO&estado=VIGENTE"
```

### 6. Obtener contratos próximos a vencer (próximos 30 días)

```bash
curl http://localhost:8080/api/contratos/proximos-a-vencer
```

### 7. Obtener contratos vencidos

```bash
curl http://localhost:8080/api/contratos/vencidos
```

---

## Conceptos Clave de JOOQ

### 1. Type-Safe Queries

**Sin JOOQ (tradicional):**
```java
String sql = "SELECT * FROM contratos WHERE cliente_id = " + clienteId;
// ❌ String concatenation: vulnerable a SQL injection
// ❌ Si cambias nombre columna en BD, no se entera en compile-time
```

**Con JOOQ:**
```java
dsl.select()
   .from(table(name(SCHEMA, "contratos")))
   .where(field(name(SCHEMA, "contratos", "cliente_id")).eq(clienteId))
   .fetch();
// ✅ Type-safe: errores en compile-time
// ✅ Si cambias nombre columna, no compila
```

### 2. Queries Complejas (Joins, Filtros Dinámicos)

Mira `ContratoJooqRepository.findProximosAVencer()`:

```java
// Contratos vigentes cuya fecha_fin está entre hoy y +30 días
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
```

Es legible, type-safe, y puedes verlo traducido a SQL exacto.

### 3. Mapping Manual (Record → Object)

```java
private Contrato recordToContrato(Record record) {
    return Contrato.builder()
        .id(record.get("id", Long.class))
        .numero(record.get("numero", String.class))
        // ... mapeo de campos
        .build();
}
```

Por qué manual? **Control total**. No hay magic de Hibernate.

---

## Estructura del Código

```
poc-01-jooq/
├── src/main/java/com/entelgy/
│   ├── domain/
│   │   └── model/
│   │       ├── Cliente.java          # Entidad de negocio (POJO)
│   │       ├── Contrato.java
│   │       └── Parte.java
│   ├── application/
│   │   ├── ContratoApplicationService.java  # Orquestación
│   │   ├── dto/
│   │   │   ├── CrearContratoRequest.java
│   │   │   └── ContratoResponse.java
│   │   └── mapper/
│   │       └── ContratoMapper.java      # MapStruct
│   ├── infrastructure/
│   │   └── repository/
│   │       ├── ContratoJooqRepository.java  # JOOQ queries
│   │       └── ClienteJooqRepository.java
│   ├── presentation/
│   │   ├── ContratoController.java      # REST endpoints
│   │   └── GlobalExceptionHandler.java  # Manejo de errores
│   └── Poc01JooqApplication.java        # Main
├── src/main/resources/
│   ├── application.yml                  # Configuración
│   └── db/migration/
│       └── V1__Initial_Schema.sql       # Flyway migration
├── docker-compose.yml
└── pom.xml
```

### Flujo de una Operación

```
HTTP POST /api/contratos
    ↓
ContratoController.crear()
    ↓ (valida formato JSON)
ContratoApplicationService.crear()
    ↓ (orquesta, valida cliente existe)
ContratoMapper.toDomain()
    ↓ (convierte DTO a entidad)
Contrato.esValido()
    ↓ (valida reglas negocio)
ContratoJooqRepository.save()
    ↓ (JOOQ ejecuta INSERT)
SQL Server (persiste datos)
    ↓
ContratoResponse (mapea respuesta)
    ↓
HTTP 201 + JSON
```

---

## Debugging

### Ver las queries que JOOQ genera

En `application.yml`, habilita:

```yaml
logging:
  level:
    org.jooq.tools.LoggerListener: DEBUG
```

Verás en los logs exactamente qué SQL se ejecuta.

### Inspeccionar BD con Adminer

1. Levanta el perfil debug:
```bash
docker-compose --profile debug up -d
```

2. Accede a `http://localhost:8080`
3. Conexión:
    - Servidor: `sqlserver`
    - Usuario: `sa`
    - Contraseña: `YourStrong!Passw0rd`
    - BD: `entelgy_poc`

### Ver logs de Flyway

Los logs muestran qué migraciones se ejecutaron:

```
Flyway: Successfully validated 1 migration
Flyway: Creating Schema History table "dbo"."flyway_schema_history"
Flyway: Current version of schema "dbo": << Empty >>
Flyway: Migrating schema "dbo" to version 1 - Initial Schema
```

---

## Puntos Clave para Aprender

### ✅ Debes entender:

1. **Diferencia entre JPA y JOOQ**
    - JPA: ORM (mapea objetos a tablas)
    - JOOQ: Query builder (SQL type-safe)

2. **Type-safe queries**
    - Errores en compile-time, no runtime
    - Refactorización segura (renombrar columnas)

3. **Records de JOOQ**
    - `Record` es una fila de resultado
    - Mapeas manualmente a tu entidad

4. **Separación de capas**
    - Controller (HTTP)
    - Application Service (orquestación)
    - Domain Model (lógica de negocio)
    - Repository (persistencia)

5. **Flyway**
    - Migraciones de BD versionadas
    - Se ejecutan automáticamente al startup

### ❌ No debes hacer:

1. ❌ Queries con concatenación de strings
2. ❌ Mapear resultado de BD a DTO directamente (usa mapper)
3. ❌ Poner lógica de BD en el controller
4. ❌ Queries sin índices (mira los índices creados en Flyway)

---

## Próximos Pasos

Cuando entiendas esta POC:

1. **Entiende JOOQ code generation** (opcional pero recomendado)
    - En lugar de hardcodear nombres de tabla/columna, JOOQ genera clases basadas en BD
    - Ejecuta: `mvn jooq-codegen:generate`

2. **Agrega más queries complejas**
    - JOINs con múltiples tablas
    - Subconsultas
    - Aggregations (COUNT, SUM, etc.)

3. **Entiende testing**
    - TestContainers para tests con BD real
    - Mockito para tests unitarios

---

## Preguntas Frecuentes

**P: ¿Es JOOQ mejor que JPA?**
R: Depende. JOOQ es mejor para queries complejas y legacy. JPA es mejor para CRUD simple.

**P: ¿Puedo usar JOOQ con Hibernate?**
R: Sí, pero no lo hagas. Elige uno.

**P: ¿Cómo manejo transacciones?**
R: Usa `@Transactional` en Application Service (que ya está hecho).

**P: ¿Dónde va la lógica de negocio?**
R: En `domain/service` (en esta POC no tenemos separado, está en mapper/model, pero debería ir en service).

**P: ¿Puedo usar JOOQ desde CLI?**
R: Sí, todo es Spring, inyecta `DSLContext` donde sea.

---

## Recursos

- [JOOQ Docs](https://www.jooq.org/doc/latest/manual/)
- [SQL Server + JOOQ](https://www.jooq.org/doc/latest/manual/sql-building/queryparts/select-statement/)
- [Spring + JOOQ](https://spring.io/guides/gs/relational-data-access/)
- [Flyway](https://flywaydb.org/documentation/)

---