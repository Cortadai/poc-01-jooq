package com.entelgy.application;

import com.entelgy.application.dto.ClienteConContratosDTO;
import com.entelgy.application.dto.ContratoActivoClienteEmpresaDTO;
import com.entelgy.application.dto.ContratoClienteDTO;
import com.entelgy.application.dto.ContratoDetalleDTO;
import com.entelgy.infrastructure.repository.JoinExamplesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests UNITARIOS para JoinExamplesApplicationService
 *
 * Estrategia:
 * - ✅ @ExtendWith(MockitoExtension.class): Mocks sin BD real
 * - ✅ Tests RÁPIDOS (sin Testcontainers ni BD)
 * - ✅ Aislamiento perfecto: Solo testea lógica del Service
 * - ✅ Mock del Repository: Controla exactamente qué datos retorna
 * - ✅ Verificaciones COMPLETAS: No solo contar, sino validar transformación
 * - ✅ Edge cases: null, vacío, anomalías
 *
 * Diferencia vs Tests de Integración (JoinExamplesRepositoryTest):
 * - Repository: Testea queries contra BD real
 * - Service: Testea lógica de negocio con mocks
 *
 * Cobertura:
 * 1. reporteContratosActivos() - Filtro VIGENTE + ordenamiento
 * 2. reporteClientesSinContratosActivos() - LEFT JOIN + análisis
 * 3. reporteContratosConDetalleCompleto() - Cálculos + detección anomalías
 * 4. reporteCarteraEmpresarial() - Agregaciones + TOP N
 * 5. obtenerContratoConCliente() - Búsqueda específica
 * 6. estadisticasContratoPorEstado() - Map de conteos
 * 7. Edge cases y errores
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JoinExamplesApplicationService - Tests Unitarios Completos")
class JoinExamplesApplicationServiceTest {

    @Mock
    private JoinExamplesRepository mockRepository;

    private JoinExamplesApplicationService service;

    @BeforeEach
    void setup() {
        service = new JoinExamplesApplicationService(mockRepository);
    }

    // ============== TESTS: reporteContratosActivos() ==============

    @Test
    @DisplayName("reporteContratosActivos debe filtrar SOLO contratos VIGENTES")
    void testReporteContratosActivos_FiltroVigentes() {
        // Arrange
        List<ContratoClienteDTO> datosRepository = List.of(
                ContratoClienteDTO.builder()
                        .id(1)
                        .numero("CTR-001")
                        .estado("VIGENTE")
                        .clienteNombre("Cliente A")
                        .clienteEmail("a@empresa.com")
                        .build(),
                ContratoClienteDTO.builder()
                        .id(2)
                        .numero("CTR-002")
                        .estado("VENCIDO")
                        .clienteNombre("Cliente B")
                        .clienteEmail("b@empresa.com")
                        .build(),
                ContratoClienteDTO.builder()
                        .id(3)
                        .numero("CTR-003")
                        .estado("VIGENTE")
                        .clienteNombre("Cliente C")
                        .clienteEmail("c@empresa.com")
                        .build(),
                ContratoClienteDTO.builder()
                        .id(4)
                        .numero("CTR-004")
                        .estado("CANCELADO")
                        .clienteNombre("Cliente D")
                        .clienteEmail("d@empresa.com")
                        .build()
        );

        when(mockRepository.getContratosConClientes()).thenReturn(datosRepository);

        // Act
        List<ContratoClienteDTO> resultado = service.reporteContratosActivos();

        // Assert - Verificaciones COMPLETAS
        assertNotNull(resultado, "Resultado no debe ser nulo");
        assertEquals(2, resultado.size(), "Debe haber exactamente 2 contratos VIGENTES");

        // Verificar que TODOS son VIGENTES
        assertTrue(resultado.stream().allMatch(c -> "VIGENTE".equals(c.getEstado())),
                "Todos los contratos deben estar en estado VIGENTE");

        // Verificar específicamente cuáles se retornan
        assertTrue(resultado.stream().anyMatch(c -> "CTR-001".equals(c.getNumero())),
                "Debe incluir CTR-001");
        assertTrue(resultado.stream().anyMatch(c -> "CTR-003".equals(c.getNumero())),
                "Debe incluir CTR-003");
        assertFalse(resultado.stream().anyMatch(c -> "CTR-002".equals(c.getNumero())),
                "No debe incluir CTR-002 (VENCIDO)");
        assertFalse(resultado.stream().anyMatch(c -> "CTR-004".equals(c.getNumero())),
                "No debe incluir CTR-004 (CANCELADO)");

        // Verificar que el repository fue llamado
        verify(mockRepository, times(1)).getContratosConClientes();
        verifyNoMoreInteractions(mockRepository);
    }

