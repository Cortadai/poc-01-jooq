# 📚 HELP - Stored Procedures con jOOQ

## 🎯 Qué son los Stored Procedures en esta POC

Los Stored Procedures son funciones SQL almacenadas en PostgreSQL que se ejecutan desde Java a través de jOOQ. Se usan para operaciones complejas que es mejor mantener en la BD.

### 3 Procedures implementados:

1. **sp_actualizar_estado_contratos()** - Actualiza contratos vencidos
2. **sp_generar_reporte_cartera()** - Genera análisis de cartera por cliente
3. **sp_crear_parte_trabajo()** - Crea partes de trabajo

---

## 📂 Estructura de archivos

```
src/
├── main/
│   ├── java/com/entelgy/
│   │   ├── presentation/
│   │   │   └── ProcedureController.java          ← Endpoints HTTP
│   │   ├── application/
│   │   │   └── ProcedureApplicationService.java  ← Lógica de negocio
│   │   └── infrastructure/repository/
│   │       └── ProcedureRepository.java          ← Ejecución de procedures
│   └── resources/db/migration/
│       └── V2__Stored_Procedures.sql             ← Definición SQL
```

---

## 🔧 Componentes principales

### 1. ProcedureController.java
**Responsabilidad:** Mapear HTTP a métodos

```java
@RestController
@RequestMapping("/api/procedures")
public class ProcedureController {
    
    @GetMapping("/actualizar-vencidos")
    public ResponseEntity<?> actualizarVencidos() { ... }
    
    @GetMapping("/reporte-cartera")
    public ResponseEntity<List<CarteraReporteDTO>> obtenerReporte() { ... }
    
    @PostMapping("/crear-parte")
    public ResponseEntity<?> crearParte(...) { ... }
}
```

**Tips:**
- ✅ Usa `@RequestParam` para parámetros simples
- ✅ Usa `@Valid` para validar request bodies
- ✅ Retorna `ResponseEntity<>` para control de HTTP status
- ✅ Las excepciones las captura `GlobalExceptionHandler`

---

### 2. ProcedureApplicationService.java
**Responsabilidad:** Orquestar la lógica de negocio

```java
@Service
@RequiredArgsConstructor
public class ProcedureApplicationService {
    
    private final ProcedureRepository repository;
    
    @Transactional
    public void actualizarContratosvencidos() {
        repository.actualizarEstadoContratos();
    }
}
```

**Tips:**
- ✅ Inyecta el repository
- ✅ Usa `@Transactional` para transacciones
- ✅ Mapea resultados a DTOs antes de retornar
- ✅ Agrega logging para debugging

---

### 3. ProcedureRepository.java
**Responsabilidad:** Ejecutar el procedure desde BD

```java
@Repository
public class ProcedureRepository {
    
    private final DSLContext dsl;
    
    public void actualizarEstadoContratos() {
        dsl.fetch("SELECT * FROM sp_actualizar_estado_contratos()")
           .intoMaps();
    }
}
```

**Tips:**
- ✅ Inyecta `DSLContext` para jOOQ
- ✅ Usa `.fetch()` para ejecutar
- ✅ Usa `.intoMaps()` para convertir `Result<Record>` a `List<Map>`
- ✅ El método es el que llama el `ApplicationService`

---

## 💡 Buenas prácticas aplicadas

### 1. Separación de capas
```
HTTP Request
    ↓
[Controller] - Valida entrada HTTP
    ↓
[ApplicationService] - Orquesta lógica
    ↓
[Repository] - Ejecuta SQL/Procedure
    ↓
PostgreSQL Procedure
```

### 2. Manejo de tipos de datos
```java
// Procedure retorna datos tipados
RETURNS TABLE (
    total_actualizados INTEGER,
    fecha_ejecucion TIMESTAMP,
    mensaje TEXT
)

// Java mapea a Map
Map<String, Object> resultado = fetch().intoMaps().get(0);
Integer total = (Integer) resultado.get("total_actualizados");
```

