# 📚 HELP - Arquitectura y Controllers

## 🎯 Arquitectura general de la POC

La POC sigue arquitectura de **capas limpias** (Clean Architecture):

```
┌─────────────────────────────────────────────────────┐
│ 1. PRESENTATION - Controladores REST               │
│    ├── ProcedureController.java                    │
│    ├── NativeQueriesController.java                │
│    ├── ContratoController.java                     │
│    ├── JoinExamplesController.java                 │
│    └── GlobalExceptionHandler.java                 │
└─────────────────────────────────────────────────────┘
                        ↑
        [Valida HTTP, parsea, mapea]
                        ↓
┌─────────────────────────────────────────────────────┐
│ 2. APPLICATION - Servicios de negocio              │
│    ├── ProcedureApplicationService.java            │
│    ├── ContratoApplicationService.java             │
│    ├── JoinExamplesApplicationService.java         │
│    ├── dto/                                        │
│    │   ├── CrearContratoRequest.java              │
│    │   ├── ContratoResponse.java                  │
│    │   └── CarteraReporteDTO.java                 │
│    └── mapper/                                     │
│        └── ContratoMapper.java                     │
└─────────────────────────────────────────────────────┘
                        ↑
        [Orquesta lógica, valida reglas]
                        ↓
┌─────────────────────────────────────────────────────┐
│ 3. INFRASTRUCTURE - Acceso a datos                  │
│    ├── repository/                                 │
│    │   ├── ProcedureRepository.java               │
│    │   ├── NativeQueriesRepository.java           │
│    │   ├── ContratoJooqRepository.java            │
│    │   ├── JoinExamplesRepository.java            │
│    │   └── ParteJooqRepository.java               │
│    └── exception/                                  │
│        └── DuplicateEntryException.java           │
└─────────────────────────────────────────────────────┘
                        ↑
        [Ejecuta SQL, mapea a Java]
                        ↓
┌─────────────────────────────────────────────────────┐
│ 4. PERSISTENCE - Base de datos                      │
│    ├── PostgreSQL 16.10                           │
│    ├── jOOQ (generado)                            │
│    ├── Flyway migrations                          │
│    └── Stored Procedures SQL                      │
└─────────────────────────────────────────────────────┘
```

---

## 📂 Componentes existentes

### Tier 1: PRESENTATION

```
ProcedureController
├── GET /api/procedures/actualizar-vencidos
├── GET /api/procedures/reporte-cartera
└── POST /api/procedures/crear-parte

NativeQueriesController
├── GET /api/native-queries/contratos/vigentes
├── GET /api/native-queries/contratos/{id}
├── GET /api/native-queries/analisis/cartera
└── ... (24 más)

ContratoController
├── POST /api/contratos
├── GET /api/contratos/{id}
├── GET /api/contratos
├── PUT /api/contratos/{id}
└── DELETE /api/contratos/{id}

JoinExamplesController
├── GET /api/join-examples/contratos-clientes
└── GET /api/join-examples/contratos-activos

GlobalExceptionHandler
├── @ExceptionHandler(DataAccessException.class)
├── @ExceptionHandler(DuplicateEntryException.class)
└── @ExceptionHandler(Exception.class)
```

### Tier 2: APPLICATION

```
ProcedureApplicationService
├── actualizarContratosvencidos()
├── obtenerReporteCartera()
└── crearParteTrabajo(...)

ContratoApplicationService
├── crear(CrearContratoRequest)
├── obtenerPorId(Integer)
├── obtener()
├── actualizar(Integer, CrearContratoRequest)
└── eliminar(Integer)

JoinExamplesApplicationService
├── obtenerContratosConClientes()
└── obtenerContratosActivos()

DTOs
├── CrearContratoRequest - entrada
├── ContratoResponse - salida
├── CarteraReporteDTO - salida
└── ParteTrabajoCreadoDTO - salida

Mappers
└── ContratoMapper - de Entity a DTO
```

### Tier 3: INFRASTRUCTURE

