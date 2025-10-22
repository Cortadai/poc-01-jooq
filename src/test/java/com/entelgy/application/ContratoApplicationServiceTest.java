package com.entelgy.application;

import com.entelgy.application.dto.ContratoResponse;
import com.entelgy.application.dto.CrearContratoRequest;
import com.entelgy.application.mapper.ContratoMapper;
import com.entelgy.domain.model.Contrato;
import com.entelgy.infrastructure.repository.ClienteJooqRepository;
import com.entelgy.infrastructure.repository.ContratoJooqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests UNITARIOS para ContratoApplicationService
 *
 * Estrategia:
 * - ✅ Tests unitarios con MOCKS (sin BD real)
 * - ✅ Rápidos: <100ms por test
 * - ✅ Enfoque: Lógica del service
 * - ✅ Comportamiento esperado
 *
 * Cobertura:
 * 1. Happy path (crear, obtener, listar)
 * 2. Error handling (cliente no existe, datos inválidos)
 * 3. Business rules (esValido, próximo a vencer, vencido)
 * 4. Stream mapping (toResponse)
 */
@ExtendWith(MockitoExtension.class)
class ContratoApplicationServiceTest {

    @Mock
    private ContratoJooqRepository contratoRepository;

    @Mock
    private ClienteJooqRepository clienteRepository;

    @Mock
    private ContratoMapper contratoMapper;

    @InjectMocks
    private ContratoApplicationService service;

    // Test Data
    private CrearContratoRequest crearContratoRequest;
    private Contrato contratoDomain;
    private ContratoResponse contratoResponse;

    @BeforeEach
    void setup() {
        // Datos de entrada
        crearContratoRequest = CrearContratoRequest.builder()
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

        // Modelo de dominio
        contratoDomain = Contrato.builder()
                .id(1)
                .numero("CTR-001")
                .clienteId(1)
                .instalacionId(1)
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

        // Respuesta DTO
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

    // ============== TESTS DE CREAR ==============

    @Test
    @DisplayName("crear debe guardar contrato válido y retornar respuesta")
    void testCrearContratoValido() {
        // Arrange
        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(new com.entelgy.domain.model.Cliente()));
        when(contratoMapper.toDomain(crearContratoRequest))
                .thenReturn(contratoDomain);
        when(contratoRepository.save(contratoDomain))
                .thenReturn(contratoDomain);
        when(contratoMapper.toResponse(contratoDomain))
                .thenReturn(contratoResponse);

        // Act
        ContratoResponse resultado = service.crear(crearContratoRequest);

        // Assert
        assertNotNull(resultado, "La respuesta no debe ser nula");
        assertEquals("CTR-001", resultado.getNumero());
        assertEquals("VIGENTE", resultado.getEstado());
        assertEquals(1L, resultado.getId());

        // Verificar que se llamó a los mocks correctamente
        verify(clienteRepository, times(1)).findById(1L);
        verify(contratoMapper, times(1)).toDomain(crearContratoRequest);
        verify(contratoRepository, times(1)).save(contratoDomain);
        verify(contratoMapper, times(1)).toResponse(contratoDomain);
    }

    @Test
    @DisplayName("crear debe lanzar excepción si cliente no existe")
    void testCrearContratoClienteNoExiste() {
        // Arrange
        when(clienteRepository.findById(1L))
                .thenReturn(Optional.empty());  // Cliente no existe

        // Act & Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(crearContratoRequest),
                "Debe lanzar IllegalArgumentException"
        );

        assertTrue(excepcion.getMessage().contains("Cliente no existe"),
                "Mensaje debe mencionar que cliente no existe");

        // Verificar que NO se guardó nada
        verify(contratoRepository, never()).save(any());
        verify(contratoMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("crear debe lanzar excepción si contrato es inválido")
    void testCrearContratoInvalido() {
        // Arrange
        Contrato contratoInvalido = Contrato.builder()
                .numero(null)  // ❌ Número nulo
                .clienteId(1)
                .instalacionId(1)
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(12000))
                .build();

        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(new com.entelgy.domain.model.Cliente()));
        when(contratoMapper.toDomain(crearContratoRequest))
                .thenReturn(contratoInvalido);

