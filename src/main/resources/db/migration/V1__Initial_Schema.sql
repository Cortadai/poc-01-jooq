-- POC 1: JOOQ + SQL Server
-- Flyway Migration: V1__Initial_Schema.sql

-- Tabla de Clientes
CREATE TABLE clientes (
                          id BIGINT PRIMARY KEY IDENTITY(1,1),
                          nombre NVARCHAR(255) NOT NULL,
                          email NVARCHAR(255),
                          telefono NVARCHAR(20),
                          empresa_id INT NOT NULL DEFAULT 1,
                          delegacion_id INT NOT NULL DEFAULT 1,
                          estado NVARCHAR(50) NOT NULL DEFAULT 'ACTIVO', -- ACTIVO, INACTIVO
                          fecha_creacion DATETIME NOT NULL DEFAULT GETDATE(),
                          usuario_creacion NVARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
                          fecha_modificacion DATETIME,
                          usuario_modificacion NVARCHAR(100),
                          CONSTRAINT uk_clientes_email UNIQUE (email)
);

-- Tabla de Instalaciones (vinculadas a clientes)
CREATE TABLE instalaciones (
                               id BIGINT PRIMARY KEY IDENTITY(1,1),
                               cliente_id BIGINT NOT NULL,
                               codigo NVARCHAR(100) NOT NULL,
                               descripcion NVARCHAR(255),
                               direccion NVARCHAR(500),
                               codigo_postal NVARCHAR(10),
                               zona NVARCHAR(50),
                               estado NVARCHAR(50) NOT NULL DEFAULT 'ACTIVO',
                               fecha_creacion DATETIME NOT NULL DEFAULT GETDATE(),
                               usuario_creacion NVARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
                               CONSTRAINT fk_instalaciones_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
                               CONSTRAINT uk_instalaciones_codigo UNIQUE (codigo)
);

-- Tabla de Contratos
CREATE TABLE contratos (
                           id BIGINT PRIMARY KEY IDENTITY(1,1),
                           numero NVARCHAR(100) NOT NULL,
                           cliente_id BIGINT NOT NULL,
                           instalacion_id BIGINT NOT NULL,
                           tipo_contrato NVARCHAR(50) NOT NULL, -- MANTENIMIENTO, REPARACION, REVISION
                           estado NVARCHAR(50) NOT NULL DEFAULT 'VIGENTE', -- VIGENTE, VENCIDO, CANCELADO, RENOVADO
                           cobertura_material BIT NOT NULL DEFAULT 1,
                           cobertura_mano_obra BIT NOT NULL DEFAULT 1,
                           cobertura_fin_semana BIT NOT NULL DEFAULT 0,
                           fecha_inicio DATE NOT NULL,
                           fecha_fin DATE NOT NULL,
                           precio_anual DECIMAL(10, 2) NOT NULL,
                           porcentaje_central INT NOT NULL DEFAULT 70,
                           empresa_id INT NOT NULL DEFAULT 1,
                           fecha_creacion DATETIME NOT NULL DEFAULT GETDATE(),
                           usuario_creacion NVARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
                           fecha_modificacion DATETIME,
                           usuario_modificacion NVARCHAR(100),
                           CONSTRAINT fk_contratos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
                           CONSTRAINT fk_contratos_instalacion FOREIGN KEY (instalacion_id) REFERENCES instalaciones(id),
                           CONSTRAINT uk_contratos_numero UNIQUE (numero)
);

-- Tabla de Historial de Contratos (auditoría)
CREATE TABLE contratos_historico (
                                     id BIGINT PRIMARY KEY IDENTITY(1,1),
                                     contrato_id BIGINT NOT NULL,
                                     accion NVARCHAR(50), -- CREADO, MODIFICADO, RENOVADO, CANCELADO
                                     estado_anterior NVARCHAR(50),
                                     estado_nuevo NVARCHAR(50),
                                     usuario NVARCHAR(100),
                                     fecha DATETIME DEFAULT GETDATE(),
                                     descripcion NVARCHAR(1000),
                                     CONSTRAINT fk_historico_contrato FOREIGN KEY (contrato_id) REFERENCES contratos(id)
);

