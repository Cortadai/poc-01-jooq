package com.entelgy.application;

import com.entelgy.application.dto.ClienteConContratosDTO;
import com.entelgy.application.dto.ContratoClienteDTO;
import com.entelgy.domain.model.Cliente;
import com.entelgy.domain.model.Contrato;
import com.entelgy.infrastructure.repository.ClienteJooqRepository;
import com.entelgy.infrastructure.repository.ContratoJooqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Integración Service+Repository
 *
 * Estrategia:
 * - ✅ @SpringBootTest + @ActiveProfiles("test")
 * - ✅ BD de prueba configurada en application-test.yaml
 * - ✅ @Autowired Service + JooqRepositories (sin @MockBean)
 * - ✅ Inserta datos reales en BD
 * - ✅ Valida lógica CON DATOS REALES
 *
 * Diferencia vs Tests Unitarios:
 * - Unitarios: Repository mockeado → datos fake
 * - Integración: Repository real → datos reales en BD de prueba
 *
 * Cobertura:
 * - Filtro VIGENTE con datos reales
 * - Ordenamientos funcionan realmente
 * - Cálculos son exactos
 * - Detecta bugs entre capas
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JoinExamplesServiceIntegrationTest {

    @Autowired
    private JoinExamplesApplicationService service;

    @Autowired
    private ClienteJooqRepository clienteRepository;

    @Autowired
    private ContratoJooqRepository contratoRepository;

    @BeforeEach
    void setup() {
        // Limpiar datos - usar deleteAll que existe en JooqRepository
        contratoRepository.delete();  // Borra todo
        clienteRepository.delete();   // Borra todo
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
        contrato.setTipoContrato("MANTENIMIENTO");  // ← Obligatorio en BD
        contrato.setPrecioAnual(precio);
        contrato.setFechaInicio(LocalDate.now().minusMonths(1));
        contrato.setFechaFin(LocalDate.now().plusMonths(11));
        return contratoRepository.save(contrato);
    }

    // ============== TESTS ==============

    @Test
    @DisplayName("Integración: Filtro VIGENTE con datos reales")
    void testReporteContratosActivos_FiltroVigente() {
        // Arrange
        Cliente c1 = crearCliente("Cliente A", "a@empresa.com");
        Cliente c2 = crearCliente("Cliente B", "b@empresa.com");

        crearContrato(c1, "CTR-001", "VIGENTE", BigDecimal.valueOf(12000));
        crearContrato(c2, "CTR-002", "VENCIDO", BigDecimal.valueOf(8000));
        crearContrato(c2, "CTR-003", "VIGENTE", BigDecimal.valueOf(15000));

        // Act
        List<ContratoClienteDTO> resultado = service.reporteContratosActivos();

        // Assert
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(c -> "VIGENTE".equals(c.getEstado())));
        assertTrue(resultado.stream().anyMatch(c -> "CTR-001".equals(c.getNumero())));
        assertTrue(resultado.stream().anyMatch(c -> "CTR-003".equals(c.getNumero())));
        assertFalse(resultado.stream().anyMatch(c -> "CTR-002".equals(c.getNumero())));
    }

    @Test
    @DisplayName("Integración: Ordenamiento A-Z")
    void testReporteContratosActivos_Ordenamiento() {
        // Arrange
        Cliente z = crearCliente("Zebra Corp", "z@empresa.com");
        Cliente a = crearCliente("Acme S.L.", "a@empresa.com");
        Cliente m = crearCliente("Megacorp Ltd", "m@empresa.com");

        crearContrato(z, "CTR-001", "VIGENTE", BigDecimal.valueOf(10000));
        crearContrato(a, "CTR-002", "VIGENTE", BigDecimal.valueOf(10000));
        crearContrato(m, "CTR-003", "VIGENTE", BigDecimal.valueOf(10000));

        // Act
        List<ContratoClienteDTO> resultado = service.reporteContratosActivos();

        // Assert
        assertEquals(3, resultado.size());
        assertEquals("Acme S.L.", resultado.get(0).getClienteNombre());
        assertEquals("Megacorp Ltd", resultado.get(1).getClienteNombre());
        assertEquals("Zebra Corp", resultado.get(2).getClienteNombre());
    }

    @Test
    @DisplayName("Integración: LEFT JOIN retorna clientes sin contratos")
    void testReporteClientesSinContratos() {
        // Arrange
        Cliente sin = crearCliente("Cliente Sin", "sin@empresa.com");
        Cliente con = crearCliente("Cliente Con", "con@empresa.com");

        crearContrato(con, "CTR-001", "VIGENTE", BigDecimal.valueOf(12000));

        // Act
        List<ClienteConContratosDTO> resultado = service.reporteClientesSinContratosActivos();

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("Cliente Sin", resultado.get(0).getClienteNombre());
        assertNull(resultado.get(0).getContratoNumero());
    }

    @Test
    @DisplayName("Integración: Estadísticas por estado exactas")
    void testEstadisticasContratoPorEstado() {
        // Arrange
        Cliente c1 = crearCliente("Cliente 1", "c1@empresa.com");
        Cliente c2 = crearCliente("Cliente 2", "c2@empresa.com");

        crearContrato(c1, "CTR-001", "VIGENTE", BigDecimal.valueOf(10000));
        crearContrato(c1, "CTR-002", "VIGENTE", BigDecimal.valueOf(10000));
        crearContrato(c2, "CTR-003", "VENCIDO", BigDecimal.valueOf(8000));
        crearContrato(c2, "CTR-004", "CANCELADO", BigDecimal.valueOf(5000));

        // Act
        Map<String, Long> resultado = service.estadisticasContratoPorEstado();

        // Assert
        assertEquals(3, resultado.size());
        assertEquals(2L, resultado.get("VIGENTE"));
        assertEquals(1L, resultado.get("VENCIDO"));
        assertEquals(1L, resultado.get("CANCELADO"));
    }

    @Test
    @DisplayName("Integración: Sin datos no lanza exceptions")
    void testSinDatos_NoExceptions() {
        // Act & Assert
        assertNotNull(service.reporteContratosActivos());
        assertNotNull(service.reporteClientesSinContratosActivos());
        assertNotNull(service.reporteContratosConDetalleCompleto());
        assertNotNull(service.reporteCarteraEmpresarial());
        assertNotNull(service.estadisticasContratoPorEstado());

        assertTrue(service.reporteContratosActivos().isEmpty());
        assertTrue(service.reporteClientesSinContratosActivos().isEmpty());
        assertTrue(service.reporteContratosConDetalleCompleto().isEmpty());
        assertTrue(service.reporteCarteraEmpresarial().isEmpty());
        assertTrue(service.estadisticasContratoPorEstado().isEmpty());
    }

    @Test
    @DisplayName("Integración: Búsqueda por ID")
    void testObtenerContratoEspecifico() {
        // Arrange
        Cliente cliente = crearCliente("Cliente Test", "test@empresa.com");
        Contrato c1 = crearContrato(cliente, "CTR-001", "VIGENTE", BigDecimal.valueOf(12000));

        // Act
        ContratoClienteDTO resultado = service.obtenerContratoConCliente(c1.getId());

        // Assert
        assertNotNull(resultado);
        assertEquals(c1.getId(), resultado.getId());
        assertEquals("CTR-001", resultado.getNumero());
    }

    @Test
    @DisplayName("Integración: Validaciones entrada")
    void testValidacionesEntrada() {
        // Assert
        assertNull(service.obtenerContratoConCliente(null));
        assertNull(service.obtenerContratoConCliente(-1));
        assertNull(service.obtenerContratoConCliente(0));
        assertNull(service.obtenerContratoConCliente(999999));
    }
}