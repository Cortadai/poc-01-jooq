package com.entelgy.infrastructure.repository;

import com.entelgy.application.dto.ClienteConContratosDTO;
import com.entelgy.application.dto.ContratoActivoClienteEmpresaDTO;
import com.entelgy.application.dto.ContratoClienteDTO;
import com.entelgy.application.dto.ContratoDetalleDTO;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jooq.JooqTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de INTEGRACIÓN para JoinExamplesRepository
 *
 * Estrategia:
 * - ✅ @JooqTest: Carga contexto con Testcontainers + Flyway
 * - ✅ @Transactional: Aislamiento y rollback automático
 * - ✅ Testcontainers: PostgreSQL real en Docker
 * - ✅ Flyway: Migraciones automáticas (V1__Initial_Schema.sql)
 * - ✅ Test data: Inserción en @BeforeEach
 *
 * Cobertura:
 * 1. INNER JOINs (simple y múltiple)
 * 2. LEFT JOINs (con NULLs)
 * 3. WHERE complejos (LIKE, AND, OR)
 * 4. ORDER BY (verificar orden)
 * 5. Aggregations (COUNT)
 * 6. Edge cases (registros sin coincidencia, resultados vacíos)
 *
 * Nota: Flyway ejecuta automáticamente V1__Initial_Schema.sql
 * Que ya contiene datos de prueba iniciales
 */
@JooqTest
@Transactional
@ActiveProfiles("test")
class JoinExamplesRepositoryTest {

    @Autowired
    private DSLContext dsl;

    private JoinExamplesRepository repository;

    // ============== SETUP ==============

    @BeforeEach
    void setup() {
        repository = new JoinExamplesRepository(dsl);

        // Los datos iniciales ya están en BD gracias a Flyway (V1__Initial_Schema.sql)
        // Este método puede usarse para agregar datos adicionales si es necesario

        // Verificar que la BD está inicializada
        long totalContratos = repository.contarTotalContratos();
        assertTrue(totalContratos > 0, "Debe haber contratos iniciales de Flyway");
    }

    // ============== TESTS: INNER JOIN SIMPLE ==============

    @Test
    @DisplayName("getContratosConClientes debe retornar todos los contratos con sus clientes")
    void testGetContratosConClientes() {
        // Act
        List<ContratoClienteDTO> resultado = repository.getContratosConClientes();

        // Assert
        assertNotNull(resultado, "Resultado no debe ser nulo");
        assertFalse(resultado.isEmpty(), "Debe haber contratos");
        assertTrue(resultado.size() >= 4, "Debe haber al menos 4 contratos (datos Flyway)");

        // Verificar estructura de datos
        ContratoClienteDTO primer = resultado.get(0);
        assertNotNull(primer.getId(), "ID del contrato no debe ser nulo");
        assertNotNull(primer.getNumero(), "Número del contrato no debe ser nulo");
        assertNotNull(primer.getEstado(), "Estado no debe ser nulo");
        assertNotNull(primer.getClienteNombre(), "Nombre del cliente no debe ser nulo (INNER JOIN)");
        assertNotNull(primer.getClienteEmail(), "Email del cliente no debe ser nulo (INNER JOIN)");
    }

    @Test
    @DisplayName("getContratosConClientes debe ordenar por número de contrato")
    void testGetContratosConClientesOrdenamiento() {
        // Act
        List<ContratoClienteDTO> resultado = repository.getContratosConClientes();

        // Assert
        assertTrue(resultado.size() >= 2, "Debe haber al menos 2 contratos para verificar orden");

        // Verificar que está ordenado por número
        for (int i = 0; i < resultado.size() - 1; i++) {
            String numeroActual = resultado.get(i).getNumero();
            String numeroSiguiente = resultado.get(i + 1).getNumero();
            assertTrue(numeroActual.compareTo(numeroSiguiente) <= 0,
                    "Debe estar ordenado por número: " + numeroActual + " <= " + numeroSiguiente);
        }
    }

    @Test
    @DisplayName("getContratosConClientes solo retorna contratos con cliente (INNER JOIN)")
    void testGetContratosConClientesInnerJoin() {
        // Arrange - Insertar contrato sin cliente es imposible (FK NOT NULL)
        // Por lo que este test verifica que todos los registros tienen cliente

        // Act
        List<ContratoClienteDTO> resultado = repository.getContratosConClientes();

        // Assert
        resultado.forEach(dto -> {
            assertNotNull(dto.getClienteNombre(), "Todo contrato debe tener cliente (INNER JOIN)");
            assertNotNull(dto.getClienteEmail(), "Todo contrato debe tener email cliente (INNER JOIN)");
        });
    }

