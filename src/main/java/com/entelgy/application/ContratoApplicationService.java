package com.entelgy.application;

import com.entelgy.application.dto.ContratoResponse;
import com.entelgy.application.dto.CrearContratoRequest;
import com.entelgy.application.mapper.ContratoMapper;
import com.entelgy.domain.model.Contrato;
import com.entelgy.infrastructure.repository.ClienteJooqRepository;
import com.entelgy.infrastructure.repository.ContratoJooqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application Service para Contratos
 *
 * Responsabilidades:
 * - Orquestar el flujo de operación
 * - Validar datos de entrada
 * - Llamar a domain service
 * - Mapear DTOs
 * - Manejo de transacciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContratoApplicationService {

    private final ContratoJooqRepository contratoRepository;
    private final ClienteJooqRepository clienteRepository;
    private final ContratoMapper contratoMapper;

    /**
     * Crea un nuevo contrato
     *
     * Flujo:
     * 1. Validar entrada (javax.validation hace esto en controller)
     * 2. Validar que cliente existe
     * 3. Mapear DTO a entidad
     * 4. Validar reglas de negocio
     * 5. Guardar en BD
     * 6. Retornar respuesta
     */
    @Transactional
    public ContratoResponse crear(CrearContratoRequest request) {
        log.info("Creando nuevo contrato: {}", request.getNumero());

        // Validar que cliente existe
        if (!clienteRepository.findById(request.getClienteId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Cliente no existe: " + request.getClienteId()
            );
        }

        // Convertir DTO a entidad de negocio
        Contrato contrato = contratoMapper.toDomain(request);

        // Validar reglas de negocio
        if (!contrato.esValido()) {
            throw new IllegalArgumentException(
                    "Contrato no válido: fechas o datos incompletos"
            );
        }

        // Persistir
        Contrato contratoGuardado = contratoRepository.save(contrato);

        log.info("Contrato creado exitosamente: ID={}", contratoGuardado.getId());

        // Convertir a respuesta
        return contratoMapper.toResponse(contratoGuardado);
    }

    /**
     * Obtiene contrato por ID
     */
    @Transactional(readOnly = true)
    public ContratoResponse obtenerPorId(Long id) {
        log.debug("Obteniendo contrato: {}", id);

        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado: " + id));

        return contratoMapper.toResponse(contrato);
    }

    /**
     * Lista todos los contratos activos
     */
    @Transactional(readOnly = true)
    public List<ContratoResponse> listarActivos() {
        log.debug("Listando contratos activos");

        return contratoRepository.findAllActivos().stream()
                .map(contratoMapper::toResponse)
                .toList();
    }

    /**
     * Lista contratos por cliente
     */
    @Transactional(readOnly = true)
    public List<ContratoResponse> listarPorCliente(Long clienteId) {
        log.debug("Listando contratos para cliente: {}", clienteId);

        return contratoRepository.findByClienteId(clienteId).stream()
                .map(contratoMapper::toResponse)
                .toList();
    }

    /**
     * Obtiene contratos próximos a vencer
     */
    @Transactional(readOnly = true)
    public List<ContratoResponse> obtenerProximosAVencer() {
        log.debug("Obteniendo contratos próximos a vencer");

        return contratoRepository.findProximosAVencer().stream()
                .map(contratoMapper::toResponse)
                .toList();
    }

    /**
     * Obtiene contratos vencidos
     */
    @Transactional(readOnly = true)
    public List<ContratoResponse> obtenerVencidos() {
        log.debug("Obteniendo contratos vencidos");

        return contratoRepository.findVencidos().stream()
                .map(contratoMapper::toResponse)
                .toList();
    }

    /**
     * Búsqueda avanzada con filtros
     */
    @Transactional(readOnly = true)
    public List<ContratoResponse> buscar(Long clienteId, String tipoContrato, String estado) {
        log.debug("Buscando contratos con filtros");

        return contratoRepository.findByFiltros(clienteId, tipoContrato, estado).stream()
                .map(contratoMapper::toResponse)
                .toList();
    }
}
