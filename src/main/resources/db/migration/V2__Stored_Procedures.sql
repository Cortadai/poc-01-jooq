-- ===================================================================
-- MIGRACIÓN V2: STORED PROCEDURES - CORREGIDA CON VOLATILITY
-- ===================================================================
-- El error "UPDATE is not allowed in a non-volatile function" se debe
-- a que usamos STABLE. Necesitamos VOLATILE para funciones que modifican datos.
-- ===================================================================

DROP FUNCTION IF EXISTS sp_actualizar_estado_contratos();

CREATE FUNCTION sp_actualizar_estado_contratos()
    RETURNS TABLE (
                      total_actualizados INTEGER,
                      fecha_ejecucion TIMESTAMP,
                      mensaje TEXT
                  )
    LANGUAGE plpgsql
VOLATILE
AS $$
DECLARE
v_total_actualizados INTEGER := 0;
BEGIN
UPDATE contratos
SET
    estado = 'VENCIDO',
    fecha_modificacion = CURRENT_TIMESTAMP,
    usuario_modificacion = 'SYSTEM_JOB'
WHERE
    estado = 'VIGENTE'
  AND fecha_fin < CURRENT_DATE;

GET DIAGNOSTICS v_total_actualizados = ROW_COUNT;

RETURN QUERY SELECT
        v_total_actualizados,
        CURRENT_TIMESTAMP::TIMESTAMP,
        ('Actualizado ' || v_total_actualizados || ' contratos')::TEXT;
END;
$$;

-- ===================================================================

DROP FUNCTION IF EXISTS sp_generar_reporte_cartera();

CREATE FUNCTION sp_generar_reporte_cartera()
    RETURNS TABLE (
                      cliente_id INTEGER,
                      cliente_nombre VARCHAR,
                      total_contratos BIGINT,
                      contratos_vigentes BIGINT,
                      contratos_vencidos BIGINT,
                      contratos_cancelados BIGINT,
                      contratos_suspendidos BIGINT,
                      volumen_total NUMERIC,
                      volumen_vigentes NUMERIC,
                      volumen_vencidos NUMERIC,
                      precio_promedio NUMERIC,
                      porcentaje_cobertura_material NUMERIC,
                      porcentaje_cobertura_mano_obra NUMERIC,
                      fecha_proximo_vencimiento DATE,
                      dias_para_vencer INTEGER,
                      estado_cartera VARCHAR
                  )
    LANGUAGE plpgsql
STABLE
AS $$
BEGIN
RETURN QUERY
    WITH cliente_stats AS (
        SELECT
            c.id,
            c.nombre,
            COUNT(con.id) as total_contratos,
            COUNT(CASE WHEN con.estado = 'VIGENTE' THEN 1 END) as contratos_vigentes,
            COUNT(CASE WHEN con.estado = 'VENCIDO' THEN 1 END) as contratos_vencidos,
            COUNT(CASE WHEN con.estado = 'CANCELADO' THEN 1 END) as contratos_cancelados,
            COUNT(CASE WHEN con.estado = 'SUSPENDIDO' THEN 1 END) as contratos_suspendidos,
            COALESCE(SUM(con.precio_anual), 0) as volumen_total,
            COALESCE(SUM(CASE WHEN con.estado = 'VIGENTE' THEN con.precio_anual ELSE 0 END), 0) as volumen_vigentes,
            COALESCE(SUM(CASE WHEN con.estado = 'VENCIDO' THEN con.precio_anual ELSE 0 END), 0) as volumen_vencidos,
            COALESCE(AVG(con.precio_anual), 0) as precio_promedio,
            ROUND(100.0 * COUNT(CASE WHEN con.cobertura_material = true THEN 1 END) / NULLIF(COUNT(con.id), 0), 2) as porcentaje_cobertura_material,
            ROUND(100.0 * COUNT(CASE WHEN con.cobertura_mano_obra = true THEN 1 END) / NULLIF(COUNT(con.id), 0), 2) as porcentaje_cobertura_mano_obra,
            MIN(CASE WHEN con.estado = 'VIGENTE' THEN con.fecha_fin END) as fecha_proximo_vencimiento,
            (MIN(CASE WHEN con.estado = 'VIGENTE' THEN con.fecha_fin END) - CURRENT_DATE) as dias_para_vencer
        FROM clientes c
        LEFT JOIN contratos con ON c.id = con.cliente_id
        GROUP BY c.id, c.nombre
    )