```
Repositories
├── ProcedureRepository - Stored Procedures
├── NativeQueriesRepository - Native SQL (27 queries)
├── ContratoJooqRepository - jOOQ DSL
├── JoinExamplesRepository - Ejemplos de JOINs
└── ParteJooqRepository - Partes de trabajo

Exceptions
└── DuplicateEntryException - excepciones custom
```

---

## 💡 Flujo de una request HTTP

### Ejemplo: GET /api/procedures/actualizar-vencidos

```
1. HTTP Request llega
   GET /api/procedures/actualizar-vencidos
        ↓
2. Spring mapea a método
   @GetMapping("/actualizar-vencidos")
   public ResponseEntity<?> actualizarVencidos()
        ↓
3. Controller llama a Service
   procedureService.actualizarContratosvencidos()
        ↓
4. Service orquesta
   - Llama repository
   - Valida reglas negocio
   - Mapea resultado a DTO
        ↓
5. Repository ejecuta SQL
   dsl.fetch("SELECT * FROM sp_actualizar_estado_contratos()")
        ↓
6. PostgreSQL retorna datos
   [Query result]
        ↓
7. Repository convierte a Map/DTO
   result.intoMaps()
        ↓
8. Service retorna DTO
   new ActualizacionDTO(...)
        ↓
9. Controller retorna ResponseEntity
   ResponseEntity.ok(resultado)
        ↓
10. Spring serializa a JSON
    {"totalActualizados": 3, "mensaje": "..."}
        ↓
11. HTTP 200 OK con JSON al cliente
```

---

## 🔧 Patrón Controller → Service → Repository

### Controller - Responsabilidades

```java
@RestController
@RequestMapping("/api/procedimientos")
@RequiredArgsConstructor  // Lombok inyecta dependencias
public class ProcedureController {
    
    private final ProcedureApplicationService procedureService;
    
    @GetMapping("/actualizar-vencidos")
    public ResponseEntity<?> actualizarVencidos() {
        // 1. Valida entrada HTTP (implícito en @RequestParam, @Valid)
        // 2. Llama service
        var resultado = procedureService.actualizarContratosvencidos();
        // 3. Retorna ResponseEntity con HTTP status
        return ResponseEntity.ok(resultado);
    }
}
```

**Responsabilidades del Controller:**
- ✅ Mapear rutas HTTP a métodos Java
- ✅ Validar formato de request (@Valid, @RequestParam)
- ✅ Llamar a service
- ✅ Retornar ResponseEntity con status HTTP correcto
- ❌ NO hace lógica de negocio
- ❌ NO accede a BD directamente

### Service - Responsabilidades

```java
@Service
@RequiredArgsConstructor
@Transactional
public class ProcedureApplicationService {
    
    private final ProcedureRepository repository;
    
    public ActualizacionDTO actualizarContratosvencidos() {
        // 1. Valida reglas de negocio
        // 2. Orquesta llamadas a repository
        var resultado = repository.actualizarEstadoContratos();
        // 3. Mapea resultado a DTO
        return new ActualizacionDTO(resultado);
    }
}
```

**Responsabilidades del Service:**
- ✅ Orquestar lógica de negocio
- ✅ Valida reglas de negocio
- ✅ Maneja transacciones (@Transactional)
- ✅ Mapea a DTOs
- ✅ Agrega logging
- ❌ NO accede a BD directamente

### Repository - Responsabilidades

```java
@Repository
public class ProcedureRepository {
    
    private final DSLContext dsl;
    
    public List<Map<String, Object>> actualizarEstadoContratos() {
        // 1. Ejecuta SQL/Procedure
        // 2. Mapea resultado a Map/Object
        // 3. Retorna datos sin lógica
        return dsl.fetch("SELECT * FROM sp_actualizar_estado_contratos()")
                  .intoMaps();
    }
}
```

**Responsabilidades del Repository:**
- ✅ Ejecutar SQL/Procedures
- ✅ Mapear datos BD a Java
- ✅ Convertir Result<Record> a Map/List
- ✅ Manejar errores de BD
- ❌ NO hace lógica de negocio
- ❌ NO retorna DTOs

---

## 📊 Tipos de Controllers existentes

### 1. ProcedureController - Stored Procedures
**Usa:** Procedures SQL complejos
**Retorna:** DTOs con datos del procedure

