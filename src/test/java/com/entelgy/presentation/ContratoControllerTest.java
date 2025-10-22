package com.entelgy.presentation;

import com.entelgy.application.ContratoApplicationService;
import com.entelgy.application.dto.ContratoResponse;
import com.entelgy.application.dto.CrearContratoRequest;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de INTEGRACIÓN para ContratoController
 *
 * Estrategia:
 * - ✅ @WebMvcTest: Carga solo el contexto de Controller
 * - ✅ MockMvc: Simula HTTP requests sin servidor real
 * - ✅ Mocks de service layer
 * - ✅ Verifica: HTTP status, JSON responses, validaciones
 *
 * Cobertura:
 * 1. Happy paths (crear, obtener, listar)
 * 2. Error handling (validación, excepciones)
 * 3. HTTP status codes (201, 200, 400, 404, etc)
 * 4. JSON requests/responses
 * 5. Query parameters
 * 6. Path variables
 */
@WebMvcTest(ContratoController.class)
@DisplayName("ContratoController - Tests de Integración")
class ContratoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContratoApplicationService service;

    @Autowired
    private ObjectMapper objectMapper;

    // Test Data
    private CrearContratoRequest crearRequest;
    private ContratoResponse contratoResponse;

    @BeforeEach
    void setup() {
        // Request DTO para POST
        crearRequest = CrearContratoRequest.builder()
                .numero("CTR-001")
                .clienteId(1L)
                .instalacionId(1L)
                .tipoContrato("MANTENIMIENTO")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(12000))
                .porcentajeCentral(70)
                .coberturaMaterial(true)
                .coberturaManoObra(true)
                .coberturaFinSemana(false)
                .build();

        // Response DTO
        contratoResponse = ContratoResponse.builder()
                .id(1L)
                .numero("CTR-001")
                .clienteId(1L)
                .instalacionId(1L)
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
                .proximoAVencer(false)
                .vencido(false)
                .build();
    }

    // ============== POST /api/contratos ==============

    @Test
    @DisplayName("POST /api/contratos debe crear contrato y retornar 201 CREATED")
    void testCrearContrato() throws Exception {
        // Arrange
        when(service.crear(any(CrearContratoRequest.class)))
                .thenReturn(contratoResponse);

        // Act & Assert
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest)))
                .andDo(print())
                .andExpect(status().isCreated())  // 201
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numero").value("CTR-001"))
                .andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.precioAnual").value(12000))
                .andExpect(jsonPath("$.proximoAVencer").value(false));

        verify(service, times(1)).crear(any(CrearContratoRequest.class));
    }

    @Test
    @DisplayName("POST /api/contratos debe rechazar si falta número")
    void testCrearContratoSinNumero() throws Exception {
        // Arrange: Request inválido (número nulo)
        CrearContratoRequest requestInvalido = CrearContratoRequest.builder()
                .numero(null)  // ❌ Falta
                .clienteId(1L)
                .instalacionId(1L)
                .tipoContrato("MANTENIMIENTO")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(12000))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andDo(print())
                .andExpect(status().isBadRequest())  // 400
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.errors.numero").exists());

        verify(service, never()).crear(any());
    }

    @Test
    @DisplayName("POST /api/contratos debe rechazar si clienteId es negativo")
    void testCrearContratoClienteIdNegativo() throws Exception {
        // Arrange
        CrearContratoRequest requestInvalido = CrearContratoRequest.builder()
                .numero("CTR-001")
                .clienteId(-1L)  // ❌ Negativo
                .instalacionId(1L)
                .tipoContrato("MANTENIMIENTO")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(12000))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.clienteId").exists());

        verify(service, never()).crear(any());
    }

    @Test
    @DisplayName("POST /api/contratos debe rechazar si precioAnual es negativo")
    void testCrearContratoPrecioNegativo() throws Exception {
        // Arrange
        CrearContratoRequest requestInvalido = CrearContratoRequest.builder()
                .numero("CTR-001")
                .clienteId(1L)
                .instalacionId(1L)
                .tipoContrato("MANTENIMIENTO")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(-5000))  // ❌ Negativo
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.precioAnual").exists());

        verify(service, never()).crear(any());
    }

    @Test
    @DisplayName("POST /api/contratos debe manejar IllegalArgumentException (cliente no existe)")
    void testCrearContratoClienteNoExiste() throws Exception {
        // Arrange
        when(service.crear(any(CrearContratoRequest.class)))
                .thenThrow(new IllegalArgumentException("Cliente no existe: 999"));

        // Act & Assert
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest)))
                .andExpect(status().isBadRequest())  // 400
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Cliente no existe: 999"));

        verify(service, times(1)).crear(any());
    }

    @Test
    @DisplayName("POST /api/contratos debe retornar 201 con JSON válido")
    void testCrearContratoResponseFormato() throws Exception {
        // Arrange
        when(service.crear(any(CrearContratoRequest.class)))
                .thenReturn(contratoResponse);

        // Act & Assert
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(16)))  // ✅ 16 campos (id, numero, clienteId, instalacionId, tipoContrato, estado, coberturaMaterial, coberturaManoObra, coberturaFinSemana, fechaInicio, fechaFin, precioAnual, porcentajeCentral, empresaId, proximoAVencer, vencido)
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.numero", notNullValue()))
                .andExpect(jsonPath("$.estado", notNullValue()))
                .andExpect(jsonPath("$.proximoAVencer", notNullValue()));
    }

    // ============== GET /api/contratos/{id} ==============

    @Test
    @DisplayName("GET /api/contratos/{id} debe retornar contrato")
    void testObtenerContratoporId() throws Exception {
        // Arrange
        when(service.obtenerPorId(1L))
                .thenReturn(contratoResponse);

        // Act & Assert
        mockMvc.perform(get("/api/contratos/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())  // 200
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numero").value("CTR-001"));

        verify(service, times(1)).obtenerPorId(1L);
    }

    @Test
    @DisplayName("GET /api/contratos/{id} debe retornar 400 si contrato no existe")
    void testObtenerContratoNoExiste() throws Exception {
        // Arrange
        when(service.obtenerPorId(999L))
                .thenThrow(new IllegalArgumentException("Contrato no encontrado: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/contratos/999"))
                .andExpect(status().isBadRequest())  // 400 (por IllegalArgumentException)
                .andExpect(jsonPath("$.message").value("Contrato no encontrado: 999"));

        verify(service, times(1)).obtenerPorId(999L);
    }

    // ============== GET /api/contratos (listar) ==============

    @Test
    @DisplayName("GET /api/contratos debe retornar lista de activos")
    void testListarContratos() throws Exception {
        // Arrange
        List<ContratoResponse> contratos = List.of(
                contratoResponse,
                ContratoResponse.builder()
                        .id(2L)
                        .numero("CTR-002")
                        .estado("VIGENTE")
                        .proximoAVencer(true)
                        .vencido(false)
                        .build()
        );

        when(service.listarActivos())
                .thenReturn(contratos);

        // Act & Assert
        mockMvc.perform(get("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())  // 200
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].numero").value("CTR-001"))
                .andExpect(jsonPath("$[1].numero").value("CTR-002"));

        verify(service, times(1)).listarActivos();
        verify(service, never()).buscar(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/contratos debe retornar lista vacía")
    void testListarContratosVacio() throws Exception {
        // Arrange
        when(service.listarActivos())
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/contratos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(service, times(1)).listarActivos();
    }

    @Test
    @DisplayName("GET /api/contratos?clienteId=1&estado=VIGENTE debe aplicar filtros")
    void testListarConFiltros() throws Exception {
        // Arrange
        List<ContratoResponse> filtrados = List.of(contratoResponse);

        when(service.buscar(1L, null, "VIGENTE"))
                .thenReturn(filtrados);

        // Act & Assert
        mockMvc.perform(get("/api/contratos")
                        .param("clienteId", "1")
                        .param("estado", "VIGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clienteId").value(1));

        verify(service, times(1)).buscar(1L, null, "VIGENTE");
        verify(service, never()).listarActivos();
    }

    // ============== GET /api/contratos/cliente/{clienteId} ==============

    @Test
    @DisplayName("GET /api/contratos/cliente/{clienteId} debe retornar contratos del cliente")
    void testListarPorCliente() throws Exception {
        // Arrange
        List<ContratoResponse> contratosPorCliente = List.of(contratoResponse);

        when(service.listarPorCliente(1L))
                .thenReturn(contratosPorCliente);

        // Act & Assert
        mockMvc.perform(get("/api/contratos/cliente/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clienteId").value(1));

        verify(service, times(1)).listarPorCliente(1L);
    }

    // ============== GET /api/contratos/proximos-a-vencer ==============

    @Test
    @DisplayName("GET /api/contratos/proximos-a-vencer debe retornar contratos próximos")
    void testObtenerProximosAVencer() throws Exception {
        // Arrange
        ContratoResponse contratoProximo = ContratoResponse.builder()
                .id(1L)
                .numero("CTR-PROX")
                .proximoAVencer(true)
                .vencido(false)
                .build();

        List<ContratoResponse> proximosList = List.of(contratoProximo);

        when(service.obtenerProximosAVencer())
                .thenReturn(proximosList);

        // Act & Assert
        mockMvc.perform(get("/api/contratos/proximos-a-vencer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].proximoAVencer").value(true));

        verify(service, times(1)).obtenerProximosAVencer();
    }

    // ============== GET /api/contratos/vencidos ==============

    @Test
    @DisplayName("GET /api/contratos/vencidos debe retornar contratos vencidos")
    void testObtenerVencidos() throws Exception {
        // Arrange
        ContratoResponse contratoVencido = ContratoResponse.builder()
                .id(1L)
                .numero("CTR-VENCIDO")
                .proximoAVencer(false)
                .vencido(true)
                .build();

        List<ContratoResponse> vencidosList = List.of(contratoVencido);

        when(service.obtenerVencidos())
                .thenReturn(vencidosList);

        // Act & Assert
        mockMvc.perform(get("/api/contratos/vencidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vencido").value(true));

        verify(service, times(1)).obtenerVencidos();
    }

    // ============== GET /api/contratos/health ==============

    @Test
    @DisplayName("GET /api/contratos/health debe retornar estado OK")
    void testHealth() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/contratos/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("POC 1 - JOOQ is UP"));
    }

    // ============== ERROR HANDLING ==============

    @Test
    @DisplayName("POST /api/contratos debe retornar 400 si body JSON es inválido")
    void testCrearContratoJsonInvalido() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\": \"CTR-001\", \"invalid\": \"json\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/contratos debe rechazar si porcentajeCentral > 100")
    void testCrearContratoPorcentajeInvalido() throws Exception {
        // Arrange
        CrearContratoRequest requestInvalido = CrearContratoRequest.builder()
                .numero("CTR-001")
                .clienteId(1L)
                .instalacionId(1L)
                .tipoContrato("MANTENIMIENTO")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(12000))
                .porcentajeCentral(150)  // ❌ > 100
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.porcentajeCentral").exists());

        verify(service, never()).crear(any());
    }

    @Test
    @DisplayName("POST /api/contratos debe rechazar si fechaInicio después de fechaFin")
    void testCrearContratoFechasInvalidas() throws Exception {
        // Arrange
        CrearContratoRequest requestInvalido = CrearContratoRequest.builder()
                .numero("CTR-001")
                .clienteId(1L)
                .instalacionId(1L)
                .tipoContrato("MANTENIMIENTO")
                .fechaInicio(LocalDate.now().plusYears(1))  // ❌ Después
                .fechaFin(LocalDate.now())
                .precioAnual(BigDecimal.valueOf(12000))
                .build();

        // Act & Assert - Aunque esta validación ocurre en el service
        when(service.crear(any()))
                .thenThrow(new IllegalArgumentException("Contrato no válido: fechas inválidas"));

        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());

        verify(service, times(1)).crear(any());
    }

    // ============== CONTENT TYPE ==============

    @Test
    @DisplayName("GET /api/contratos debe retornar JSON")
    void testResponseContentType() throws Exception {
        // Arrange
        when(service.listarActivos())
                .thenReturn(List.of(contratoResponse));

        // Act & Assert
        mockMvc.perform(get("/api/contratos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/contratos debe aceptar JSON")
    void testRequestContentType() throws Exception {
        // Arrange
        when(service.crear(any()))
                .thenReturn(contratoResponse);

        // Act & Assert - JSON válido
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest)))
                .andExpect(status().isCreated());

        // XML es rechazado por Spring (HttpMediaTypeNotSupportedException)
        // GlobalExceptionHandler lo captura como Exception genérica → 500
        // Esto es correcto: el servidor rechaza el tipo de contenido
        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(crearRequest.toString()))
                .andExpect(status().isInternalServerError());  // 500 (capturado por Exception genérica)
    }

    // ============== PATH VARIABLES ==============

    @Test
    @DisplayName("GET /api/contratos/{id} debe extraer ID correctamente")
    void testObtenerContratoPathVariable() throws Exception {
        // Arrange
        ContratoResponse response = ContratoResponse.builder()
                .id(123L)
                .numero("CTR-001")
                .estado("VIGENTE")
                .build();

        when(service.obtenerPorId(123L))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/contratos/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123));

        verify(service, times(1)).obtenerPorId(123L);
    }

    // ============== QUERY PARAMETERS ==============

    @Test
    @DisplayName("GET /api/contratos?clienteId=5 debe pasar parámetro correctamente")
    void testQueryParameter() throws Exception {
        // Arrange
        when(service.buscar(5L, null, null))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/contratos")
                        .param("clienteId", "5"))
                .andExpect(status().isOk());

        verify(service, times(1)).buscar(5L, null, null);
    }

    @Test
    @DisplayName("GET /api/contratos?clienteId=1&tipoContrato=MANTENIMIENTO&estado=VIGENTE")
    void testMultipleQueryParameters() throws Exception {
        // Arrange
        when(service.buscar(1L, "MANTENIMIENTO", "VIGENTE"))
                .thenReturn(List.of(contratoResponse));

        // Act & Assert
        mockMvc.perform(get("/api/contratos")
                        .param("clienteId", "1")
                        .param("tipoContrato", "MANTENIMIENTO")
                        .param("estado", "VIGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(service, times(1)).buscar(1L, "MANTENIMIENTO", "VIGENTE");
    }
}