### 3. Error handling
```java
try {
    dsl.fetch("SELECT * FROM sp_...");
} catch (DataAccessException e) {
    log.error("Error ejecutando procedure: {}", e.getMessage());
    throw new RuntimeException("Error en procedure");
}
```

---

## 🧪 CÓMO PROBAR - Curls por endpoint

### Endpoint 1: Actualizar contratos vencidos
**GET** `/api/procedures/actualizar-vencidos`

```bash
curl -X GET "http://localhost:8080/api/procedures/actualizar-vencidos" | jq .
```

**Response esperado:**
```json
{
  "totalActualizados": 3,
  "fechaEjecucion": "2025-10-23T12:35:11.866+00:00",
  "mensaje": "Actualizado 3 contratos",
  "exitoso": true
}
```

**Qué hace:**
- Busca contratos con `estado = 'VIGENTE'` y `fecha_fin < CURRENT_DATE`
- Los cambia a `estado = 'VENCIDO'`
- Retorna cuántos actualizó

**Tips:**
- ✅ Idempotente: puedes ejecutar varias veces
- ✅ Sin parámetros: es simple
- ✅ Bueno para ejecutar programado (Scheduler)

---

### Endpoint 2: Obtener reporte de cartera
**GET** `/api/procedures/reporte-cartera`

```bash
curl -X GET "http://localhost:8080/api/procedures/reporte-cartera" | jq .
```

**Response esperado:**
```json
[
  {
    "clienteId": 1,
    "clienteNombre": "Cliente A - Empresa X",
    "totalContratos": 3,
    "contratosVigentes": 1,
    "contratosVencidos": 2,
    "volumenTotal": 40000.00,
    "volumenVigentes": 12000.00,
    "estadoCartera": "ALERTA",
    "diasParaVencer": 69
  },
  {
    "clienteId": 2,
    "clienteNombre": "Cliente B - Empresa Y",
    "totalContratos": 1,
    "contratosVigentes": 0,
    "contratosVencidos": 1,
    "volumenTotal": 15000.00,
    "volumenVigentes": 0,
    "estadoCartera": "SIN_VIGENTES",
    "diasParaVencer": null
  }
]
```

**Qué hace:**
- Analiza cartera por cliente
- Cuenta contratos por estado
- Suma volúmenes
- Clasifica riesgo (NORMAL, ALERTA, CRÍTICO, etc)

**Tips:**
- ✅ Usar para dashboards
- ✅ Información completa en una sola llamada
- ✅ Groupby cliente_id

---

### Endpoint 3: Crear parte de trabajo
**POST** `/api/procedures/crear-parte`

```bash
curl -X POST "http://localhost:8080/api/procedures/crear-parte?contratoId=1&descripcion=Test&tipoTrabajo=Revision"
```

O mejor (sin tildes en URL):
```bash
curl -X POST "http://localhost:8080/api/procedures/crear-parte?contratoId=1&descripcion=Revisi%C3%B3n&tipoTrabajo=Mantenimiento"
```

**Parámetros:**
| Parámetro | Tipo | Requerido | Ejemplo |
|-----------|------|-----------|---------|
| `contratoId` | Integer | ✅ | 1 |
| `descripcion` | String | ✅ | "Revisión sistema" |
| `tipoTrabajo` | String | ✅ | "Mantenimiento" |

**Response esperado:**
```json
{
  "parteId": 4,
  "numero": "PARTE-20251023-00001",
  "estado": "OK",
  "mensaje": "Parte PARTE-20251023-00001 creado",
  "codigoError": 0,
  "exitoso": true
}
```

**Qué hace:**
- Valida que contrato exista
- Valida que contrato esté VIGENTE
- Genera número único: PARTE-YYYYMMDD-00001
- Inserta en tabla `partes`
- Retorna ID del parte creado

**Tips:**
- ✅ Validaciones en el procedure mismo (contrato debe existir y estar VIGENTE)
- ✅ Número auto-generado con secuencia diaria
- ✅ Si falla validación, retorna código de error

---

## ⚠️ Errores comunes y cómo evitarlos