```bash
GET /api/procedures/actualizar-vencidos
GET /api/procedures/reporte-cartera
POST /api/procedures/crear-parte
```

### 2. NativeQueriesController - SQL Nativo
**Usa:** SQL directo con jOOQ
**Retorna:** Map<String, Object> con flexibilidad

```bash
GET /api/native-queries/contratos/vigentes
GET /api/native-queries/analisis/cartera
POST /api/native-queries/contratos/filtrar
... (24 más)
```

### 3. ContratoController - jOOQ DSL
**Usa:** DSL builder type-safe
**Retorna:** DTOs tipados

```bash
POST /api/contratos
GET /api/contratos/{id}
GET /api/contratos
PUT /api/contratos/{id}
DELETE /api/contratos/{id}
```

### 4. JoinExamplesController - Ejemplos de JOINs
**Usa:** Ejemplos educativos de diferentes JOINs
**Retorna:** Datos de ejemplo

```bash
GET /api/join-examples/contratos-clientes
GET /api/join-examples/contratos-activos
```

---

## ⚙️ GlobalExceptionHandler - Manejo centralizado

```java
@RestControllerAdvice  // Intercepta excepciones de todos los controllers
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e) {
        // Maneja errores de BD
        return ResponseEntity.status(500).body(
            new ErrorResponse("Error en BD", e.getMessage())
        );
    }
    
    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEntryException e) {
        // Maneja duplicados
        return ResponseEntity.status(409).body(
            new ErrorResponse("Registro duplicado", e.getMessage())
        );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        // Maneja cualquier otra excepción
        return ResponseEntity.status(500).body(
            new ErrorResponse("Error interno", e.getMessage())
        );
    }
}
```

**Ventajas:**
- ✅ Manejo centralizado de excepciones
- ✅ Respuestas consistentes
- ✅ Logs automáticos
- ✅ HTTP status codes apropiados

---

## 🔄 DTOs - Mapeo de datos

### Request DTOs (entrada)

```java
@Data
@Valid  // JSR-303 validation
public class CrearContratoRequest {
    @NotNull(message = "clienteId es requerido")
    private Integer clienteId;
    
    @NotBlank(message = "número no puede estar vacío")
    private String numero;
    
    @NotNull
    @Positive(message = "precio debe ser positivo")
    private BigDecimal precioAnual;
    
    // getters/setters auto-generados por Lombok
}
```

### Response DTOs (salida)

```java
@Data
@Builder  // Lombok - patrón Builder
public class ContratoResponse {
    private Integer id;
    private String numero;
    private String estado;
    private BigDecimal precioAnual;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
```

### Mapper - Conversión

```java
@Component
public class ContratoMapper {
    
    // Entity/Record a DTO
    public ContratoResponse toDTO(ContratosRecord record) {
        return ContratoResponse.builder()
            .id(record.getId())
            .numero(record.getNumero())
            .estado(record.getEstado())
            .precioAnual(record.getPrecioAnual())
            .build();
    }
    
    // Request a Entity
    public ContratosRecord toEntity(CrearContratoRequest request) {
        ContratosRecord record = new ContratosRecord();
        record.setClienteId(request.getClienteId());
        record.setNumero(request.getNumero());
        return record;
    }
}
```

---

## 💡 Buenas prácticas aplicadas

### 1. Inyección de dependencias
```java
// ✅ BIEN - Constructor injection con Lombok @RequiredArgsConstructor
@Service
@RequiredArgsConstructor
public class ContratoApplicationService {
    private final ContratoJooqRepository repository;
    private final ContratoMapper mapper;
}

// ❌ MAL - Field injection
@Service
public class ContratoApplicationService {
    @Autowired
    private ContratoJooqRepository repository;
}
```

### 2. Validación en entrada
```java
// ✅ BIEN - @Valid + JSR-303
@PostMapping
public ResponseEntity<ContratoResponse> crear(
    @Valid @RequestBody CrearContratoRequest request
) { ... }

// ❌ MAL - Sin validación
@PostMapping
public ResponseEntity<ContratoResponse> crear(
    @RequestBody CrearContratoRequest request
) { ... }
```