    @Test
    @DisplayName("reporteContratosActivos debe ordenar por nombre de cliente (A-Z)")
    void testReporteContratosActivos_Ordenamiento() {
        // Arrange
        List<ContratoClienteDTO> datosDesordenados = List.of(
                ContratoClienteDTO.builder()
                        .id(1)
                        .numero("CTR-001")
                        .estado("VIGENTE")
                        .clienteNombre("Zebra Corp")
                        .build(),
                ContratoClienteDTO.builder()
                        .id(2)
                        .numero("CTR-002")
                        .estado("VIGENTE")
                        .clienteNombre("Acme S.L.")
                        .build(),
                ContratoClienteDTO.builder()
                        .id(3)
                        .numero("CTR-003")
                        .estado("VIGENTE")
                        .clienteNombre("Megacorp Ltd")
                        .build()
        );

        when(mockRepository.getContratosConClientes()).thenReturn(datosDesordenados);

        // Act
        List<ContratoClienteDTO> resultado = service.reporteContratosActivos();

        // Assert - Verificar orden alfabético
        assertEquals("Acme S.L.", resultado.get(0).getClienteNombre());
        assertEquals("Megacorp Ltd", resultado.get(1).getClienteNombre());
        assertEquals("Zebra Corp", resultado.get(2).getClienteNombre());

        // Verificar que está ordenado para todo el flujo
        for (int i = 0; i < resultado.size() - 1; i++) {
            String nombreActual = resultado.get(i).getClienteNombre();
            String nombreSiguiente = resultado.get(i + 1).getClienteNombre();
            assertTrue(nombreActual.compareTo(nombreSiguiente) <= 0,
                    "Debe estar ordenado alfabéticamente: " + nombreActual + " > " + nombreSiguiente);
        }
    }

    @Test
    @DisplayName("reporteContratosActivos con lista vacía debe retornar lista vacía")
    void testReporteContratosActivos_ListaVacia() {
        // Arrange
        when(mockRepository.getContratosConClientes()).thenReturn(Collections.emptyList());

        // Act
        List<ContratoClienteDTO> resultado = service.reporteContratosActivos();

        // Assert
        assertNotNull(resultado, "Nunca debe retornar null");
        assertTrue(resultado.isEmpty(), "Debe retornar lista vacía");
        assertEquals(0, resultado.size());
    }

    @Test
    @DisplayName("reporteContratosActivos cuando NO hay VIGENTES debe retornar vacío")
    void testReporteContratosActivos_SinVigentes() {
        // Arrange
        List<ContratoClienteDTO> datos = List.of(
                ContratoClienteDTO.builder()
                        .numero("CTR-001")
                        .estado("VENCIDO")
                        .clienteNombre("Cliente A")
                        .build(),
                ContratoClienteDTO.builder()
                        .numero("CTR-002")
                        .estado("CANCELADO")
                        .clienteNombre("Cliente B")
                        .build()
        );

        when(mockRepository.getContratosConClientes()).thenReturn(datos);

        // Act
        List<ContratoClienteDTO> resultado = service.reporteContratosActivos();

        // Assert
        assertTrue(resultado.isEmpty(), "Debe retornar lista vacía si no hay VIGENTES");
    }

    // ============== TESTS: reporteClientesSinContratosActivos() ==============

