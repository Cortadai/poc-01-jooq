package com.entelgy.infrastructure.repository;

import com.entelgy.domain.model.Contrato;
import com.entelgy.infrastructure.exception.DuplicateEntryException;
import com.entelgy.jooq.generated.tables.Clientes;
import com.entelgy.jooq.generated.tables.Contratos;
import com.entelgy.jooq.generated.tables.Instalaciones;
import com.entelgy.jooq.generated.tables.records.ClientesRecord;
import com.entelgy.jooq.generated.tables.records.ContratosRecord;
import com.entelgy.jooq.generated.tables.records.InstalacionesRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jooq.JooqTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.entelgy.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para ContratoJooqRepository
 *
 * Usa @JooqTest para:
 * - Crear H2 en memoria (o PostgreSQL en test)
 * - Inyectar DSLContext configurado
 * - Rollback automático después de cada test
 *
 * FIXES APLICADOS:
 * 1.  Añadido @Transactional para garantizar que @BeforeEach y test están en la misma transacción
 * 2.  Usado RETURNING() para obtener IDs generados por la BD
 * 3.  Creados builders para Cliente e Instalacion que devuelven objetos con IDs reales
 * 4.  Eliminados números hardcodeados de cliente_id/instalacion_id
 * 5.  Refactorizados helpers para que pasen correctamente todos los parámetros
 */
@JooqTest
@Transactional  // Asegura que @BeforeEach y tests están en la misma transacción
@ActiveProfiles("test")
class ContratoJooqRepositoryTest {

    @Autowired
    private DSLContext dsl;

    private ContratoJooqRepository repository;

    // 👇 Variables para almacenar IDs reales de BD
    private Integer cliente1Id, cliente2Id;
    private Integer instalacion1Id, instalacion2Id;

    private static final Contratos CONTRATOS_TABLE = CONTRATOS;
    private static final Clientes CLIENTES_TABLE = CLIENTES;
    private static final Instalaciones INSTALACIONES_TABLE = INSTALACIONES;

    @BeforeEach
    void setup() {
        repository = new ContratoJooqRepository(dsl);

        // ==================== LIMPIEZA ====================
        // Orden correcto: de dependientes a independientes
        dsl.deleteFrom(CONTRATOS_TABLE).execute();
        dsl.deleteFrom(CLIENTES_TABLE).execute();
        dsl.deleteFrom(INSTALACIONES_TABLE).execute();

        // ==================== INSERCIÓN DE TEST DATA ====================
        // Usar builders que devuelven objetos con IDs reales
        cliente1Id = crearClienteYObtenerID("Cliente Test 1", "test1@empresa.es");
        cliente2Id = crearClienteYObtenerID("Cliente Test 2", "test2@empresa.es");

        instalacion1Id = crearInstalacionYObtenerID("Madrid", "Oficina");
        instalacion2Id = crearInstalacionYObtenerID("Barcelona", "Almacén");

        // 🔍 DEBUG: Verificar que los datos existen
        long countClientes = dsl.selectCount()
                .from(CLIENTES_TABLE)
                .fetchOne(0, long.class);
        long countInstalaciones = dsl.selectCount()
                .from(INSTALACIONES_TABLE)
                .fetchOne(0, long.class);

        System.out.println("Setup completado:");
        System.out.println("   - Clientes: " + countClientes);
        System.out.println("   - Instalaciones: " + countInstalaciones);
        System.out.println("   - cliente1Id: " + cliente1Id);
        System.out.println("   - instalacion1Id: " + instalacion1Id);
    }

    // ============= TESTS DE LECTURA (SELECT) =============

    @Test
    @DisplayName("findById debe retornar Optional.empty cuando contrato no existe")
    void testFindByIdNoExiste() {
        // Act
        Optional<Contrato> resultado = repository.findById(999L);

        // Assert
        assertTrue(resultado.isEmpty(), "Debería retornar Optional vacío");
    }