### Error 1: "structure of query does not match function result type"
**Causa:** Tipos de datos no coinciden (TIMESTAMP vs TIMESTAMP WITH TIME ZONE)

**Solución:**
```sql
-- ❌ MAL
SELECT CURRENT_TIMESTAMP  -- retorna TIMESTAMP WITH TIME ZONE

-- ✅ BIEN
SELECT CURRENT_TIMESTAMP::TIMESTAMP  -- castea a TIMESTAMP
```

### Error 2: "column reference 'estado' is ambiguous"
**Causa:** Hay ambigüedad entre variable PL/pgSQL y columna tabla

**Solución:**
```sql
-- ❌ MAL
IF (SELECT estado FROM contratos WHERE id = p_contrato_id) != 'VIGENTE'

-- ✅ BIEN
IF (SELECT contratos.estado FROM contratos WHERE contratos.id = p_contrato_id) != 'VIGENTE'
```

### Error 3: "null value in column violates not-null constraint"
**Causa:** Faltan columnas obligatorias en INSERT

**Solución:**
```sql
-- ✅ BIEN - incluye todas las columnas NOT NULL
INSERT INTO contratos (cliente_id, numero, estado, precio_anual, 
                       tipo_contrato, fecha_inicio, fecha_fin, fecha_creacion)
VALUES (?, ?, ?, ?, 'Estándar', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 year', CURRENT_TIMESTAMP)
```

### Error 4: "function year(date) does not exist"
**Causa:** PostgreSQL no tiene función `YEAR()` (es de MySQL)

**Solución:**
```sql
-- ❌ MAL
WHERE YEAR(fecha_inicio) = YEAR(CURRENT_DATE)

-- ✅ BIEN
WHERE EXTRACT(YEAR FROM fecha_inicio) = EXTRACT(YEAR FROM CURRENT_DATE)
```

---

## 📊 Diagrama de flujo

```
Cliente HTTP
    │
    ├─ GET /api/procedures/actualizar-vencidos
    ├─ GET /api/procedures/reporte-cartera
    └─ POST /api/procedures/crear-parte
           │
           ▼
    [ProcedureController] - @RestController
           │ Valida @RequestParam, @Valid
           │
           ▼
    [ProcedureApplicationService] - @Service @Transactional
           │ Orquesta lógica, mapea DTOs
           │
           ▼
    [ProcedureRepository] - @Repository
           │ Ejecuta: dsl.fetch("SELECT * FROM sp_...")
           │
           ▼
    PostgreSQL Procedure
           │ Valida reglas de negocio
           │ Ejecuta operaciones complejas
           │
           ▼
    Response JSON
```

---

## 🔍 Debugging - Ver SQL ejecutado

En `application.yaml`, agrega:

```yaml
logging:
  level:
    org.jooq: DEBUG
```

Verás en consola:
```
[jooq.tools.LoggerListener] Executing query : SELECT * FROM sp_actualizar_estado_contratos()
[jooq.tools.LoggerListener] -> with bind values : SELECT * FROM sp_actualizar_estado_contratos()
```

---

## 📝 Resumen

| Aspecto | Detalle |
|--------|---------|
| **Procedures en la POC** | 3 (actualizar, reporte, crear-parte) |
| **Endpoints** | 3 (/actualizar-vencidos, /reporte-cartera, /crear-parte) |
| **Patrón usado** | Controller → Service → Repository |
| **ORM** | jOOQ con native SQL |
| **BD** | PostgreSQL 16.10 |
| **Validaciones** | En procedure + Java |
| **Transacciones** | @Transactional en service |
| **DTOs** | CarteraReporteDTO, ParteTrabajoCreadoDTO |

---

## 🎓 Próximos pasos

1. ✅ Ejecutar los 3 endpoints con curls
2. ✅ Ver logs de SQL ejecutado
3. ✅ Entender el flujo Controller → Service → Repository
4. ✅ Modificar un procedure y ver cambios
5. ✅ Crear un nuevo procedure si lo necesitas