SELECT
    cs.id,
    cs.nombre,
    cs.total_contratos,
    cs.contratos_vigentes,
    cs.contratos_vencidos,
    cs.contratos_cancelados,
    cs.contratos_suspendidos,
    cs.volumen_total,
    cs.volumen_vigentes,
    cs.volumen_vencidos,
    ROUND(cs.precio_promedio, 2),
    cs.porcentaje_cobertura_material,
    cs.porcentaje_cobertura_mano_obra,
    cs.fecha_proximo_vencimiento,
    cs.dias_para_vencer,
    (CASE
         WHEN cs.total_contratos = 0 THEN 'SIN_CONTRATOS'
         WHEN cs.contratos_vigentes = 0 THEN 'SIN_VIGENTES'
         WHEN cs.dias_para_vencer < 30 AND cs.dias_para_vencer > 0 THEN 'CRÍTICO'
         WHEN cs.dias_para_vencer <= 0 THEN 'VENCIDO'
         WHEN cs.dias_para_vencer <= 90 THEN 'ALERTA'
         ELSE 'NORMAL'
        END)::VARCHAR
FROM cliente_stats cs
ORDER BY cs.volumen_vigentes DESC, cs.nombre ASC;
END;
$$;

-- ===================================================================

DROP FUNCTION IF EXISTS sp_crear_parte_trabajo(INTEGER, TEXT, VARCHAR);

CREATE FUNCTION sp_crear_parte_trabajo(
    p_contrato_id INTEGER,
    p_descripcion TEXT,
    p_tipo_trabajo VARCHAR DEFAULT 'Mantenimiento'
)
    RETURNS TABLE (
                      parte_id INTEGER,
                      numero VARCHAR,
                      estado VARCHAR,
                      mensaje TEXT,
                      codigo_error INTEGER
                  )
    LANGUAGE plpgsql
VOLATILE
AS $$
DECLARE
v_parte_id INTEGER;
    v_numero VARCHAR;
    v_error_code INTEGER;
    v_mensaje TEXT;
BEGIN
    v_error_code := 0;
    v_mensaje := '';

    IF NOT EXISTS (SELECT 1 FROM contratos WHERE id = p_contrato_id) THEN
        RETURN QUERY SELECT NULL::INTEGER, NULL::VARCHAR, 'ERROR'::VARCHAR, 'Contrato no existe'::TEXT, 1;
RETURN;
END IF;

    IF (SELECT contratos.estado FROM contratos WHERE contratos.id = p_contrato_id) != 'VIGENTE' THEN
        RETURN QUERY SELECT NULL::INTEGER, NULL::VARCHAR, 'ERROR'::VARCHAR, 'Contrato no vigente'::TEXT, 2;
RETURN;
END IF;

SELECT 'PARTE-' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || '-' || LPAD((COUNT(*) + 1)::TEXT, 5, '0')
INTO v_numero
FROM partes
WHERE DATE(fecha_creacion) = CURRENT_DATE;

IF v_numero IS NULL THEN
        v_numero := 'PARTE-' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || '-00001';
END IF;

INSERT INTO partes (
    numero, contrato_id, fecha_inicio, descripcion, tipo_trabajo,
    estado, empresa_id, fecha_creacion, usuario_creacion
)
SELECT
    v_numero, p_contrato_id, CURRENT_TIMESTAMP, p_descripcion, p_tipo_trabajo,
    'ABIERTO', empresa_id, CURRENT_TIMESTAMP, 'SYSTEM'
FROM contratos
WHERE id = p_contrato_id
    RETURNING id INTO v_parte_id;

RETURN QUERY SELECT v_parte_id, v_numero, 'OK'::VARCHAR, ('Parte ' || v_numero || ' creado')::TEXT, 0;
END;
$$;