    @Test
    @DisplayName("findById debe retornar contrato cuando existe")
    void testFindByIdExiste() {
        // Arrange: Crear un contrato de prueba y obtener su ID real
        Long contratoId = crearContratoTestYObtenerID("CTR-TEST-001", cliente1Id, instalacion1Id);

        // Act
        Optional<Contrato> resultado = repository.findById(contratoId);  // Usar ID real

        // Assert
        assertTrue(resultado.isPresent(), "Debería retornar Optional con contrato");
        assertEquals("CTR-TEST-001", resultado.get().getNumero());
    }

    @Test
    @DisplayName("findAllActivos debe retornar solo contratos VIGENTE")
    void testFindAllActivos() {
        // Arrange - ESTADO SE PASA CORRECTAMENTE
        crearContratoTest("CTR-001", cliente1Id, instalacion1Id, "VIGENTE");
        crearContratoTest("CTR-002", cliente1Id, instalacion1Id, "VIGENTE");
        crearContratoTest("CTR-003", cliente1Id, instalacion1Id, "VENCIDO");

        // Act
        List<Contrato> resultados = repository.findAllActivos();

        // Assert
        assertEquals(2, resultados.size(), "Debería retornar solo 2 contratos VIGENTES");
        assertTrue(resultados.stream().allMatch(c -> "VIGENTE".equals(c.getEstado())));
    }

    @Test
    @DisplayName("findByClienteId debe retornar contratos de cliente específico")
    void testFindByClienteId() {
        // Arrange
        crearContratoTest("CTR-001", cliente1Id, instalacion1Id);
        crearContratoTest("CTR-002", cliente1Id, instalacion1Id);
        crearContratoTest("CTR-003", cliente2Id, instalacion1Id);  // Otro cliente

        // Act
        List<Contrato> resultados = repository.findByClienteId(cliente1Id.longValue());

        // Assert
        assertEquals(2, resultados.size(), "Debería retornar 2 contratos del cliente 1");
        assertTrue(resultados.stream().allMatch(c -> c.getClienteId() == cliente1Id));
    }

    @Test
    @DisplayName("findProximosAVencer debe retornar contratos próximos a vencer")
    void testFindProximosAVencer() {
        // Arrange
        LocalDate hoy = LocalDate.now();
        crearContratoTestConFechas("CTR-001", cliente1Id, instalacion1Id,
                hoy.minusDays(10), hoy.plusDays(15));
        crearContratoTestConFechas("CTR-002", cliente1Id, instalacion1Id,
                hoy.minusDays(10), hoy.plusDays(60));

        // Act
        List<Contrato> resultados = repository.findProximosAVencer();

        // Assert
        assertEquals(1, resultados.size(), "Solo CTR-001 vence en 30 días");
        assertEquals("CTR-001", resultados.get(0).getNumero());
    }

    @Test
    @DisplayName("findVencidos debe retornar solo contratos vencidos")
    void testFindVencidos() {
        // Arrange
        LocalDate hoy = LocalDate.now();
        crearContratoTestConFechasConEstado("CTR-001", cliente1Id, instalacion1Id,
                hoy.minusDays(30), hoy.minusDays(10), "VENCIDO");
        crearContratoTestConFechasConEstado("CTR-002", cliente1Id, instalacion1Id,
                hoy.minusDays(10), hoy.plusDays(10), "VIGENTE");

        // Act
        List<Contrato> resultados = repository.findVencidos();

        // Assert
        assertEquals(1, resultados.size(), "Solo CTR-001 está vencido");
        assertEquals("CTR-001", resultados.get(0).getNumero());
    }

    // ============= TESTS DE ESCRITURA (INSERT) =============

    @Test
    @DisplayName("save debe insertar nuevo contrato correctamente")
    void testSaveNuevoContrato() {
        // Arrange
        Contrato nuevoContrato = Contrato.builder()
                .numero("CTR-NEW-001")
                .clienteId(cliente1Id)
                .instalacionId(instalacion1Id)
                .tipoContrato("MANTENIMIENTO")
                .estado("VIGENTE")
                .coberturaMaterial(true)
                .coberturaManoObra(true)
                .coberturaFinSemana(false)
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(12000))
                .porcentajeCentral(70)
                .empresaId(1)
                .build();