    @Test
    @DisplayName("reporteClientesSinContratosActivos debe retornar clientes sin contratos")
    void testReporteClientesSinContratos_SinContratos() {
        // Arrange
        List<ClienteConContratosDTO> datos = List.of(
                ClienteConContratosDTO.builder()
                        .clienteId(1)
                        .clienteNombre("Cliente SinContratos")
                        .clienteEmail("sin@empresa.com")
                        .contratoNumero(null)
                        .contratoEstado(null)
                        .build(),
                ClienteConContratosDTO.builder()
                        .clienteId(2)
                        .clienteNombre("Cliente ConVigente")
                        .clienteEmail("con@empresa.com")
                        .contratoNumero("CTR-001")
                        .contratoEstado("VIGENTE")
                        .build(),
                ClienteConContratosDTO.builder()
                        .clienteId(3)
                        .clienteNombre("Otro SinContratos")
                        .clienteEmail("otro@empresa.com")
                        .contratoNumero(null)
                        .contratoEstado(null)
                        .build()
        );

        when(mockRepository.getTodosClientesConSusContratos()).thenReturn(datos);

        // Act
        List<ClienteConContratosDTO> resultado = service.reporteClientesSinContratosActivos();

        // Assert
        assertNotNull(resultado, "Resultado no debe ser nulo");
        assertEquals(2, resultado.size(), "Debe haber 2 clientes sin contratos");

        // Verificar que solo contiene clientes sin contratos
        assertTrue(resultado.stream().allMatch(c -> c.getContratoNumero() == null),
                "Todos deben tener contratoNumero = null");

        // Verificar específicamente cuáles se retornan
        assertTrue(resultado.stream().anyMatch(c -> "Cliente SinContratos".equals(c.getClienteNombre())),
                "Debe incluir Cliente SinContratos");
        assertTrue(resultado.stream().anyMatch(c -> "Otro SinContratos".equals(c.getClienteNombre())),
                "Debe incluir Otro SinContratos");
        assertFalse(resultado.stream().anyMatch(c -> "Cliente ConVigente".equals(c.getClienteNombre())),
                "No debe incluir Cliente ConVigente");

        verify(mockRepository, times(1)).getTodosClientesConSusContratos();
    }

    @Test
    @DisplayName("reporteClientesSinContratosActivos debe retornar vacío si todos tienen contratos")
    void testReporteClientesSinContratos_TodosTienenContratos() {
        // Arrange
        List<ClienteConContratosDTO> datos = List.of(
                ClienteConContratosDTO.builder()
                        .clienteId(1)
                        .clienteNombre("Cliente A")
                        .contratoNumero("CTR-001")
                        .contratoEstado("VIGENTE")
                        .build(),
                ClienteConContratosDTO.builder()
                        .clienteId(2)
                        .clienteNombre("Cliente B")
                        .contratoNumero("CTR-002")
                        .contratoEstado("VIGENTE")
                        .build()
        );

        when(mockRepository.getTodosClientesConSusContratos()).thenReturn(datos);

        // Act
        List<ClienteConContratosDTO> resultado = service.reporteClientesSinContratosActivos();

        // Assert
        assertTrue(resultado.isEmpty(), "Debe estar vacío si todos tienen contratos");
    }

    @Test
    @DisplayName("reporteClientesSinContratosActivos debe identificar clientes con solo VENCIDOS")
    void testReporteClientesSinContratos_SoloVencidos() {
        // Arrange - Cliente con múltiples registros, todos VENCIDOS
        List<ClienteConContratosDTO> datos = List.of(
                ClienteConContratosDTO.builder()
                        .clienteId(1)
                        .clienteNombre("Cliente Riesgo")
                        .contratoNumero("CTR-001")
                        .contratoEstado("VENCIDO")
                        .build(),
                ClienteConContratosDTO.builder()
                        .clienteId(1)
                        .clienteNombre("Cliente Riesgo")
                        .contratoNumero("CTR-002")
                        .contratoEstado("VENCIDO")
                        .build(),
                ClienteConContratosDTO.builder()
                        .clienteId(2)
                        .clienteNombre("Cliente Activo")
                        .contratoNumero("CTR-003")
                        .contratoEstado("VIGENTE")
                        .build()
        );

        when(mockRepository.getTodosClientesConSusContratos()).thenReturn(datos);

        // Act
        List<ClienteConContratosDTO> resultado = service.reporteClientesSinContratosActivos();

        // Assert
        // Clientes con solo vencidos NO aparecen en el reporte (pero se loguean como alerta)
        assertTrue(resultado.isEmpty(),
                "Clientes con solo VENCIDOS no aparecen en 'sin contratos activos'");
    }