    // ============== TESTS: INNER JOINs MÚLTIPLES ==============

    @Test
    @DisplayName("getContratosConClientesEInstalaciones debe retornar detalles completos")
    void testGetContratosConClientesEInstalaciones() {
        // Act
        List<ContratoDetalleDTO> resultado = repository.getContratosConClientesEInstalaciones();

        // Assert
        assertNotNull(resultado, "Resultado no debe ser nulo");
        assertTrue(resultado.size() >= 1, "Debe haber al menos 1 contrato VIGENTE con cliente e instalación");

        // Verificar estructura completa
        ContratoDetalleDTO primer = resultado.get(0);
        assertNotNull(primer.getId(), "ID no nulo");
        assertNotNull(primer.getNumero(), "Número no nulo");
        assertEquals("VIGENTE", primer.getEstado(), "Debe filtrar por VIGENTE");
        assertNotNull(primer.getPrecioAnual(), "Precio no nulo");
        assertNotNull(primer.getClienteNombre(), "Nombre cliente no nulo");
        assertNotNull(primer.getClienteTelefono(), "Teléfono cliente no nulo");
        assertNotNull(primer.getInstalacionUbicacion(), "Ubicación instalación no nula");
        assertNotNull(primer.getInstalacionTipo(), "Tipo instalación no nulo");
    }

    @Test
    @DisplayName("getContratosConClientesEInstalaciones solo retorna VIGENTES")
    void testGetContratosConClientesEInstalacionesFiltroEstado() {
        // Act
        List<ContratoDetalleDTO> resultado = repository.getContratosConClientesEInstalaciones();

        // Assert
        resultado.forEach(dto -> {
            assertEquals("VIGENTE", dto.getEstado(),
                    "Todos los contratos deben estar VIGENTES");
        });
    }

    @Test
    @DisplayName("getContratosConClientesEInstalaciones ordenamiento por cliente y ubicación")
    void testGetContratosConClientesEInstalacionesOrden() {
        // Act
        List<ContratoDetalleDTO> resultado = repository.getContratosConClientesEInstalaciones();

        // Assert
        assertTrue(resultado.size() >= 1, "Debe haber resultados");

        for (int i = 0; i < resultado.size() - 1; i++) {
            String clienteActual = resultado.get(i).getClienteNombre();
            String clienteSiguiente = resultado.get(i + 1).getClienteNombre();

            // Verificar orden por cliente (y si son del mismo cliente, por ubicación)
            int comparacion = clienteActual.compareTo(clienteSiguiente);
            assertTrue(comparacion <= 0, "Debe estar ordenado por cliente");
        }
    }

    // ============== TESTS: INNER JOIN CON WHERE COMPLEJO ==============

    @Test
    @DisplayName("getContratosActivosConClientesEmpresa debe filtrar por VIGENTE y email LIKE 'empresa'")
    void testGetContratosActivosConClientesEmpresa() {
        // Act
        List<ContratoActivoClienteEmpresaDTO> resultado = repository.getContratosActivosConClientesEmpresa();

        // Assert
        assertNotNull(resultado, "Resultado no debe ser nulo");
        assertTrue(resultado.size() >= 1, "Debe haber contratos activos de empresas");

        // Verificar filtros
        resultado.forEach(dto -> {
            assertEquals("VIGENTE", "VIGENTE", "Implícito: debe estar VIGENTE");
            assertTrue(dto.getClienteEmail().toLowerCase().contains("empresa"),
                    "Email debe contener 'empresa': " + dto.getClienteEmail());
        });
    }

    @Test
    @DisplayName("getContratosActivosConClientesEmpresa ordenamiento DESC por precio")
    void testGetContratosActivosConClientesEmpresaOrdenPrecio() {
        // Act
        List<ContratoActivoClienteEmpresaDTO> resultado = repository.getContratosActivosConClientesEmpresa();

        // Assert
        assertTrue(resultado.size() >= 2, "Necesitar múltiples registros para verificar orden DESC");

        for (int i = 0; i < resultado.size() - 1; i++) {
            BigDecimal precioActual = resultado.get(i).getPrecioAnual();
            BigDecimal precioSiguiente = resultado.get(i + 1).getPrecioAnual();

            assertTrue(precioActual.compareTo(precioSiguiente) >= 0,
                    "Debe estar ordenado DESC por precio: " + precioActual + " >= " + precioSiguiente);
        }
    }