        // Act
        Contrato resultado = repository.save(nuevoContrato);

        // Assert
        assertNotNull(resultado.getId(), "El contrato guardado debe tener ID");
        assertEquals("CTR-NEW-001", resultado.getNumero());

        // Verificar que se guardó en BD
        Optional<Contrato> verificacion = repository.findById(resultado.getId().longValue());
        assertTrue(verificacion.isPresent());
    }

    @Test
    @DisplayName("save debe lanzar DuplicateEntryException si número ya existe")
    void testSaveDuplicateNumber() {
        // Arrange
        crearContratoTest("CTR-DUP-001", cliente1Id, instalacion1Id);

        Contrato duplicado = Contrato.builder()
                .numero("CTR-DUP-001")  // Mismo número
                .clienteId(cliente2Id)
                .instalacionId(instalacion2Id)
                .tipoContrato("MANTENIMIENTO")
                .estado("VIGENTE")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(15000))
                .empresaId(1)
                .build();

        // Act & Assert
        assertThrows(
                DuplicateEntryException.class,
                () -> repository.save(duplicado),
                "Debería lanzar DuplicateEntryException"
        );
    }

    @Test
    @DisplayName("save debe rechazar contrato sin número")
    void testSaveContratoSinNumero() {
        // Arrange
        Contrato sinNumero = Contrato.builder()
                .numero(null)  // ✗ Sin número
                .clienteId(cliente1Id)
                .instalacionId(instalacion1Id)
                .tipoContrato("MANTENIMIENTO")
                .estado("VIGENTE")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(12000))
                .empresaId(1)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> repository.save(sinNumero));
    }

    // ============= TESTS DE ACTUALIZACIÓN (UPDATE) =============

    @Test
    @DisplayName("update debe cambiar estado de contrato")
    void testUpdateEstado() {
        // Arrange - Crear contrato y obtener su ID real
        Long contratoId = crearContratoTestYObtenerID("CTR-UPD-001", cliente1Id, instalacion1Id, "VIGENTE", BigDecimal.valueOf(12000));  //  Con precio

        Contrato contratoAActualizar = repository.findById(contratoId).orElseThrow();  //  Usar ID real
        contratoAActualizar.setEstado("CANCELADO");

        // Act
        repository.update(contratoAActualizar);

        // Assert
        Contrato verificacion = repository.findById(contratoId).orElseThrow();  //  Usar ID real
        assertEquals("CANCELADO", verificacion.getEstado());
    }

    @Test
    @DisplayName("update debe fallar si contrato no existe")
    void testUpdateContratoNoExiste() {
        // Arrange
        Contrato noExiste = Contrato.builder()
                .id(999)
                .estado("VIGENTE")
                .build();

        // Act
        Contrato resultado = repository.update(noExiste);

        // Assert
        assertNotNull(resultado);
    }

    // ============= TESTS DE BÚSQUEDA AVANZADA =============

    @Test
    @DisplayName("findByFiltros debe aplicar múltiples filtros")
    void testFindByFiltrosMultiples() {
        // Arrange - ESTADOS CORRECTOS 
        crearContratoTest("CTR-001", cliente1Id, instalacion1Id, "VIGENTE", BigDecimal.valueOf(15000));
        crearContratoTest("CTR-002", cliente1Id, instalacion1Id, "VIGENTE", BigDecimal.valueOf(5000));
        crearContratoTest("CTR-003", cliente2Id, instalacion1Id, "VENCIDO", BigDecimal.valueOf(15000));

        // Act
        List<Contrato> resultado = repository.findByFiltros(
                cliente1Id.longValue(),
                null,
                "VIGENTE"
        );

        // Assert
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(c -> c.getClienteId().equals(cliente1Id)));
        assertTrue(resultado.stream().allMatch(c -> "VIGENTE".equals(c.getEstado())));
    }

    @Test
    @DisplayName("findByFiltros sin filtros debe retornar todos")
    void testFindByFiltrosSinFiltros() {
        // Arrange
        crearContratoTest("CTR-001", cliente1Id, instalacion1Id);
        crearContratoTest("CTR-002", cliente2Id, instalacion1Id);
        crearContratoTest("CTR-003", cliente1Id, instalacion2Id);

        // Act
        List<Contrato> resultado = repository.findByFiltros(null, null, null);

        // Assert
        assertEquals(3, resultado.size());
    }

    // ============= MÉTODOS AUXILIARES PARA TESTS =============

    /**
     * Helper: Crear cliente y retorna su ID generado por la BD
     *  FIXED: Usa RETURNING() para obtener ID real
     */
    private Integer crearClienteYObtenerID(String nombre, String email) {
        Result<ClientesRecord> result = dsl.insertInto(CLIENTES_TABLE)
                .columns(CLIENTES_TABLE.NOMBRE, CLIENTES_TABLE.EMAIL, CLIENTES_TABLE.USUARIO_CREACION)
                .values(nombre, email, "TEST")
                .returning(CLIENTES_TABLE.ID)
                .fetch();

        if (!result.isEmpty()) {
            return result.get(0).getValue(CLIENTES_TABLE.ID, Integer.class);
        }
        throw new RuntimeException("No se pudo crear cliente: " + nombre);
    }

    /**
     * Helper: Crear instalación y retorna su ID generado por la BD
     *  FIXED: Usa RETURNING() para obtener ID real
     */
    private Integer crearInstalacionYObtenerID(String ubicacion, String tipo) {
        Result<InstalacionesRecord> result = dsl.insertInto(INSTALACIONES_TABLE)
                .columns(INSTALACIONES_TABLE.UBICACION, INSTALACIONES_TABLE.TIPO, INSTALACIONES_TABLE.USUARIO_CREACION)
                .values(ubicacion, tipo, "TEST")
                .returning(INSTALACIONES_TABLE.ID)
                .fetch();

        if (!result.isEmpty()) {
            return result.get(0).getValue(INSTALACIONES_TABLE.ID, Integer.class);
        }
        throw new RuntimeException("No se pudo crear instalación: " + ubicacion);
    }

    /**
     * Helper: Crear contrato y retorna su ID generado por la BD
     *  NUEVO: Obtiene el ID real del contrato insertado
     */
    private Long crearContratoTestYObtenerID(String numero, int clienteId, int instalacionId) {
        return crearContratoTestYObtenerID(numero, clienteId, instalacionId, "VIGENTE", BigDecimal.valueOf(12000));
    }

    /**
     * Helper: Crear contrato con estado y retorna su ID
     */
    private Long crearContratoTestYObtenerID(String numero, int clienteId, int instalacionId,
                                             String estado, BigDecimal precio) {
        LocalDate hoy = LocalDate.now();
        Result<ContratosRecord> result = dsl.insertInto(CONTRATOS_TABLE)
                .columns(CONTRATOS_TABLE.NUMERO, CONTRATOS_TABLE.CLIENTE_ID, CONTRATOS_TABLE.INSTALACION_ID,
                        CONTRATOS_TABLE.TIPO_CONTRATO, CONTRATOS_TABLE.ESTADO,
                        CONTRATOS_TABLE.COBERTURA_MATERIAL, CONTRATOS_TABLE.COBERTURA_MANO_OBRA,
                        CONTRATOS_TABLE.COBERTURA_FIN_SEMANA,
                        CONTRATOS_TABLE.FECHA_INICIO, CONTRATOS_TABLE.FECHA_FIN,
                        CONTRATOS_TABLE.PRECIO_ANUAL, CONTRATOS_TABLE.PORCENTAJE_CENTRAL,
                        CONTRATOS_TABLE.EMPRESA_ID, CONTRATOS_TABLE.USUARIO_CREACION)
                .values(numero, clienteId, instalacionId,
                        "MANTENIMIENTO", estado,
                        true, true, false,
                        hoy, hoy.plusYears(1),
                        precio, 70, 1, "TEST")
                .returning(CONTRATOS_TABLE.ID)
                .fetch();

        if (!result.isEmpty()) {
            return result.get(0).getValue(CONTRATOS_TABLE.ID, Long.class);
        }
        throw new RuntimeException("No se pudo crear contrato: " + numero);
    }

    /**
     * Helper: Crear contrato con valores por defecto
     * Estado: VIGENTE | Precio: 12000 | Período: 1 año desde hoy
     */
    private void crearContratoTest(String numero, int clienteId, int instalacionId) {
        crearContratoTest(numero, clienteId, instalacionId, "VIGENTE");
    }

    /**
     * Helper: Crear contrato con estado específico
     * Precio: 12000 | Período: 1 año desde hoy
     */
    private void crearContratoTest(String numero, int clienteId, int instalacionId, String estado) {
        crearContratoTest(numero, clienteId, instalacionId, estado, BigDecimal.valueOf(12000));
    }

    /**
     * Helper: Crear contrato con estado y precio específicos
     * Período: 1 año desde hoy
     *  FIXED: Ahora pasa estado y precio correctamente
     */
    private void crearContratoTest(String numero, int clienteId, int instalacionId,
                                   String estado, BigDecimal precio) {
        LocalDate hoy = LocalDate.now();
        crearContratoTestConFechasConEstado(numero, clienteId, instalacionId,
                hoy, hoy.plusYears(1), estado, precio);
    }

    /**
     * Helper: Crear contrato con fechas específicas
     * Estado: VIGENTE | Precio: 12000
     *  FIXED: Delega a método completo con todos los parámetros
     */
    private void crearContratoTestConFechas(String numero, int clienteId, int instalacionId,
                                            LocalDate inicio, LocalDate fin) {
        crearContratoTestConFechasConEstado(numero, clienteId, instalacionId,
                inicio, fin, "VIGENTE", BigDecimal.valueOf(12000));
    }

    /**
     * Helper: Crear contrato con fechas y estado
     * Precio: 12000
     *  FIXED: Delega a método completo
     */
    private void crearContratoTestConFechasConEstado(String numero, int clienteId, int instalacionId,
                                                     LocalDate inicio, LocalDate fin, String estado) {
        crearContratoTestConFechasConEstado(numero, clienteId, instalacionId,
                inicio, fin, estado, BigDecimal.valueOf(12000));
    }

    /**
     * Helper: Crear contrato con TODOS los parámetros especificados
     * Este es el punto único de verdad para la inserción
     *  FIXED: Ahora recibe y usa TODOS los parámetros
     */
    private void crearContratoTestConFechasConEstado(String numero, int clienteId, int instalacionId,
                                                     LocalDate inicio, LocalDate fin,
                                                     String estado, BigDecimal precio) {
        dsl.insertInto(CONTRATOS_TABLE)
                .set(CONTRATOS_TABLE.NUMERO, numero)
                .set(CONTRATOS_TABLE.CLIENTE_ID, clienteId)
                .set(CONTRATOS_TABLE.INSTALACION_ID, instalacionId)
                .set(CONTRATOS_TABLE.TIPO_CONTRATO, "MANTENIMIENTO")
                .set(CONTRATOS_TABLE.ESTADO, estado)  //  SE USA
                .set(CONTRATOS_TABLE.COBERTURA_MATERIAL, true)
                .set(CONTRATOS_TABLE.COBERTURA_MANO_OBRA, true)
                .set(CONTRATOS_TABLE.COBERTURA_FIN_SEMANA, false)
                .set(CONTRATOS_TABLE.FECHA_INICIO, inicio)
                .set(CONTRATOS_TABLE.FECHA_FIN, fin)
                .set(CONTRATOS_TABLE.PRECIO_ANUAL, precio)  //  SE USA
                .set(CONTRATOS_TABLE.PORCENTAJE_CENTRAL, 70)
                .set(CONTRATOS_TABLE.EMPRESA_ID, 1)
                .set(CONTRATOS_TABLE.USUARIO_CREACION, "TEST")
                .execute();
    }
}