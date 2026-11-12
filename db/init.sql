-- Esquema de base de datos y datos iniciales

CREATE DATABASE IF NOT EXISTS universidad_backend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE universidad_backend;

-- Tabla tipo_usuario
CREATE TABLE IF NOT EXISTS tipo_usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla usuario
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

-- Tabla cliente
CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNIQUE,
    nombre_invitado VARCHAR(100),
    apellido_p_invitado VARCHAR(100),
    correo_invitado VARCHAR(150),
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

-- Tabla trabajador
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

-- Tabla servicio
CREATE TABLE IF NOT EXISTS servicio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    descripcion TEXT,
    duracion_minutos INT NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla trabajador_servicio
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

-- Tabla horario_disponibilidad
CREATE TABLE IF NOT EXISTS horario_disponibilidad (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trabajador_id BIGINT NOT NULL,
    dia_semana TINYINT NOT NULL COMMENT '1=Lunes a 7=Domingo',
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    hora_inicio_descanso TIME NULL,
    hora_fin_descanso TIME NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_trabajador_dia (trabajador_id, dia_semana),
    CONSTRAINT fk_horario_trabajador 
        FOREIGN KEY (trabajador_id) 
        REFERENCES trabajador (id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla cita
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

-- Datos iniciales

INSERT INTO tipo_usuario (id, nombre, descripcion) VALUES
(1, 'ADMINISTRADOR', 'Administrador con acceso total al sistema'),
(2, 'CLIENTE', 'Cliente que solicita y agenda citas'),
(3, 'PROFESIONAL', 'Profesional o especialista que atiende las citas')
ON DUPLICATE KEY UPDATE 
    nombre = VALUES(nombre), 
    descripcion = VALUES(descripcion);

-- Este script no crea cuentas de usuario: ninguna contraseña (ni su hash) se guarda en el repositorio.
-- El primer administrador lo crea el backend al arrancar (variables APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD).
-- Los clientes, profesionales y citas de ejemplo se cargan solo con APP_DEMO_DATA_ENABLED=true.

INSERT INTO servicio (id, nombre, descripcion, duracion_minutos, precio, activo) VALUES
(1, 'Corte de Cabello y Peinado', 'Lavado, corte estilizado y secado profesional', 45, 18000.00, TRUE),
(2, 'Manicura Rusa con Esmaltado Permanente', 'Limpieza profunda de cutículas y esmaltado de alta duración', 60, 22000.00, TRUE),
(3, 'Tratamiento Facial Hidratante', 'Limpieza profunda, exfoliación y mascarilla regeneradora', 60, 28000.00, TRUE)
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    duracion_minutos = VALUES(duracion_minutos),
    precio = VALUES(precio);