-- Tabla de Partes (intervenciones técnicas)
CREATE TABLE partes (
                        id BIGINT PRIMARY KEY IDENTITY(1,1),
                        numero_parte NVARCHAR(100) NOT NULL,
                        contrato_id BIGINT NOT NULL,
                        cliente_id BIGINT NOT NULL,
                        instalacion_id BIGINT NOT NULL,
                        tipo_parte NVARCHAR(50) NOT NULL, -- AVERIA, REVISION, INSTALACION, MANTENIMIENTO
                        estado NVARCHAR(50) NOT NULL DEFAULT 'ABIERTO', -- ABIERTO, CERRADO, CANCELADO
                        descripcion NVARCHAR(1000),
                        hora_inicio DATETIME,
                        hora_fin DATETIME,
                        tecnico_id BIGINT,
                        fecha_creacion DATETIME NOT NULL DEFAULT GETDATE(),
                        usuario_creacion NVARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
                        CONSTRAINT fk_partes_contrato FOREIGN KEY (contrato_id) REFERENCES contratos(id),
                        CONSTRAINT fk_partes_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
                        CONSTRAINT fk_partes_instalacion FOREIGN KEY (instalacion_id) REFERENCES instalaciones(id),
                        CONSTRAINT uk_partes_numero UNIQUE (numero_parte)
);

-- Índices para queries comunes
CREATE INDEX idx_clientes_empresa ON clientes(empresa_id);
CREATE INDEX idx_clientes_estado ON clientes(estado);
CREATE INDEX idx_instalaciones_cliente ON instalaciones(cliente_id);
CREATE INDEX idx_contratos_cliente ON contratos(cliente_id);
CREATE INDEX idx_contratos_estado ON contratos(estado);
CREATE INDEX idx_contratos_fecha_fin ON contratos(fecha_fin);
CREATE INDEX idx_partes_contrato ON partes(contrato_id);
CREATE INDEX idx_partes_estado ON partes(estado);
CREATE INDEX idx_partes_cliente ON partes(cliente_id);

-- Inserts de prueba
INSERT INTO clientes (nombre, email, telefono, empresa_id, delegacion_id, estado)
VALUES
    ('Cliente A', 'clientea@empresa.com', '123456789', 1, 1, 'ACTIVO'),
    ('Cliente B', 'clienteb@empresa.com', '987654321', 1, 1, 'ACTIVO'),
    ('Cliente C', 'clientec@empresa.com', '555555555', 1, 2, 'ACTIVO');

INSERT INTO instalaciones (cliente_id, codigo, descripcion, direccion, codigo_postal, zona, estado)
VALUES
    (1, 'INST-001', 'Instalación Principal A', 'Calle Principal 1', '28001', 'MADRID', 'ACTIVO'),
    (1, 'INST-002', 'Instalación Secundaria A', 'Calle Secundaria 1', '28002', 'MADRID', 'ACTIVO'),
    (2, 'INST-003', 'Instalación Principal B', 'Calle Principal 2', '08002', 'BARCELONA', 'ACTIVO'),
    (3, 'INST-004', 'Instalación Principal C', 'Calle Principal 3', '46001', 'VALENCIA', 'ACTIVO');

INSERT INTO contratos (numero, cliente_id, instalacion_id, tipo_contrato, estado, cobertura_material, cobertura_mano_obra, cobertura_fin_semana, fecha_inicio, fecha_fin, precio_anual, porcentaje_central, empresa_id)
VALUES
    ('CONT-2024-001', 1, 1, 'MANTENIMIENTO', 'VIGENTE', 1, 1, 0, '2024-01-01', '2024-12-31', 1200.00, 70, 1),
    ('CONT-2024-002', 1, 2, 'REVISION', 'VIGENTE', 1, 1, 1, '2024-01-01', '2024-12-31', 800.00, 70, 1),
    ('CONT-2024-003', 2, 3, 'MANTENIMIENTO', 'VIGENTE', 1, 1, 0, '2024-02-01', '2025-01-31', 1500.00, 65, 1),
    ('CONT-2024-004', 3, 4, 'MANTENIMIENTO', 'VENCIDO', 1, 1, 0, '2023-01-01', '2023-12-31', 1000.00, 70, 1);