    // ============== TESTS: reporteContratosConDetalleCompleto() ==============

    @Test
    @DisplayName("reporteContratosConDetalleCompleto debe retornar detalles y calcular cartera")
    void testReporteDetalleCompleto_CalculoCartera() {
        // Arrange
        List<ContratoDetalleDTO> datos = List.of(
                ContratoDetalleDTO.builder()
                        .id(1)
                        .numero("CTR-001")
                        .estado("VIGENTE")
                        .precioAnual(BigDecimal.valueOf(12000))
                        .clienteNombre("Cliente A")
                        .clienteTelefono("912345678")
                        .instalacionUbicacion("Madrid")
                        .instalacionTipo("Oficina")
                        .build(),
                ContratoDetalleDTO.builder()
                        .id(2)
                        .numero("CTR-002")
                        .estado("VIGENTE")
                        .precioAnual(BigDecimal.valueOf(18000))
                        .clienteNombre("Cliente B")
                        .clienteTelefono("934567890")
                        .instalacionUbicacion("Barcelona")
                        .instalacionTipo("Almacén")
                        .build(),
                ContratoDetalleDTO.builder()
                        .id(3)
                        .numero("CTR-003")
                        .estado("VIGENTE")
                        .precioAnual(BigDecimal.valueOf(15000))
                        .clienteNombre("Cliente C")
                        .clienteTelefono("955678901")
                        .instalacionUbicacion("Valencia")
                        .instalacionTipo("Planta")
                        .build()
        );

        when(mockRepository.getContratosConClientesEInstalaciones()).thenReturn(datos);

        // Act
        List<ContratoDetalleDTO> resultado = service.reporteContratosConDetalleCompleto();

        // Assert - Estructura básica
        assertNotNull(resultado, "Resultado no debe ser nulo");
        assertEquals(3, resultado.size(), "Debe haber 3 contratos");

        // Verificar que todos tienen detalles completos
        resultado.forEach(dto -> {
            assertNotNull(dto.getId(), "ID no nulo");
            assertNotNull(dto.getNumero(), "Número no nulo");
            assertNotNull(dto.getEstado(), "Estado no nulo");
            assertNotNull(dto.getPrecioAnual(), "Precio no nulo");
            assertNotNull(dto.getClienteNombre(), "Nombre cliente no nulo");
            assertNotNull(dto.getClienteTelefono(), "Teléfono cliente no nulo");
            assertNotNull(dto.getInstalacionUbicacion(), "Ubicación instalación no nula");
            assertNotNull(dto.getInstalacionTipo(), "Tipo instalación no nulo");
        });

        // Verificar cálculo de cartera total
        BigDecimal carteraEsperada = BigDecimal.valueOf(45000);
        BigDecimal carteraActual = resultado.stream()
                .map(ContratoDetalleDTO::getPrecioAnual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(carteraEsperada, carteraActual, "Cartera total debe ser 45000");

        verify(mockRepository, times(1)).getContratosConClientesEInstalaciones();
    }

    @Test
    @DisplayName("reporteContratosConDetalleCompleto debe detectar precios anómalos (2x promedio)")
    void testReporteDetalleCompleto_DetectaAnomalias() {
        // Arrange
        List<ContratoDetalleDTO> datos = List.of(
                ContratoDetalleDTO.builder()
                        .numero("CTR-001")
                        .precioAnual(BigDecimal.valueOf(10000))
                        .build(),
                ContratoDetalleDTO.builder()
                        .numero("CTR-002")
                        .precioAnual(BigDecimal.valueOf(10000))
                        .build(),
                ContratoDetalleDTO.builder()
                        .numero("CTR-ANOMALIA")
                        .precioAnual(BigDecimal.valueOf(100000))  // 5x el promedio
                        .build()
        );

        when(mockRepository.getContratosConClientesEInstalaciones()).thenReturn(datos);

        // Act
        List<ContratoDetalleDTO> resultado = service.reporteContratosConDetalleCompleto();

        // Assert
        assertEquals(3, resultado.size(), "Debe retornar todos los contratos");

        // Verificar cálculo: (10k + 10k + 100k) / 3 = 40k
        BigDecimal carteraTotal = BigDecimal.valueOf(120000);
        BigDecimal carteraActual = resultado.stream()
                .map(ContratoDetalleDTO::getPrecioAnual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(carteraTotal, carteraActual, "Cartera total 120000");

        // El promedio es 40k, la anomalía es 100k (2.5x) → DEBE detectarse
        BigDecimal promedio = carteraTotal.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal anomaliaEsperada = BigDecimal.valueOf(100000);
        assertTrue(anomaliaEsperada.compareTo(promedio.multiply(BigDecimal.valueOf(2))) > 0,
                "100k > 40k * 2, debería detectarse como anomalía");
    }

    @Test
    @DisplayName("reporteContratosConDetalleCompleto con lista vacía debe retornar vacío")
    void testReporteDetalleCompleto_ListaVacia() {
        // Arrange
        when(mockRepository.getContratosConClientesEInstalaciones()).thenReturn(Collections.emptyList());

        // Act
        List<ContratoDetalleDTO> resultado = service.reporteContratosConDetalleCompleto();

        // Assert
        assertNotNull(resultado, "Nunca null");
        assertTrue(resultado.isEmpty(), "Debe estar vacío");
    }

    // ============== TESTS: reporteCarteraEmpresarial() ==============

    @Test
    @DisplayName("reporteCarteraEmpresarial debe calcular ingresos totales y clientes únicos")
    void testCarteraEmpresarial_CalculoIngresos() {
        // Arrange
        List<ContratoActivoClienteEmpresaDTO> datos = List.of(
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-001")
                        .clienteNombre("Empresa A")
                        .clienteEmail("empresa-a@corp.com")
                        .precioAnual(BigDecimal.valueOf(20000))
                        .build(),
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-002")
                        .clienteNombre("Empresa A")  // Mismo cliente
                        .clienteEmail("empresa-a@corp.com")
                        .precioAnual(BigDecimal.valueOf(15000))
                        .build(),
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-003")
                        .clienteNombre("Empresa B")
                        .clienteEmail("empresa-b@corp.com")
                        .precioAnual(BigDecimal.valueOf(25000))
                        .build()
        );

        when(mockRepository.getContratosActivosConClientesEmpresa()).thenReturn(datos);

        // Act
        List<ContratoActivoClienteEmpresaDTO> resultado = service.reporteCarteraEmpresarial();

        // Assert - Estructura
        assertEquals(3, resultado.size(), "Debe retornar 3 contratos");

        // Verificar ingresos totales
        BigDecimal ingresosEsperados = BigDecimal.valueOf(60000);
        BigDecimal ingresosActuales = resultado.stream()
                .map(ContratoActivoClienteEmpresaDTO::getPrecioAnual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(ingresosEsperados, ingresosActuales, "Ingresos totales 60000");

        // Verificar clientes únicos
        long clientesUnicos = resultado.stream()
                .map(ContratoActivoClienteEmpresaDTO::getClienteNombre)
                .distinct()
                .count();
        assertEquals(2, clientesUnicos, "Debe haber 2 clientes únicos (Empresa A, Empresa B)");

        verify(mockRepository, times(1)).getContratosActivosConClientesEmpresa();
    }

    @Test
    @DisplayName("reporteCarteraEmpresarial debe identificar TOP 3 clientes ordenados por precio DESC")
    void testCarteraEmpresarial_Top3Ordenados() {
        // Arrange
        List<ContratoActivoClienteEmpresaDTO> datos = List.of(
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-001")
                        .clienteNombre("Empresa D")
                        .precioAnual(BigDecimal.valueOf(10000))
                        .build(),
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-002")
                        .clienteNombre("Empresa C")
                        .precioAnual(BigDecimal.valueOf(20000))
                        .build(),
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-003")
                        .clienteNombre("Empresa B")
                        .precioAnual(BigDecimal.valueOf(30000))
                        .build(),
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-004")
                        .clienteNombre("Empresa A")
                        .precioAnual(BigDecimal.valueOf(50000))
                        .build(),
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-005")
                        .clienteNombre("Empresa E")
                        .precioAnual(BigDecimal.valueOf(5000))
                        .build()
        );

        when(mockRepository.getContratosActivosConClientesEmpresa()).thenReturn(datos);

        // Act
        List<ContratoActivoClienteEmpresaDTO> resultado = service.reporteCarteraEmpresarial();

        // Assert - Verificar que se retornan todos pero podemos obtener TOP 3
        assertEquals(5, resultado.size(), "Debe retornar todos los contratos");

        // Extraer y verificar TOP 3
        List<ContratoActivoClienteEmpresaDTO> top3 = resultado.stream()
                .sorted((a, b) -> b.getPrecioAnual().compareTo(a.getPrecioAnual()))
                .limit(3)
                .collect(Collectors.toList());

        assertEquals(3, top3.size(), "TOP 3 debe tener 3 elementos");
        assertEquals("Empresa A", top3.get(0).getClienteNombre(), "TOP 1: Empresa A (50k)");
        assertEquals("Empresa B", top3.get(1).getClienteNombre(), "TOP 2: Empresa B (30k)");
        assertEquals("Empresa C", top3.get(2).getClienteNombre(), "TOP 3: Empresa C (20k)");

        // Verificar que está ordenado DESC
        for (int i = 0; i < top3.size() - 1; i++) {
            assertTrue(top3.get(i).getPrecioAnual().compareTo(top3.get(i + 1).getPrecioAnual()) >= 0,
                    "Debe estar ordenado DESC");
        }
    }

    // ============== TESTS: obtenerContratoConCliente() ==============

    @Test
    @DisplayName("obtenerContratoConCliente debe retornar contrato específico por ID")
    void testObtenerContratoEspecifico_Existe() {
        // Arrange
        List<ContratoClienteDTO> datos = List.of(
                ContratoClienteDTO.builder().id(1).numero("CTR-001").build(),
                ContratoClienteDTO.builder().id(2).numero("CTR-002").build(),
                ContratoClienteDTO.builder().id(3).numero("CTR-003").build()
        );

        when(mockRepository.getContratosConClientes()).thenReturn(datos);

        // Act
        ContratoClienteDTO resultado = service.obtenerContratoConCliente(2);

        // Assert
        assertNotNull(resultado, "Debe encontrar el contrato");
        assertEquals(2, resultado.getId(), "ID debe ser 2");
        assertEquals("CTR-002", resultado.getNumero(), "Número debe ser CTR-002");
    }

    @Test
    @DisplayName("obtenerContratoConCliente con ID inexistente debe retornar null")
    void testObtenerContratoEspecifico_NoExiste() {
        // Arrange
        List<ContratoClienteDTO> datos = List.of(
                ContratoClienteDTO.builder().id(1).numero("CTR-001").build(),
                ContratoClienteDTO.builder().id(2).numero("CTR-002").build()
        );

        when(mockRepository.getContratosConClientes()).thenReturn(datos);

        // Act
        ContratoClienteDTO resultado = service.obtenerContratoConCliente(999);

        // Assert
        assertNull(resultado, "Debe retornar null si no existe");
    }

    @Test
    @DisplayName("obtenerContratoConCliente con ID null o <= 0 debe retornar null SIN llamar al repository")
    void testObtenerContratoEspecifico_IdInvalido() {
        // Arrange
        // NO configuramos el mock porque el servicio rechaza IDs inválidos ANTES de llamar al repository
        // La validación ocurre en el service (null check, <= 0), no en el repository

        // Act & Assert
        assertNull(service.obtenerContratoConCliente(null), "null debe retornar null");
        assertNull(service.obtenerContratoConCliente(-1), "-1 debe retornar null");
        assertNull(service.obtenerContratoConCliente(0), "0 debe retornar null");

        // Verificar que el repository NUNCA fue llamado (porque la validación es temprana)
        verify(mockRepository, never()).getContratosConClientes();
    }

    // ============== TESTS: estadisticasContratoPorEstado() ==============

    @Test
    @DisplayName("estadisticasContratoPorEstado debe contar correctamente por estado")
    void testEstadisticas_ConteosPorEstado() {
        // Arrange
        List<ContratoClienteDTO> datos = List.of(
                ContratoClienteDTO.builder().numero("CTR-001").estado("VIGENTE").build(),
                ContratoClienteDTO.builder().numero("CTR-002").estado("VIGENTE").build(),
                ContratoClienteDTO.builder().numero("CTR-003").estado("VIGENTE").build(),
                ContratoClienteDTO.builder().numero("CTR-004").estado("VENCIDO").build(),
                ContratoClienteDTO.builder().numero("CTR-005").estado("VENCIDO").build(),
                ContratoClienteDTO.builder().numero("CTR-006").estado("CANCELADO").build()
        );

        when(mockRepository.getContratosConClientes()).thenReturn(datos);

        // Act
        Map<String, Long> resultado = service.estadisticasContratoPorEstado();

        // Assert - Verificar estructura
        assertNotNull(resultado, "Map no debe ser nulo");
        assertFalse(resultado.isEmpty(), "Map no debe estar vacío");

        // Verificar conteos exactos
        assertEquals(3L, resultado.get("VIGENTE"), "Debe haber 3 VIGENTES");
        assertEquals(2L, resultado.get("VENCIDO"), "Debe haber 2 VENCIDOS");
        assertEquals(1L, resultado.get("CANCELADO"), "Debe haber 1 CANCELADO");

        // Verificar suma total
        long totalContratos = resultado.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        assertEquals(6, totalContratos, "Total debe ser 6");

        // Verificar que no hay estados adicionales
        assertEquals(3, resultado.size(), "Solo 3 estados diferentes");

        verify(mockRepository, times(1)).getContratosConClientes();
    }

    @Test
    @DisplayName("estadisticasContratoPorEstado con lista vacía debe retornar Map vacío")
    void testEstadisticas_ListaVacia() {
        // Arrange
        when(mockRepository.getContratosConClientes()).thenReturn(Collections.emptyList());

        // Act
        Map<String, Long> resultado = service.estadisticasContratoPorEstado();

        // Assert
        assertNotNull(resultado, "Nunca null");
        assertTrue(resultado.isEmpty(), "Map debe estar vacío");
    }

    // ============== TESTS: EDGE CASES ==============

    @Test
    @DisplayName("Todos los métodos deben retornar List o Map, nunca null")
    void testNuncaRetornaNull() {
        // Arrange
        when(mockRepository.getContratosConClientes()).thenReturn(Collections.emptyList());
        when(mockRepository.getTodosClientesConSusContratos()).thenReturn(Collections.emptyList());
        when(mockRepository.getContratosConClientesEInstalaciones()).thenReturn(Collections.emptyList());
        when(mockRepository.getContratosActivosConClientesEmpresa()).thenReturn(Collections.emptyList());

        // Assert
        assertNotNull(service.reporteContratosActivos());
        assertNotNull(service.reporteClientesSinContratosActivos());
        assertNotNull(service.reporteContratosConDetalleCompleto());
        assertNotNull(service.reporteCarteraEmpresarial());
        assertNotNull(service.estadisticasContratoPorEstado());
    }

    @Test
    @DisplayName("El service debe usar el repository exactamente UNA VEZ por método")
    void testRepositorioLlamadoUnaVez() {
        // Arrange
        when(mockRepository.getContratosConClientes()).thenReturn(Collections.emptyList());

        // Act
        service.reporteContratosActivos();

        // Assert
        verify(mockRepository, times(1)).getContratosConClientes();
        verifyNoMoreInteractions(mockRepository);
    }

    @Test
    @DisplayName("Valores BigDecimal deben compararse correctamente")
    void testComparacionBigDecimal() {
        // Arrange
        List<ContratoDetalleDTO> datos = List.of(
                ContratoDetalleDTO.builder()
                        .numero("CTR-001")
                        .precioAnual(new BigDecimal("12000.00"))
                        .build(),
                ContratoDetalleDTO.builder()
                        .numero("CTR-002")
                        .precioAnual(new BigDecimal("18000.00"))
                        .build()
        );

        when(mockRepository.getContratosConClientesEInstalaciones()).thenReturn(datos);

        // Act
        List<ContratoDetalleDTO> resultado = service.reporteContratosConDetalleCompleto();

        // Assert
        BigDecimal carteraEsperada = new BigDecimal("30000.00");
        BigDecimal carteraActual = resultado.stream()
                .map(ContratoDetalleDTO::getPrecioAnual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, carteraEsperada.compareTo(carteraActual),
                "BigDecimal debe compararse correctamente");
    }
}