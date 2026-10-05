-- Base minima para despliegue limpio (seguro con IF NOT EXISTS, no borra datos)
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(255),
    apellido VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    username VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    estado BOOLEAN DEFAULT TRUE,
    rol_id INTEGER NOT NULL REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS asistencias (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha DATE,
    hora_entrada TIME,
    hora_salida TIME,
    estado_asistencia VARCHAR(20) DEFAULT 'PENDIENTE',
    observaciones VARCHAR(300)
);

-- Tabla de horarios (turnos con tolerancia)
CREATE TABLE IF NOT EXISTS horarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    hora_entrada TIME NOT NULL,
    hora_salida TIME NOT NULL,
    tolerancia_minutos INTEGER NOT NULL DEFAULT 15
);

-- Tabla de cursos
CREATE TABLE IF NOT EXISTS cursos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    turno VARCHAR(30),
    anio VARCHAR(10)
);

-- Relación muchos-a-muchos: horarios asignados a usuarios
CREATE TABLE IF NOT EXISTS horario_usuario (
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    horario_id INTEGER NOT NULL REFERENCES horarios(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, horario_id)
);

-- Relación muchos-a-muchos: cursos asignados a usuarios
CREATE TABLE IF NOT EXISTS curso_usuario (
    curso_id INTEGER NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    PRIMARY KEY (curso_id, usuario_id)
);

-- Nuevas columnas en asistencias para el flujo de validación
ALTER TABLE asistencias
    ADD COLUMN IF NOT EXISTS validado_por_id INTEGER REFERENCES usuarios(id),
    ADD COLUMN IF NOT EXISTS hora_validacion TIME;

-- Tabla de periodos lectivos (gestion del anio lectivo, panel del director)
CREATE TABLE IF NOT EXISTS periodos_lectivos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    duracion_semanas INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    es_ultimo_periodo BOOLEAN NOT NULL DEFAULT FALSE
);

-- Tabla de configuracion de semana de examenes (singleton: se usa la fila de id mas alto)
CREATE TABLE IF NOT EXISTS configuraciones_examenes (
    id SERIAL PRIMARY KEY,
    semana_examenes_activa BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_inicio_semana DATE,
    fecha_fin_semana DATE,
    mensaje_personalizado VARCHAR(200),
    mostrar_a_alumnos BOOLEAN NOT NULL DEFAULT TRUE,
    mostrar_a_profesores BOOLEAN NOT NULL DEFAULT TRUE
);

-- [P1] Integridad: una sola asistencia por usuario y fecha.
-- 1) Duplicados históricos: se conserva la fila con id más alto por (usuario_id, fecha).
DELETE FROM asistencias a
    USING asistencias b
    WHERE a.usuario_id = b.usuario_id
      AND a.fecha = b.fecha
      AND a.id < b.id;

-- 2) Restricción real en BD (el último recurso anti-doble-entrada)
CREATE UNIQUE INDEX IF NOT EXISTS ux_asistencias_usuario_fecha
    ON asistencias (usuario_id, fecha);
