-- ===================================================================
-- SCHEMA INICIAL PARA POC-01-JOOQ CON POSTGRESQL
-- ===================================================================
-- Nota: Flyway ejecutará este script automáticamente al iniciar la app

-- ===================================================================
-- TABLA: CLIENTES
-- ===================================================================
CREATE TABLE IF NOT EXISTS clientes (
                                        id SERIAL PRIMARY KEY,
                                        nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    telefono VARCHAR(20),
    empresa_id INTEGER DEFAULT 1,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion VARCHAR(50) DEFAULT 'SYSTEM'
    );

COMMENT ON TABLE clientes IS 'Tabla de clientes del sistema';
COMMENT ON COLUMN clientes.id IS 'Identificador único del cliente';
COMMENT ON COLUMN clientes.nombre IS 'Nombre del cliente';
COMMENT ON COLUMN clientes.email IS 'Email del cliente';
COMMENT ON COLUMN clientes.telefono IS 'Teléfono de contacto';

-- ===================================================================
-- TABLA: INSTALACIONES
-- ===================================================================
CREATE TABLE IF NOT EXISTS instalaciones (
                                             id SERIAL PRIMARY KEY,
                                             ubicacion VARCHAR(255) NOT NULL,
    tipo VARCHAR(100),
    descripcion TEXT,
    empresa_id INTEGER DEFAULT 1,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion VARCHAR(50) DEFAULT 'SYSTEM'
    );

COMMENT ON TABLE instalaciones IS 'Tabla de instalaciones de clientes';
COMMENT ON COLUMN instalaciones.id IS 'Identificador único de la instalación';
COMMENT ON COLUMN instalaciones.ubicacion IS 'Ubicación geográfica de la instalación';
COMMENT ON COLUMN instalaciones.tipo IS 'Tipo de instalación (Oficina, Almacén, Planta, etc)';

-- ===================================================================
-- TABLA: CONTRATOS (Principal)
-- ===================================================================
CREATE TABLE IF NOT EXISTS contratos (
                                         id SERIAL PRIMARY KEY,
                                         numero VARCHAR(50) UNIQUE NOT NULL,
    cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    instalacion_id INTEGER REFERENCES instalaciones(id) ON DELETE SET NULL,
    tipo_contrato VARCHAR(50) NOT NULL,
    estado VARCHAR(20) DEFAULT 'VIGENTE' CHECK (estado IN ('VIGENTE', 'VENCIDO', 'CANCELADO', 'SUSPENDIDO')),
    cobertura_material BOOLEAN DEFAULT true,
    cobertura_mano_obra BOOLEAN DEFAULT true,
    cobertura_fin_semana BOOLEAN DEFAULT false,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    precio_anual NUMERIC(12, 2) NOT NULL,
    porcentaje_central INTEGER DEFAULT 70,
    empresa_id INTEGER DEFAULT 1,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion VARCHAR(50) DEFAULT 'SYSTEM',
    fecha_modificacion TIMESTAMP,
    usuario_modificacion VARCHAR(50),
    CONSTRAINT fk_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT fk_instalacion FOREIGN KEY (instalacion_id) REFERENCES instalaciones(id) ON DELETE SET NULL
    );

COMMENT ON TABLE contratos IS 'Tabla principal de contratos de servicio';
COMMENT ON COLUMN contratos.id IS 'Identificador único del contrato';
COMMENT ON COLUMN contratos.numero IS 'Número de contrato único';
COMMENT ON COLUMN contratos.estado IS 'Estado del contrato: VIGENTE, VENCIDO, CANCELADO, SUSPENDIDO';
COMMENT ON COLUMN contratos.precio_anual IS 'Precio anual del contrato en euros';

-- ===================================================================
-- TABLA: PARTES (Detalles de trabajos)
-- ===================================================================
CREATE TABLE IF NOT EXISTS partes (
                                      id SERIAL PRIMARY KEY,
                                      numero VARCHAR(50) UNIQUE NOT NULL,
    contrato_id INTEGER NOT NULL REFERENCES contratos(id) ON DELETE CASCADE,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP,
    descripcion TEXT,
    tipo_trabajo VARCHAR(100),
    estado VARCHAR(20) DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CERRADO', 'CANCELADO')),
    horas_trabajadas NUMERIC(8, 2),
    empresa_id INTEGER DEFAULT 1,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion VARCHAR(50) DEFAULT 'SYSTEM',
    fecha_modificacion TIMESTAMP,
    usuario_modificacion VARCHAR(50),
    CONSTRAINT fk_contrato FOREIGN KEY (contrato_id) REFERENCES contratos(id) ON DELETE CASCADE
    );

