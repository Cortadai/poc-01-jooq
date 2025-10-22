package com.entelgy.presentation;

import com.entelgy.domain.model.Cliente;
import com.entelgy.domain.model.Contrato;
import com.entelgy.domain.model.Instalacion;
import com.entelgy.infrastructure.repository.ClienteJooqRepository;
import com.entelgy.infrastructure.repository.ContratoJooqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests E2E para JoinExamplesController usando JDBC:TC Driver
 *
 * FLUJO SIMPLIFICADO Y ELEGANTE:
 *
 * 1. @ActiveProfiles("test") carga application-test.yml
 *    ├─ datasource.url = jdbc:tc:postgresql:17:///testdb
 *    └─ driver = org.testcontainers.jdbc.ContainerDatabaseDriver
 *
 * 2. Spring inicializa contexto
 *    ├─ JDBC:TC Driver ve jdbc:tc:... URL
 *    ├─ AUTOMÁTICAMENTE inicia contenedor PostgreSQL en Docker
 *    ├─ Flyway Bean se crea
 *    ├─ Flyway executa V1_Initial_Schema.sql
 *    └─ BD lista con schema
 *
 * 3. @BeforeEach (antes de CADA test)
 *    ├─ DELETE FROM contratos CASCADE
 *    ├─ DELETE FROM clientes CASCADE
 *    ├─ DELETE FROM instalaciones CASCADE
 *    ├─ RESET sequences a 1
 *    └─ BD limpia, schema intacto
 *
 * 4. @Test ejecuta
 *    ├─ BD limpia y lista
 *    ├─ Schema válido (Flyway lo creó)
 *    ├─ Datos aislados (cleanup los limpió)
 *    └─ Sin contaminación entre tests
 *
 * 5. Fin de todos los tests
 *    └─ Contenedor destruido automáticamente
 *
 * VENTAJAS vs @Testcontainers:
 *
 * ✅ Código MUCHO más limpio
 * ✅ Sin @Testcontainers, @Container, @DynamicPropertySource
 * ✅ Configuración en YAML (mejor separación de concerns)
 * ✅ Flyway automático (manejado por Spring Boot)
 * ✅ Tests rápidos con testcontainers.reuse.enable=true
 * ✅ Fácil de entender y mantener
 * ✅ Una sola línea: @ActiveProfiles("test")
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("JoinExamplesController - Tests E2E con JDBC:TC Driver")
class JoinExamplesControllerE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClienteJooqRepository clienteRepository;

    @Autowired
    private ContratoJooqRepository contratoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Limpia datos entre tests (pero schema persiste)
     * Esto es MUCHO más rápido que reiniciar Flyway
     */
    @BeforeEach
    void cleanupData() {
        try {
            System.out.println("\n🔴 ========== INICIANDO CLEANUP DATA ==========");

            System.out.println("Ejecutando: DELETE FROM contratos...");
            jdbcTemplate.execute("DELETE FROM \"public\".\"contratos\" CASCADE;");
            System.out.println("✅ Contratos borrados");

            System.out.println("Ejecutando: DELETE FROM clientes...");
            jdbcTemplate.execute("DELETE FROM \"public\".\"clientes\" CASCADE;");
            System.out.println("✅ Clientes borrados");

            System.out.println("Ejecutando: DELETE FROM instalaciones...");
            jdbcTemplate.execute("DELETE FROM \"public\".\"instalaciones\" CASCADE;");
            System.out.println("✅ Instalaciones borradas");

            // Resetear sequences para que empiecen desde 1
            System.out.println("Ejecutando: ALTER SEQUENCE contratos_id_seq...");
            jdbcTemplate.execute("ALTER SEQUENCE \"public\".\"contratos_id_seq\" RESTART WITH 1;");
            System.out.println("✅ Sequence contratos reseteada");

            jdbcTemplate.execute("ALTER SEQUENCE \"public\".\"clientes_id_seq\" RESTART WITH 1;");
            System.out.println("✅ Sequence clientes reseteada");

            jdbcTemplate.execute("ALTER SEQUENCE \"public\".\"instalaciones_id_seq\" RESTART WITH 1;");
            System.out.println("✅ Sequence instalaciones reseteada");

            System.out.println("🟢 ========== CLEANUP COMPLETADO ==========\n");

        } catch (Exception e) {
            System.out.println("❌ ERROR EN CLEANUP: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // ============== TESTS: Reportes ==============

    @Test
    @DisplayName("E2E: GET /api/reportes/joins/contratos-activos retorna VIGENTES")
    void testReporteContratosActivos() {
        // Arrange
        Cliente c1 = crearCliente("Empresa A", "a@empresa.com");
        Cliente c2 = crearCliente("Empresa B", "b@empresa.com");

        crearContrato(c1, "CTR-001", "VIGENTE", BigDecimal.valueOf(10000));
        crearContrato(c2, "CTR-002", "VIGENTE", BigDecimal.valueOf(15000));
        crearContrato(c2, "CTR-003", "VENCIDO", BigDecimal.valueOf(5000));

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/reportes/joins/contratos-activos",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().get("status"));
        assertEquals(2, response.getBody().get("cantidad"), "Debe retornar exactamente 2 VIGENTES");
        assertTrue(response.getBody().containsKey("datos"));
    }

    @Test
    @DisplayName("E2E: GET /api/reportes/joins/clientes-sin-contratos")
    void testReporteClientesSinContratos() {
        // Arrange
        Cliente sin = crearCliente("Sin Contratos", "sin@empresa.com");
        Cliente con = crearCliente("Con Contratos", "con@empresa.com");

        crearContrato(con, "CTR-001", "VIGENTE", BigDecimal.valueOf(10000));

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/reportes/joins/clientes-sin-contratos",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().get("cantidad"));
    }

    @Test
    @DisplayName("E2E: GET /api/reportes/joins/cartera-empresarial retorna ingresos")
    void testReporteCartera() {
        // Arrange
        Cliente c1 = crearCliente("Empresa 1", "c1@empresa.com");
        Cliente c2 = crearCliente("Empresa 2", "c2@empresa.com");

        crearContrato(c1, "CTR-001", "VIGENTE", BigDecimal.valueOf(10000));
        crearContrato(c1, "CTR-002", "VIGENTE", BigDecimal.valueOf(15000));
        crearContrato(c2, "CTR-003", "VIGENTE", BigDecimal.valueOf(25000));

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/reportes/joins/cartera-empresarial",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().get("cantidad"));
        assertTrue(response.getBody().containsKey("analisisSegmento"));
    }

    @Test
    @DisplayName("E2E: GET /api/reportes/joins/estadisticas conteos por estado")
    void testEstadisticasContratoPorEstado() {
        // Arrange
        Cliente c1 = crearCliente("Cliente Test", "test@empresa.com");

        crearContrato(c1, "CTR-001", "VIGENTE", BigDecimal.valueOf(10000));
        crearContrato(c1, "CTR-002", "VIGENTE", BigDecimal.valueOf(10000));
        crearContrato(c1, "CTR-003", "VENCIDO", BigDecimal.valueOf(5000));
        crearContrato(c1, "CTR-004", "CANCELADO", BigDecimal.valueOf(3000));

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/reportes/joins/estadisticas",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // El endpoint devuelve Integer, no Long
        assertEquals(4, ((Number) response.getBody().get("totalContratos")).intValue());
        assertTrue(response.getBody().containsKey("porEstado"));

        Map<String, Number> porEstado = (Map<String, Number>) response.getBody().get("porEstado");
        assertEquals(2, porEstado.get("VIGENTE").intValue());
        assertEquals(1, porEstado.get("VENCIDO").intValue());
        assertEquals(1, porEstado.get("CANCELADO").intValue());
    }

    @Test
    @DisplayName("E2E: GET /api/reportes/joins/contrato/{id}")
    void testObtenerContratoEspecifico() {
        // Arrange
        Cliente c1 = crearCliente("Cliente Test", "test@empresa.com");
        Contrato contrato = crearContrato(c1, "CTR-001", "VIGENTE", BigDecimal.valueOf(10000));

        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/reportes/joins/contrato/" + contrato.getId(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().get("cantidad"));
    }

    @Test
    @DisplayName("E2E: GET /api/reportes/joins/contrato/{id} retorna 404 si no existe")
    void testObtenerContratoEspecifico_NotFound() {
        // Act
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/reportes/joins/contrato/999999",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("E2E: Todos los endpoints retornan 200 OK sin datos")
    void testEndpointsSinDatos() {
        // SIN crear datos (BD limpia)

        // Act & Assert para cada endpoint
        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/reportes/joins/contratos-activos",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }).getStatusCode());

        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/reportes/joins/clientes-sin-contratos",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }).getStatusCode());

        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/reportes/joins/contratos-detalle",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }).getStatusCode());

        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/reportes/joins/cartera-empresarial",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }).getStatusCode());

        assertEquals(HttpStatus.OK, restTemplate.exchange(
                "/api/reportes/joins/estadisticas",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }).getStatusCode());
    }

    // ============== HELPERS ==============

    private Cliente crearCliente(String nombre, String email) {
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setEmail(email);
        cliente.setTelefono("912345678");
        return clienteRepository.save(cliente);
    }

    private Contrato crearContrato(Cliente cliente, String numero, String estado, BigDecimal precio) {
        Contrato contrato = new Contrato();
        contrato.setClienteId(cliente.getId());
        contrato.setNumero(numero);
        contrato.setEstado(estado);
        contrato.setTipoContrato("MANTENIMIENTO");
        contrato.setPrecioAnual(precio);
        contrato.setFechaInicio(LocalDate.now().minusMonths(1));
        contrato.setFechaFin(LocalDate.now().plusMonths(11));
        return contratoRepository.save(contrato);
    }
}