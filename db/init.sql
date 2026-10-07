-- ========================================================
-- Base de Datos para el Sistema de Agendamiento de Citas
-- Microservicio: backend
-- ========================================================

CREATE DATABASE IF NOT EXISTS universidad_backend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE universidad_backend;

-- 1. Tabla tipo_usuario (Roles / Tipos para usuarios del sistema)
CREATE TABLE IF NOT EXISTS tipo_usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabla usuario
CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidop VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    tipo_usuario_id BIGINT NULL,
    CONSTRAINT fk_usuario_tipo_usuario 
        FOREIGN KEY (tipo_usuario_id) 
        REFERENCES tipo_usuario (id) 
        ON DELETE SET NULL 
        ON UPDATE CASCADE
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
(3, 'Nicolet', 'Estudio', 'contacto@nicollet.cl', 'admin2026', 1)
ON DUPLICATE KEY UPDATE 
    nombre = VALUES(nombre),
    apellidop = VALUES(apellidop),
    password = VALUES(password);