COMMENT ON TABLE partes IS 'Tabla de partes de trabajo/incidencias';
COMMENT ON COLUMN partes.id IS 'Identificador único del parte';
COMMENT ON COLUMN partes.numero IS 'Número de parte único';
COMMENT ON COLUMN partes.estado IS 'Estado del parte: ABIERTO, CERRADO, CANCELADO';

-- ===================================================================
-- ÍNDICES PARA PERFORMANCE
-- ===================================================================
CREATE INDEX idx_contratos_cliente_id ON contratos(cliente_id);
CREATE INDEX idx_contratos_estado ON contratos(estado);
CREATE INDEX idx_contratos_fecha_fin ON contratos(fecha_fin);
CREATE INDEX idx_contratos_empresa_id ON contratos(empresa_id);
CREATE INDEX idx_partes_contrato_id ON partes(contrato_id);
CREATE INDEX idx_partes_estado ON partes(estado);
CREATE INDEX idx_clientes_empresa_id ON clientes(empresa_id);
CREATE INDEX idx_instalaciones_empresa_id ON instalaciones(empresa_id);

-- ===================================================================
-- DATOS INICIALES DE PRUEBA
-- ===================================================================

-- Clientes
INSERT INTO clientes (nombre, email, telefono) VALUES
                                                   ('Cliente A - Empresa X', 'contacto@empresa-a.com', '912345678'),
                                                   ('Cliente B - Empresa Y', 'info@empresa-b.com', '934567890'),
                                                   ('Cliente C - Empresa Z', 'contact@empresa-c.es', '955678901')
    ON CONFLICT DO NOTHING;

-- Instalaciones
INSERT INTO instalaciones (ubicacion, tipo, descripcion) VALUES
                                                             ('Madrid - Calle Mayor 1', 'Oficina', 'Oficina central Madrid'),
                                                             ('Barcelona - Av. Diagonal 100', 'Almacén', 'Centro de distribución'),
                                                             ('Valencia - Calle 9 de Octubre 50', 'Planta', 'Planta de producción'),
                                                             ('Bilbao - Gran Vía 25', 'Oficina', 'Oficina regional Euskadi')
    ON CONFLICT DO NOTHING;

-- Contratos
INSERT INTO contratos (numero, cliente_id, instalacion_id, tipo_contrato, estado, fecha_inicio, fecha_fin, precio_anual, porcentaje_central) VALUES
                                                                                                                                                 ('CTR-2024-001', 1, 1, 'MANTENIMIENTO', 'VIGENTE', '2024-01-01', '2025-12-31', 12000.00, 70),
                                                                                                                                                 ('CTR-2024-002', 1, 2, 'SERVICIO', 'VIGENTE', '2024-06-01', '2025-05-31', 18000.00, 65),
                                                                                                                                                 ('CTR-2024-003', 2, 3, 'MANTENIMIENTO', 'VIGENTE', '2024-03-01', '2025-02-28', 15000.00, 70),
                                                                                                                                                 ('CTR-2024-004', 3, 4, 'SERVICIO', 'VIGENTE', '2024-09-01', '2025-08-31', 9000.00, 75),
                                                                                                                                                 ('CTR-2023-001', 1, 1, 'MANTENIMIENTO', 'VENCIDO', '2023-01-01', '2024-01-01', 10000.00, 70)
    ON CONFLICT DO NOTHING;

-- Partes de ejemplo
INSERT INTO partes (numero, contrato_id, fecha_inicio, fecha_fin, descripcion, tipo_trabajo, estado, horas_trabajadas) VALUES
                                                                                                                           ('PARTE-001', 1, '2025-01-10 09:00:00', '2025-01-10 12:30:00', 'Revisión de equipos', 'Revisión Preventiva', 'CERRADO', 3.5),
                                                                                                                           ('PARTE-002', 1, '2025-01-15 14:00:00', '2025-01-15 17:00:00', 'Reparación de software', 'Reparación', 'CERRADO', 3.0),
                                                                                                                           ('PARTE-003', 2, '2025-01-20 08:00:00', NULL, 'Mantenimiento sistemas', 'Mantenimiento', 'ABIERTO', NULL)
    ON CONFLICT DO NOTHING;

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================
-- Después de ejecutar este script, verificá con:
-- SELECT COUNT(*) FROM clientes;
-- SELECT COUNT(*) FROM contratos;
-- SELECT COUNT(*) FROM partes;