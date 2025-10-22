package com.entelgy.presentation;

import com.entelgy.application.JoinExamplesApplicationService;
import com.entelgy.application.dto.ClienteConContratosDTO;
import com.entelgy.application.dto.ContratoActivoClienteEmpresaDTO;
import com.entelgy.application.dto.ContratoClienteDTO;
import com.entelgy.application.dto.ContratoDetalleDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para JoinExamplesController
 *
 * Estrategia:
 * - ✅ @WebMvcTest: Solo carga el controller + MockMvc
 * - ✅ @MockBean del Service: Controla exactamente qué retorna
 * - ✅ No BD real: Tests rápidos
 * - ✅ Verifica estructura de respuestas HTTP
 * - ✅ Valida status codes, headers, body
 *
 * Cobertura:
 * 1. GET /api/reportes/joins/contratos-activos
 * 2. GET /api/reportes/joins/clientes-sin-contratos
 * 3. GET /api/reportes/joins/contratos-detalle
 * 4. GET /api/reportes/joins/cartera-empresarial
 * 5. GET /api/reportes/joins/contrato/{id}
 * 6. GET /api/reportes/joins/estadisticas
 * 7. Error cases: 404, 400, etc
 */
@WebMvcTest(JoinExamplesController.class)
@DisplayName("JoinExamplesController - Tests de Integración")
class JoinExamplesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JoinExamplesApplicationService mockService;

    private ContratoClienteDTO contratoClienteDTO;
    private ContratoDetalleDTO contratoDetalleDTO;
    private ClienteConContratosDTO clienteConContratosDTO;
    private ContratoActivoClienteEmpresaDTO contratoEmpresaDTO;

    @BeforeEach
    void setup() {
        // Datos de prueba para los DTOs
        contratoClienteDTO = ContratoClienteDTO.builder()
                .id(1)
                .numero("CTR-001")
                .estado("VIGENTE")
                .clienteNombre("Cliente A")
                .clienteEmail("a@empresa.com")
                .build();

        contratoDetalleDTO = ContratoDetalleDTO.builder()
                .id(1)
                .numero("CTR-001")
                .estado("VIGENTE")
                .precioAnual(BigDecimal.valueOf(12000))
                .clienteNombre("Cliente A")
                .clienteTelefono("912345678")
                .instalacionUbicacion("Madrid")
                .instalacionTipo("Oficina")
                .build();

        clienteConContratosDTO = ClienteConContratosDTO.builder()
                .clienteId(1)
                .clienteNombre("Cliente A")
                .clienteEmail("a@empresa.com")
                .contratoNumero(null)
                .contratoEstado(null)
                .build();

        contratoEmpresaDTO = ContratoActivoClienteEmpresaDTO.builder()
                .numeroContrato("CTR-001")
                .clienteNombre("Empresa A")
                .clienteEmail("empresa-a@corp.com")
                .precioAnual(BigDecimal.valueOf(20000))
                .build();
    }

    // ============== TESTS: GET /contratos-activos ==============

    @Test
    @DisplayName("GET /contratos-activos debe retornar 200 con contratos VIGENTES")
    void testGetContratosActivos_Success() throws Exception {
        // Arrange
        List<ContratoClienteDTO> contratos = List.of(contratoClienteDTO);
        when(mockService.reporteContratosActivos()).thenReturn(contratos);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contratos-activos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.mensaje").value("Contratos activos obtenidos exitosamente"))
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.datos", hasSize(1)))
                .andExpect(jsonPath("$.datos[0].numero").value("CTR-001"))
                .andExpect(jsonPath("$.datos[0].estado").value("VIGENTE"))
                .andExpect(jsonPath("$.datos[0].clienteNombre").value("Cliente A"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(mockService, times(1)).reporteContratosActivos();
    }

    @Test
    @DisplayName("GET /contratos-activos con lista vacía debe retornar 200 con cantidad 0")
    void testGetContratosActivos_Empty() throws Exception {
        // Arrange
        when(mockService.reporteContratosActivos()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contratos-activos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(0))
                .andExpect(jsonPath("$.datos", hasSize(0)));
    }

    // ============== TESTS: GET /clientes-sin-contratos ==============

    @Test
    @DisplayName("GET /clientes-sin-contratos debe retornar 200 con clientes sin contratos")
    void testGetClientesSinContratos_Success() throws Exception {
        // Arrange
        List<ClienteConContratosDTO> clientes = List.of(clienteConContratosDTO);
        when(mockService.reporteClientesSinContratosActivos()).thenReturn(clientes);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/clientes-sin-contratos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.datos", hasSize(1)))
                .andExpect(jsonPath("$.datos[0].clienteNombre").value("Cliente A"))
                .andExpect(jsonPath("$.accion_recomendada").exists());

        verify(mockService, times(1)).reporteClientesSinContratosActivos();
    }

    @Test
    @DisplayName("GET /clientes-sin-contratos con lista vacía debe retornar info")
    void testGetClientesSinContratos_Empty() throws Exception {
        // Arrange
        when(mockService.reporteClientesSinContratosActivos()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/clientes-sin-contratos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(0))
                .andExpect(jsonPath("$.info").value("Todos los clientes tienen contratos vigentes"));
    }

    // ============== TESTS: GET /contratos-detalle ==============

    @Test
    @DisplayName("GET /contratos-detalle debe retornar detalles completos con análisis")
    void testGetContratosDetalle_Success() throws Exception {
        // Arrange
        List<ContratoDetalleDTO> contratos = List.of(
                contratoDetalleDTO,
                ContratoDetalleDTO.builder()
                        .id(2)
                        .numero("CTR-002")
                        .estado("VIGENTE")
                        .precioAnual(BigDecimal.valueOf(18000))
                        .clienteNombre("Cliente B")
                        .clienteTelefono("934567890")
                        .instalacionUbicacion("Barcelona")
                        .instalacionTipo("Almacén")
                        .build()
        );

        when(mockService.reporteContratosConDetalleCompleto()).thenReturn(contratos);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contratos-detalle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.datos", hasSize(2)))
                .andExpect(jsonPath("$.analisisCartera").exists())
                .andExpect(jsonPath("$.analisisCartera.precioAnualTotal").value(30000.0))
                .andExpect(jsonPath("$.analisisCartera.precioAnualPromedio").value(15000.0))
                .andExpect(jsonPath("$.analisisCartera.precioAnualMinimo").value(12000.0))
                .andExpect(jsonPath("$.analisisCartera.precioAnualMaximo").value(18000.0));

        verify(mockService, times(1)).reporteContratosConDetalleCompleto();
    }

    @Test
    @DisplayName("GET /contratos-detalle con lista vacía debe retornar info")
    void testGetContratosDetalle_Empty() throws Exception {
        // Arrange
        when(mockService.reporteContratosConDetalleCompleto()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contratos-detalle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(0))
                .andExpect(jsonPath("$.info").value("No hay contratos con instalaciones registrados"));
    }

    // ============== TESTS: GET /cartera-empresarial ==============

    @Test
    @DisplayName("GET /cartera-empresarial debe retornar contratos empresariales con análisis")
    void testGetCarteraEmpresarial_Success() throws Exception {
        // Arrange
        List<ContratoActivoClienteEmpresaDTO> contratos = List.of(
                contratoEmpresaDTO,
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-002")
                        .clienteNombre("Empresa A")
                        .clienteEmail("empresa-a@corp.com")
                        .precioAnual(BigDecimal.valueOf(15000))
                        .build()
        );

        when(mockService.reporteCarteraEmpresarial()).thenReturn(contratos);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/cartera-empresarial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.datos", hasSize(2)))
                .andExpect(jsonPath("$.analisisSegmento").exists())
                .andExpect(jsonPath("$.analisisSegmento.clientesUnicos").value(1))
                .andExpect(jsonPath("$.analisisSegmento.ingresosTotales").value(35000.0))
                .andExpect(jsonPath("$.analisisSegmento.ingresoPromedioPorContrato").value(17500.0))
                .andExpect(jsonPath("$.analisisSegmento.segmento").value("EMPRESARIAL"));

        verify(mockService, times(1)).reporteCarteraEmpresarial();
    }

    @Test
    @DisplayName("GET /cartera-empresarial con lista vacía debe retornar info")
    void testGetCarteraEmpresarial_Empty() throws Exception {
        // Arrange
        when(mockService.reporteCarteraEmpresarial()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/cartera-empresarial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(0))
                .andExpect(jsonPath("$.info").value("No hay contratos empresariales activos"));
    }

    // ============== TESTS: GET /contrato/{id} ==============

    @Test
    @DisplayName("GET /contrato/{id} debe retornar contrato específico")
    void testGetContratoEspecifico_Success() throws Exception {
        // Arrange
        when(mockService.obtenerContratoConCliente(1)).thenReturn(contratoClienteDTO);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contrato/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.mensaje").value("Contrato encontrado exitosamente"))
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.datos.numero").value("CTR-001"))
                .andExpect(jsonPath("$.datos.clienteNombre").value("Cliente A"));

        verify(mockService, times(1)).obtenerContratoConCliente(1);
    }

    @Test
    @DisplayName("GET /contrato/{id} con ID inválido debe retornar 400")
    void testGetContratoEspecifico_IdInvalido() throws Exception {
        // Act & Assert - ID negativo
        mockMvc.perform(get("/api/reportes/joins/contrato/-1"))
                .andExpect(status().isBadRequest());

        // Act & Assert - ID cero
        mockMvc.perform(get("/api/reportes/joins/contrato/0"))
                .andExpect(status().isBadRequest());

        // No debería llamar al service
        verify(mockService, never()).obtenerContratoConCliente(anyInt());
    }

    @Test
    @DisplayName("GET /contrato/{id} con ID inexistente debe retornar 404")
    void testGetContratoEspecifico_NotFound() throws Exception {
        // Arrange
        when(mockService.obtenerContratoConCliente(999)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contrato/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Contrato no encontrado"))
                .andExpect(jsonPath("$.detalle").value("No existe contrato con ID: 999"));

        verify(mockService, times(1)).obtenerContratoConCliente(999);
    }

    @Test
    @DisplayName("GET /contrato/{id} con formato inválido retorna 500 (capturado por GlobalExceptionHandler)")
    void testGetContratoEspecifico_FormatoInvalido() throws Exception {
        // Act & Assert - String en lugar de number
        // Spring genera MethodArgumentTypeMismatchException
        // GlobalExceptionHandler lo captura como Exception genérica → 500
        mockMvc.perform(get("/api/reportes/joins/contrato/abc"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));

        // No debería llamar al service (el error ocurre antes en Spring)
        verify(mockService, never()).obtenerContratoConCliente(anyInt());
    }

    // ============== TESTS: GET /estadisticas ==============

    @Test
    @DisplayName("GET /estadisticas debe retornar conteos por estado")
    void testGetEstadisticas_Success() throws Exception {
        // Arrange
        Map<String, Long> estadisticas = new HashMap<>();
        estadisticas.put("VIGENTE", 4L);
        estadisticas.put("VENCIDO", 2L);
        estadisticas.put("CANCELADO", 1L);

        when(mockService.estadisticasContratoPorEstado()).thenReturn(estadisticas);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/estadisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.mensaje").value("Estadísticas obtenidas exitosamente"))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.totalContratos").value(7))
                .andExpect(jsonPath("$.porEstado.VIGENTE").value(4))
                .andExpect(jsonPath("$.porEstado.VENCIDO").value(2))
                .andExpect(jsonPath("$.porEstado.CANCELADO").value(1));

        verify(mockService, times(1)).estadisticasContratoPorEstado();
    }

    @Test
    @DisplayName("GET /estadisticas con map vacío debe retornar 200")
    void testGetEstadisticas_Empty() throws Exception {
        // Arrange
        when(mockService.estadisticasContratoPorEstado()).thenReturn(Collections.emptyMap());

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/estadisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalContratos").value(0))
                .andExpect(jsonPath("$.porEstado", aMapWithSize(0)));
    }

    // ============== TESTS: Content-Type y Headers ==============

    @Test
    @DisplayName("Todas las respuestas deben tener Content-Type: application/json")
    void testContentTypeJSON() throws Exception {
        // Arrange
        when(mockService.reporteContratosActivos()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contratos-activos"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Respuestas OK deben incluir timestamp")
    void testRespuestaInclujeTimestamp() throws Exception {
        // Arrange
        when(mockService.reporteContratosActivos()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contratos-activos"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ============== TESTS: Estructura de Respuesta ==============

    @Test
    @DisplayName("Respuesta exitosa debe tener estructura estándar")
    void testEstructuraRespuestaExitosa() throws Exception {
        // Arrange
        when(mockService.reporteContratosActivos()).thenReturn(List.of(contratoClienteDTO));

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contratos-activos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.cantidad").exists())
                .andExpect(jsonPath("$.datos").exists());
    }

    @Test
    @DisplayName("Respuesta de error debe tener estructura estándar")
    void testEstructuraRespuestaError() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contrato/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isMap());
    }

    // ============== TESTS: Métodos HTTP ==============

    @Test
    @DisplayName("GET /contratos-activos solo debe aceptar GET (POST/PUT/DELETE retornan 500)")
    void testSoloGET_ContratosActivos() throws Exception {
        // Spring genera HttpRequestMethodNotSupportedException
        // GlobalExceptionHandler lo captura como Exception genérica → 500

        // POST no debe estar permitido
        mockMvc.perform(post("/api/reportes/joins/contratos-activos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));

        // PUT no debe estar permitido
        mockMvc.perform(put("/api/reportes/joins/contratos-activos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));

        // DELETE no debe estar permitido
        mockMvc.perform(delete("/api/reportes/joins/contratos-activos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    // ============== TESTS: Análisis en respuestas ==============

    @Test
    @DisplayName("Análisis de cartera debe calcular min/max/promedio correctamente")
    void testAnalisisCarteraCalculos() throws Exception {
        // Arrange
        List<ContratoDetalleDTO> contratos = List.of(
                ContratoDetalleDTO.builder().precioAnual(BigDecimal.valueOf(5000)).build(),
                ContratoDetalleDTO.builder().precioAnual(BigDecimal.valueOf(10000)).build(),
                ContratoDetalleDTO.builder().precioAnual(BigDecimal.valueOf(100000)).build()
        );

        when(mockService.reporteContratosConDetalleCompleto()).thenReturn(contratos);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/contratos-detalle"))
                .andExpect(jsonPath("$.analisisCartera.precioAnualMinimo").value(5000.0))
                .andExpect(jsonPath("$.analisisCartera.precioAnualMaximo").value(100000.0))
                .andExpect(jsonPath("$.analisisCartera.precioAnualTotal").value(115000.0))
                .andExpect(jsonPath("$.analisisCartera.precioAnualPromedio").value(closeTo(38333.33, 1.0)));
    }

    @Test
    @DisplayName("Análisis de segmento debe contar clientes únicos correctamente")
    void testAnalisisSegmentoClientesUnicos() throws Exception {
        // Arrange
        List<ContratoActivoClienteEmpresaDTO> contratos = List.of(
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-001")
                        .clienteNombre("Empresa A")
                        .precioAnual(BigDecimal.valueOf(10000))
                        .build(),
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-002")
                        .clienteNombre("Empresa A")
                        .precioAnual(BigDecimal.valueOf(10000))
                        .build(),
                ContratoActivoClienteEmpresaDTO.builder()
                        .numeroContrato("CTR-003")
                        .clienteNombre("Empresa B")
                        .precioAnual(BigDecimal.valueOf(10000))
                        .build()
        );

        when(mockService.reporteCarteraEmpresarial()).thenReturn(contratos);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/joins/cartera-empresarial"))
                .andExpect(jsonPath("$.analisisSegmento.clientesUnicos").value(2))
                .andExpect(jsonPath("$.analisisSegmento.ingresosTotales").value(30000.0));
    }
}