        // Act & Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(crearContratoRequest),
                "Debe lanzar excepción por datos inválidos"
        );

        assertTrue(excepcion.getMessage().contains("Contrato no válido"),
                "Mensaje debe mencionar que contrato es inválido");

        // Verificar que NO se guardó
        verify(contratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear debe rechazar contrato con fechas inválidas")
    void testCrearContratoFechasInvalidas() {
        // Arrange
        Contrato contratoFechasInvalidas = Contrato.builder()
                .numero("CTR-001")
                .clienteId(1)
                .instalacionId(1)
                .fechaInicio(LocalDate.now().plusYears(1))  // ❌ Inicio después de fin
                .fechaFin(LocalDate.now())
                .precioAnual(BigDecimal.valueOf(12000))
                .build();

        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(new com.entelgy.domain.model.Cliente()));
        when(contratoMapper.toDomain(crearContratoRequest))
                .thenReturn(contratoFechasInvalidas);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(crearContratoRequest));

        verify(contratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear debe rechazar contrato con precio negativo")
    void testCrearContratoPrecioNegativo() {
        // Arrange
        Contrato contratoPrecioNegativo = Contrato.builder()
                .numero("CTR-001")
                .clienteId(1)
                .instalacionId(1)
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(1))
                .precioAnual(BigDecimal.valueOf(-1000))  // ❌ Negativo
                .build();

        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(new com.entelgy.domain.model.Cliente()));
        when(contratoMapper.toDomain(crearContratoRequest))
                .thenReturn(contratoPrecioNegativo);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(crearContratoRequest));

        verify(contratoRepository, never()).save(any());
    }

    // ============== TESTS DE OBTENER ==============

    @Test
    @DisplayName("obtenerPorId debe retornar contrato cuando existe")
    void testObtenerPorIdExiste() {
        // Arrange
        when(contratoRepository.findById(1L))
                .thenReturn(Optional.of(contratoDomain));
        when(contratoMapper.toResponse(contratoDomain))
                .thenReturn(contratoResponse);

        // Act
        ContratoResponse resultado = service.obtenerPorId(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals("CTR-001", resultado.getNumero());
        assertEquals(1L, resultado.getId());

        verify(contratoRepository, times(1)).findById(1L);
        verify(contratoMapper, times(1)).toResponse(contratoDomain);
    }

    @Test
    @DisplayName("obtenerPorId debe lanzar excepción si no existe")
    void testObtenerPorIdNoExiste() {
        // Arrange
        when(contratoRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> service.obtenerPorId(999L)
        );

        assertTrue(excepcion.getMessage().contains("Contrato no encontrado"));
        verify(contratoRepository, times(1)).findById(999L);
        verify(contratoMapper, never()).toResponse(any());
    }

    // ============== TESTS DE LISTAR ==============

    @Test
    @DisplayName("listarActivos debe retornar lista de contratos")
    void testListarActivos() {
        // Arrange
        List<Contrato> contratos = List.of(
                contratoDomain,
                Contrato.builder()
                        .id(2)
                        .numero("CTR-002")
                        .estado("VIGENTE")
                        .build()
        );

        List<ContratoResponse> respuestas = List.of(
                contratoResponse,
                ContratoResponse.builder()
                        .id(2L)
                        .numero("CTR-002")
                        .estado("VIGENTE")
                        .build()
        );

        when(contratoRepository.findAllActivos())
                .thenReturn(contratos);
        when(contratoMapper.toResponse(contratos.get(0)))
                .thenReturn(respuestas.get(0));
        when(contratoMapper.toResponse(contratos.get(1)))
                .thenReturn(respuestas.get(1));

        // Act
        List<ContratoResponse> resultado = service.listarActivos();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("CTR-001", resultado.get(0).getNumero());
        assertEquals("CTR-002", resultado.get(1).getNumero());

        verify(contratoRepository, times(1)).findAllActivos();
        verify(contratoMapper, times(2)).toResponse(any());
    }

    @Test
    @DisplayName("listarActivos debe retornar lista vacía si no hay contratos")
    void testListarActivosVacio() {
        // Arrange
        when(contratoRepository.findAllActivos())
                .thenReturn(List.of());

        // Act
        List<ContratoResponse> resultado = service.listarActivos();

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(contratoRepository, times(1)).findAllActivos();
    }

    @Test
    @DisplayName("listarPorCliente debe retornar contratos del cliente")
    void testListarPorCliente() {
        // Arrange
        List<Contrato> contratos = List.of(contratoDomain);
        List<ContratoResponse> respuestas = List.of(contratoResponse);

        when(contratoRepository.findByClienteId(1L))
                .thenReturn(contratos);
        when(contratoMapper.toResponse(contratoDomain))
                .thenReturn(contratoResponse);

        // Act
        List<ContratoResponse> resultado = service.listarPorCliente(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getClienteId());

        verify(contratoRepository, times(1)).findByClienteId(1L);
    }

    @Test
    @DisplayName("obtenerProximosAVencer debe retornar contratos próximos a vencer")
    void testObtenerProximosAVencer() {
        // Arrange - Contrato próximo a vencer (15 días)
        Contrato contratoProximo = Contrato.builder()
                .id(1)
                .numero("CTR-PROX")
                .fechaFin(LocalDate.now().plusDays(15))
                .build();

        ContratoResponse responseProximo = ContratoResponse.builder()
                .id(1L)
                .numero("CTR-PROX")
                .proximoAVencer(true)
                .vencido(false)
                .build();

        when(contratoRepository.findProximosAVencer())
                .thenReturn(List.of(contratoProximo));
        when(contratoMapper.toResponse(contratoProximo))
                .thenReturn(responseProximo);

        // Act
        List<ContratoResponse> resultado = service.obtenerProximosAVencer();

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getProximoAVencer());

        verify(contratoRepository, times(1)).findProximosAVencer();
    }

    @Test
    @DisplayName("obtenerVencidos debe retornar contratos vencidos")
    void testObtenerVencidos() {
        // Arrange - Contrato vencido
        Contrato contratoVencido = Contrato.builder()
                .id(1)
                .numero("CTR-VENCIDO")
                .fechaFin(LocalDate.now().minusDays(10))
                .build();

        ContratoResponse responseVencido = ContratoResponse.builder()
                .id(1L)
                .numero("CTR-VENCIDO")
                .proximoAVencer(false)
                .vencido(true)
                .build();

        when(contratoRepository.findVencidos())
                .thenReturn(List.of(contratoVencido));
        when(contratoMapper.toResponse(contratoVencido))
                .thenReturn(responseVencido);

        // Act
        List<ContratoResponse> resultado = service.obtenerVencidos();

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getVencido());

        verify(contratoRepository, times(1)).findVencidos();
    }

    // ============== TESTS DE BÚSQUEDA ==============

    @Test
    @DisplayName("buscar debe aplicar filtros correctamente")
    void testBuscarConFiltros() {
        // Arrange
        List<Contrato> contratos = List.of(contratoDomain);
        List<ContratoResponse> respuestas = List.of(contratoResponse);

        when(contratoRepository.findByFiltros(1L, "MANTENIMIENTO", "VIGENTE"))
                .thenReturn(contratos);
        when(contratoMapper.toResponse(contratoDomain))
                .thenReturn(contratoResponse);

        // Act
        List<ContratoResponse> resultado = service.buscar(1L, "MANTENIMIENTO", "VIGENTE");

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        verify(contratoRepository, times(1))
                .findByFiltros(1L, "MANTENIMIENTO", "VIGENTE");
    }

    @Test
    @DisplayName("buscar debe soportar filtros nulos")
    void testBuscarSinFiltros() {
        // Arrange
        List<Contrato> contratos = List.of(contratoDomain);
        List<ContratoResponse> respuestas = List.of(contratoResponse);

        when(contratoRepository.findByFiltros(null, null, null))
                .thenReturn(contratos);
        when(contratoMapper.toResponse(contratoDomain))
                .thenReturn(contratoResponse);

        // Act
        List<ContratoResponse> resultado = service.buscar(null, null, null);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        verify(contratoRepository, times(1))
                .findByFiltros(null, null, null);
    }

    // ============== TESTS DE VALIDACIÓN DE NEGOCIO ==============

    @Test
    @DisplayName("Contrato.esValido debe validar campos requeridos")
    void testContratoEsValido() {
        // Arrange & Act
        boolean esValido = contratoDomain.esValido();

        // Assert
        assertTrue(esValido, "Contrato con datos válidos debe pasar validación");
    }

    @Test
    @DisplayName("Contrato.proximoAVencer debe detectar contratos próximos a vencer")
    void testContratoProximoAVencer() {
        // Arrange
        Contrato contratoProximo = Contrato.builder()
                .fechaFin(LocalDate.now().plusDays(15))  // 15 días
                .build();

        // Act
        boolean proximoAVencer = contratoProximo.proximoAVencer();

        // Assert
        assertTrue(proximoAVencer, "Contrato con vencimiento en 15 días debe detectarse como próximo");
    }

    @Test
    @DisplayName("Contrato.estaVencido debe detectar contratos vencidos")
    void testContratoEstaVencido() {
        // Arrange
        Contrato contratoVencido = Contrato.builder()
                .fechaFin(LocalDate.now().minusDays(10))  // Vencido hace 10 días
                .build();

        // Act
        boolean estaVencido = contratoVencido.estaVencido();

        // Assert
        assertTrue(estaVencido, "Contrato con fecha fin en el pasado debe estar vencido");
    }

    @Test
    @DisplayName("Contrato.estaVencido debe retornar false para contratos vigentes")
    void testContratoNoEstaVencido() {
        // Arrange
        Contrato contratoVigente = Contrato.builder()
                .fechaFin(LocalDate.now().plusYears(1))  // Vence en 1 año
                .build();

        // Act
        boolean estaVencido = contratoVigente.estaVencido();

        // Assert
        assertFalse(estaVencido, "Contrato futuro NO debe estar vencido");
    }
}