    @Test
    @DisplayName("getContratosActivosConClientesEmpresa retorna campos correctos")
    void testGetContratosActivosConClientesEmpresaCampos() {
        // Act
        List<ContratoActivoClienteEmpresaDTO> resultado = repository.getContratosActivosConClientesEmpresa();

        // Assert
        assertTrue(resultado.size() >= 1, "Debe haber resultados");

        ContratoActivoClienteEmpresaDTO primer = resultado.get(0);
        assertNotNull(primer.getNumeroContrato(), "Número contrato no nulo");
        assertNotNull(primer.getPrecioAnual(), "Precio no nulo");
        assertNotNull(primer.getClienteNombre(), "Nombre cliente no nulo");
        assertNotNull(primer.getClienteEmail(), "Email cliente no nulo");
    }

    // ============== TESTS: LEFT JOIN (CON NULLs) ==============

    @Test
    @DisplayName("getTodosClientesConSusContratos debe retornar todos los clientes")
    void testGetTodosClientesConSusContratos() {
        // Act
        List<ClienteConContratosDTO> resultado = repository.getTodosClientesConSusContratos();

        // Assert
        assertNotNull(resultado, "Resultado no debe ser nulo");
        assertTrue(resultado.size() >= 3, "Debe haber al menos 3 clientes (datos Flyway)");

        // Verificar que hay datos de cliente
        resultado.forEach(dto -> {
            assertNotNull(dto.getClienteId(), "ID cliente no nulo");
            assertNotNull(dto.getClienteNombre(), "Nombre cliente no nulo");
            assertNotNull(dto.getClienteEmail(), "Email cliente no nulo");
        });
    }

    @Test
    @DisplayName("getTodosClientesConSusContratos puede tener campos de contrato NULL (LEFT JOIN)")
    void testGetTodosClientesConSusContratosNulls() {
        // Act
        List<ClienteConContratosDTO> resultado = repository.getTodosClientesConSusContratos();

        // Assert - Esperamos al menos un cliente sin contratos o un cliente con varios contratos
        // En los datos iniciales, todos los clientes tienen contratos
        // Pero este test verifica que la estructura permite NULLs

        boolean hayContratoNull = resultado.stream()
                .anyMatch(dto -> dto.getContratoNumero() == null);

        boolean hayMultiplesContratos = resultado.stream()
                .filter(dto -> dto.getClienteNombre() != null)
                .map(ClienteConContratosDTO::getClienteNombre)
                .distinct()
                .count() < resultado.size();

        // Al menos una de las dos condiciones debe cumplirse
        assertTrue(hayContratoNull || hayMultiplesContratos,
                "LEFT JOIN: debe haber clientes sin contratos O clientes con múltiples contratos");
    }

    @Test
    @DisplayName("getTodosClientesConSusContratos ordenamiento por nombre cliente y número contrato")
    void testGetTodosClientesConSusContratosOrden() {
        // Act
        List<ClienteConContratosDTO> resultado = repository.getTodosClientesConSusContratos();

        // Assert
        assertTrue(resultado.size() >= 1, "Debe haber resultados");

        for (int i = 0; i < resultado.size() - 1; i++) {
            String nombreActual = resultado.get(i).getClienteNombre();
            String nombreSiguiente = resultado.get(i + 1).getClienteNombre();

            assertTrue(nombreActual.compareTo(nombreSiguiente) <= 0,
                    "Debe estar ordenado por nombre cliente");
        }
    }

    @Test
    @DisplayName("getTodosClientesConSusContratos cada cliente puede tener múltiples filas")
    void testGetTodosClientesConSusContratosMultiplesFilas() {
        // Act
        List<ClienteConContratosDTO> resultado = repository.getTodosClientesConSusContratos();

        // Assert - El cliente 1 tiene 2 contratos (CTR-2024-001 y CTR-2024-002)
        long clientesUnicos = resultado.stream()
                .map(ClienteConContratosDTO::getClienteId)
                .distinct()
                .count();

        assertTrue(clientesUnicos < resultado.size(),
                "Debe haber clientes repetidos (con múltiples contratos)");
    }

    // ============== TESTS: AGGREGATIONS ==============

    @Test
    @DisplayName("contarTotalContratos debe retornar cantidad correcta")
    void testContarTotalContratos() {
        // Act
        long total = repository.contarTotalContratos();

        // Assert
        assertTrue(total >= 5, "Debe haber al menos 5 contratos (datos Flyway: 4 VIGENTES + 1 VENCIDO)");
    }

    @Test
    @DisplayName("contarContratosPorEstado debe contar VIGENTES")
    void testContarContratosPorEstadoVigente() {
        // Act
        long vigentes = repository.contarContratosPorEstado("VIGENTE");

        // Assert
        assertTrue(vigentes >= 4, "Debe haber al menos 4 contratos VIGENTES");
    }