### 3. Transacciones
```java
// ✅ BIEN - Nivel de servicio
@Service
public class ContratoApplicationService {
    @Transactional
    public ContratoResponse crear(CrearContratoRequest request) { ... }
}

// ❌ MAL - Nivel de controller
@RestController
public class ContratoController {
    @Transactional
    @PostMapping
    public ResponseEntity<ContratoResponse> crear(...) { ... }
}
```

### 4. Logging
```java
// ✅ BIEN - Con Lombok @Slf4j
@Slf4j
@RestController
public class ContratoController {
    @PostMapping
    public ResponseEntity<ContratoResponse> crear(...) {
        log.info("POST /api/contratos - Creando contrato {}", request.getNumero());
        // ...
        log.error("Error creando contrato", exception);
    }
}
```

### 5. HTTP Status codes
```java
// ✅ BIEN - Status codes apropiados
@PostMapping
public ResponseEntity<ContratoResponse> crear(...) {
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

@GetMapping("/{id}")
public ResponseEntity<ContratoResponse> obtenerPorId(Integer id) {
    return resultado.isPresent()
        ? ResponseEntity.ok(resultado.get())          // 200 OK
        : ResponseEntity.notFound().build();           // 404 NOT FOUND
}

@DeleteMapping("/{id}")
public ResponseEntity<?> eliminar(Integer id) {
    return ResponseEntity.noContent().build();         // 204 NO CONTENT
}
```

---

## 📝 Resumen de componentes

| Componente | Tipo | Responsabilidad | Ejemplos |
|-----------|------|-----------------|----------|
| Controller | REST | Mapear HTTP | ProcedureController |
| Service | Negocio | Orquestar lógica | ProcedureApplicationService |
| Repository | Datos | Ejecutar SQL | ProcedureRepository |
| DTO | Mapeo | Request/Response | CrearContratoRequest |
| Mapper | Conversion | Entity ↔ DTO | ContratoMapper |
| Exception Handler | Global | Manejo centralizado | GlobalExceptionHandler |

---

## 🎓 Flujo completo de ejemplo

### Crear un contrato

```
1. Cliente hace HTTP
   POST /api/contratos
   Body: {"clienteId": 1, "numero": "CONT-X", "precioAnual": 12000}

2. Controller recibe
   @PostMapping
   public ResponseEntity<ContratoResponse> crear(
       @Valid @RequestBody CrearContratoRequest request
   ) {
       ContratoResponse response = contratoService.crear(request);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
   }

3. Service procesa
   @Transactional
   public ContratoResponse crear(CrearContratoRequest request) {
       // Valida reglas negocio
       if (clienteNoExiste(request.getClienteId())) {
           throw new IllegalArgumentException("Cliente no existe");
       }
       // Delega a repository
       ContratosRecord record = repository.insertar(...);
       // Mapea a DTO
       return mapper.toDTO(record);
   }

4. Repository ejecuta
   public ContratosRecord insertar(CrearContratoRequest request) {
       ContratosRecord record = new ContratosRecord();
       record.setClienteId(request.getClienteId());
       record.setNumero(request.getNumero());
       // ... más campos
       record.insert();  // Ejecuta INSERT en BD
       return record;
   }

5. BD retorna datos
   INSERT INTO contratos (...)
   RETURNING *

6. Response vuelve
   ← ContratoResponse (200 CREATED)

7. Cliente recibe JSON
   {"id": 28, "numero": "CONT-X", "estado": "VIGENTE", ...}
```

---

## 🔍 Debugging

### Ver logs de Spring
```yaml
logging:
  level:
    root: INFO
    com.entelgy: DEBUG
    org.springframework.web: DEBUG
    org.jooq: DEBUG
```

### Entrada en consola
```
[ContratoController] POST /api/contratos - Creando contrato CONT-X
[ContratoApplicationService] Validando cliente 1
[ContratoJooqRepository] INSERT INTO contratos (...) VALUES (...)
[jooq] Executing query: INSERT INTO contratos ...
[ContratoApplicationService] Contrato creado con ID 28
[ContratoController] Retornando HTTP 201 CREATED
```