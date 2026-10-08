-- ========================================================
-- Base de Datos para el Sistema de Agendamiento de Citas
-- Microservicio: backend (Nicolet Estudio)
-- ========================================================

CREATE DATABASE IF NOT EXISTS universidad_backend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE universidad_backend;

-- ========================================================
-- 1. Tabla tipo_usuario (Roles del sistema)
-- ========================================================
CREATE TABLE IF NOT EXISTS tipo_usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 2. Tabla usuario (Credenciales e información base)
-- ========================================================
CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidop VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    tipo_usuario_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_tipo_usuario 
        FOREIGN KEY (tipo_usuario_id) 
        REFERENCES tipo_usuario (id) 
        ON DELETE SET NULL 
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 3. Tabla cliente (Perfil especializado del cliente)
-- ========================================================
CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    telefono VARCHAR(20),
    rut_dni VARCHAR(20) UNIQUE,
    fecha_nacimiento DATE,
    notas_preferencias TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cliente_usuario 
        FOREIGN KEY (usuario_id) 
        REFERENCES usuario (id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 4. Tabla trabajador (Perfil del especialista / profesional)
-- ========================================================
CREATE TABLE IF NOT EXISTS trabajador (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    rut_dni VARCHAR(20) UNIQUE,
    telefono VARCHAR(20),
    cargo_especialidad VARCHAR(100) NOT NULL,
    biografia TEXT,
    comision_porcentaje DECIMAL(5,2) DEFAULT 0.00,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trabajador_usuario 
        FOREIGN KEY (usuario_id) 
        REFERENCES usuario (id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 5. Tabla servicio (Catálogo de servicios ofrecidos)
-- ========================================================
CREATE TABLE IF NOT EXISTS servicio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    descripcion TEXT,
    duracion_minutos INT NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 6. Tabla trabajador_servicio (Relación M:N)
-- ========================================================
CREATE TABLE IF NOT EXISTS trabajador_servicio (
    trabajador_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    PRIMARY KEY (trabajador_id, servicio_id),
    CONSTRAINT fk_ts_trabajador 
        FOREIGN KEY (trabajador_id) 
        REFERENCES trabajador (id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_ts_servicio 
        FOREIGN KEY (servicio_id) 
        REFERENCES servicio (id) 
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 7. Tabla horario_disponibilidad (Jornada del trabajador)
-- ========================================================
CREATE TABLE IF NOT EXISTS horario_disponibilidad (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trabajador_id BIGINT NOT NULL,
    dia_semana TINYINT NOT NULL COMMENT '1=Lunes, 2=Martes, ..., 7=Domingo',
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    hora_inicio_descanso TIME NULL,
    hora_fin_descanso TIME NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_horario_trabajador 
        FOREIGN KEY (trabajador_id) 
        REFERENCES trabajador (id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 8. Tabla cita (Agendamiento central del negocio)
-- ========================================================
CREATE TABLE IF NOT EXISTS cita (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_reserva VARCHAR(20) NOT NULL UNIQUE,
    cliente_id BIGINT NOT NULL,
    trabajador_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    fecha_hora_inicio DATETIME NOT NULL,
    fecha_hora_fin DATETIME NOT NULL,
    estado ENUM('PENDIENTE', 'CONFIRMADA', 'EN_ATENCION', 'COMPLETADA', 'CANCELADA', 'NO_ASISTIO') 
        NOT NULL DEFAULT 'PENDIENTE',
    precio_final DECIMAL(10,2) NOT NULL,
    notas_cliente TEXT,
    notas_trabajador TEXT,
    motivo_cancelacion VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_cita_cliente 
        FOREIGN KEY (cliente_id) 
        REFERENCES cliente (id) 
        ON DELETE RESTRICT,
    CONSTRAINT fk_cita_trabajador 
        FOREIGN KEY (trabajador_id) 
        REFERENCES trabajador (id) 
        ON DELETE RESTRICT,
    CONSTRAINT fk_cita_servicio 
        FOREIGN KEY (servicio_id) 
        REFERENCES servicio (id) 
        ON DELETE RESTRICT,
        
    INDEX idx_cita_trabajador_fechas (trabajador_id, fecha_hora_inicio, fecha_hora_fin),
    INDEX idx_cita_cliente (cliente_id, fecha_hora_inicio),
    INDEX idx_cita_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- Datos iniciales (Seed Data)
-- ========================================================

-- Roles / Tipos de usuario predeterminados
INSERT INTO tipo_usuario (id, nombre, descripcion) VALUES
(1, 'ADMINISTRADOR', 'Administrador con acceso total al sistema'),
(2, 'CLIENTE', 'Cliente que solicita y agenda citas'),
(3, 'PROFESIONAL', 'Profesional o especialista que atiende las citas')
ON DUPLICATE KEY UPDATE 
    nombre = VALUES(nombre), 
    descripcion = VALUES(descripcion);

-- Usuarios de prueba iniciales
INSERT INTO usuario (id, nombre, apellidop, correo, password, tipo_usuario_id) VALUES
(1, 'Maximo', 'Rojas', 'maximo.rojas@estudio.cl', 'password_seguro_123', 1),
(2, 'Martina', 'Contreras', 'martina.contreras@gmail.com', '123412', 2),
(3, 'Nicolet', 'Estudio', 'contacto@nicollet.cl', 'admin2026', 1),
(4, 'Camila', 'Silva', 'camila.silva@estudio.cl', 'pro2026', 3)
ON DUPLICATE KEY UPDATE 
    nombre = VALUES(nombre),
    apellidop = VALUES(apellidop),
    password = VALUES(password),
    tipo_usuario_id = VALUES(tipo_usuario_id);

-- Perfil de Cliente de prueba (asociado a usuario 2: Martina)
INSERT INTO cliente (id, usuario_id, telefono, rut_dni, fecha_nacimiento, notas_preferencias, activo) VALUES
(1, 2, '+56912345678', '19876543-2', '1998-05-14', 'Prefiere tonos neutros para manicura', TRUE)
ON DUPLICATE KEY UPDATE
    telefono = VALUES(telefono),
    notas_preferencias = VALUES(notas_preferencias);

-- Perfil de Trabajador de prueba (asociado a usuario 4: Camila)
INSERT INTO trabajador (id, usuario_id, rut_dni, telefono, cargo_especialidad, biografia, comision_porcentaje, activo) VALUES
(1, 4, '18234567-8', '+56987654321', 'Especialista en Estilismo y Manicura', '5 años de experiencia en estética integral', 30.00, TRUE)
ON DUPLICATE KEY UPDATE
    cargo_especialidad = VALUES(cargo_especialidad),
    biografia = VALUES(biografia);

-- Catálogo inicial de Servicios
INSERT INTO servicio (id, nombre, descripcion, duracion_minutos, precio, activo) VALUES
(1, 'Corte de Cabello y Peinado', 'Lavado, corte estilizado y secado profesional', 45, 18000.00, TRUE),
(2, 'Manicura Rusa con Esmaltado Permanente', 'Limpieza profunda de cutículas y esmaltado de alta duración', 60, 22000.00, TRUE),
(3, 'Tratamiento Facial Hidratante', 'Limpieza profunda, exfoliación y mascarilla regeneradora', 60, 28000.00, TRUE)
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    duracion_minutos = VALUES(duracion_minutos),
    precio = VALUES(precio);

-- Asignación de servicios que ofrece Camila (Trabajador 1)
INSERT IGNORE INTO trabajador_servicio (trabajador_id, servicio_id) VALUES
(1, 1),
(1, 2);

-- Horario de disponibilidad de Camila (Lunes a Viernes: 09:00 a 18:00 con colación 13:00 - 14:00)
INSERT INTO horario_disponibilidad (trabajador_id, dia_semana, hora_inicio, hora_fin, hora_inicio_descanso, hora_fin_descanso, activo) VALUES
(1, 1, '09:00:00', '18:00:00', '13:00:00', '14:00:00', TRUE),
(1, 2, '09:00:00', '18:00:00', '13:00:00', '14:00:00', TRUE),
(1, 3, '09:00:00', '18:00:00', '13:00:00', '14:00:00', TRUE),
(1, 4, '09:00:00', '18:00:00', '13:00:00', '14:00:00', TRUE),
(1, 5, '09:00:00', '18:00:00', '13:00:00', '14:00:00', TRUE);

-- Cita de prueba (Martina se atiende con Camila para Manicura Rusa)
INSERT INTO cita (id, codigo_reserva, cliente_id, trabajador_id, servicio_id, fecha_hora_inicio, fecha_hora_fin, estado, precio_final, notas_cliente, notas_trabajador) VALUES
(1, 'RES-2026-0001', 1, 1, 2, '2026-10-15 10:00:00', '2026-10-15 11:00:00', 'CONFIRMADA', 22000.00, 'Primera sesión', 'Revisar estado de cutículas previas')
ON DUPLICATE KEY UPDATE
    estado = VALUES(estado);