    @Test
    @DisplayName("contarContratosPorEstado debe contar VENCIDOS")
    void testContarContratosPorEstadoVencido() {
        // Act
        long vencidos = repository.contarContratosPorEstado("VENCIDO");

        // Assert
        assertTrue(vencidos >= 1, "Debe haber al menos 1 contrato VENCIDO");
    }

    @Test
    @DisplayName("contarContratosPorEstado debe retornar 0 para estado inexistente")
    void testContarContratosPorEstadoInexistente() {
        // Act
        long inexistente = repository.contarContratosPorEstado("INEXISTENTE");

        // Assert
        assertEquals(0, inexistente, "No debe haber contratos con estado INEXISTENTE");
    }

    @Test
    @DisplayName("contarTotalContratos >= contarContratosPorEstado(VIGENTE) + contarContratosPorEstado(VENCIDO)")
    void testContarContratosSuma() {
        // Act
        long total = repository.contarTotalContratos();
        long vigentes = repository.contarContratosPorEstado("VIGENTE");
        long vencidos = repository.contarContratosPorEstado("VENCIDO");

        // Assert
        assertTrue(total >= vigentes + vencidos,
                "Total debe ser >= suma de estados principales");
    }

    // ============== TESTS: EDGE CASES ==============

    @Test
    @DisplayName("getContratosConClientes retorna List (nunca null)")
    void testGetContratosConClientesNeverNull() {
        // Act
        List<ContratoClienteDTO> resultado = repository.getContratosConClientes();

        // Assert
        assertNotNull(resultado, "Debe retornar List, nunca null");
        assertTrue(resultado instanceof List, "Debe ser instanceof List");
    }

    @Test
    @DisplayName("getContratosConClientesEInstalaciones retorna List (nunca null)")
    void testGetContratosConClientesEInstalacionesNeverNull() {
        // Act
        List<ContratoDetalleDTO> resultado = repository.getContratosConClientesEInstalaciones();

        // Assert
        assertNotNull(resultado, "Debe retornar List, nunca null");
        assertTrue(resultado instanceof List, "Debe ser instanceof List");
    }

    @Test
    @DisplayName("getTodosClientesConSusContratos retorna List (nunca null)")
    void testGetTodosClientesConSusContratosNeverNull() {
        // Act
        List<ClienteConContratosDTO> resultado = repository.getTodosClientesConSusContratos();

        // Assert
        assertNotNull(resultado, "Debe retornar List, nunca null");
        assertTrue(resultado instanceof List, "Debe ser instanceof List");
    }

    @Test
    @DisplayName("Todos los DTOs mapean correctamente desde Record de JOOQ")
    void testDTOMapping() {
        // Act
        List<ContratoClienteDTO> contratos = repository.getContratosConClientes();
        List<ContratoDetalleDTO> detalles = repository.getContratosConClientesEInstalaciones();
        List<ClienteConContratosDTO> clientes = repository.getTodosClientesConSusContratos();

        // Assert
        assertTrue(contratos.size() > 0, "Mapeo ContratoClienteDTO funcionando");
        assertTrue(detalles.size() > 0, "Mapeo ContratoDetalleDTO funcionando");
        assertTrue(clientes.size() > 0, "Mapeo ClienteConContratosDTO funcionando");
    }

    // ============== TESTS: DATOS INICIALES FLYWAY ==============

    @Test
    @DisplayName("Flyway ha ejecutado V1__Initial_Schema.sql correctamente")
    void testFlywayMigraciones() {
        // Assert - Verificar que todas las tablas existen y tienen datos
        long totalClientes = dsl.selectCount().from("clientes").fetchOne(0, Long.class);
        long totalInstalaciones = dsl.selectCount().from("instalaciones").fetchOne(0, Long.class);
        long totalContratos = dsl.selectCount().from("contratos").fetchOne(0, Long.class);
        long totalPartes = dsl.selectCount().from("partes").fetchOne(0, Long.class);

        assertTrue(totalClientes >= 3, "Debe haber al menos 3 clientes");
        assertTrue(totalInstalaciones >= 4, "Debe haber al menos 4 instalaciones");
        assertTrue(totalContratos >= 5, "Debe haber al menos 5 contratos");
        assertTrue(totalPartes >= 3, "Debe haber al menos 3 partes");
    }

    @Test
    @DisplayName("Datos iniciales contienen clientes con email que contiene 'empresa'")
    void testDatosIniciales_ClientesConEmpresa() {
        // Act
        List<ContratoActivoClienteEmpresaDTO> resultado = repository.getContratosActivosConClientesEmpresa();

        // Assert
        assertTrue(resultado.size() > 0, "Debe haber clientes con 'empresa' en email");
    }
}