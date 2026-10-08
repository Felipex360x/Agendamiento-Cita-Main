# Base de Datos - Nicolet Estudio (Agendamiento de Citas)

Este directorio contiene los scripts y la configuración para inicializar la base de datos MySQL requerida por el servicio `backend`.

---

## Parámetros de Conexión

| Parámetro | Valor |
|---|---|
| **Motor** | MySQL 8.0 |
| **Host local** | `localhost` (puerto `3306`) |
| **Host en Docker** | `mysql_usuario` (puerto `3306`) |
| **Nombre Base de Datos** | `universidad_backend` |
| **Usuario** | `desarrollador` |
| **Contraseña** | `password_seguro_123` |
| **Root Password** | `root_password_123` |

---

## Cómo iniciar la Base de Datos con Docker

Desde la raíz del proyecto:

```bash
docker compose up -d
```

Esto levantará automáticamente:
1. El contenedor **`mysql_usuario`** en el puerto `3306`.
2. El script de inicialización [`init.sql`](init.sql) con el esquema completo de agendamiento y datos semilla.
3. El panel visual **phpMyAdmin** accesible desde:
   **http://localhost:8081** (Usuario: `desarrollador` / Contraseña: `password_seguro_123`).

Para reiniciar con el nuevo esquema en Docker si ya existía una base de datos previa:
```bash
docker compose down -v
docker compose up -d
```

---

## Estructura de Tablas del Sistema

### 1. `tipo_usuario`
Roles del sistema: `ADMINISTRADOR`, `CLIENTE`, `PROFESIONAL`.
- `id`: BIGINT (PK, Auto Increment)
- `nombre`: VARCHAR(50) (Único)
- `descripcion`: VARCHAR(255)

### 2. `usuario`
Credenciales y cuenta base del sistema.
- `id`: BIGINT (PK, Auto Increment)
- `nombre`: VARCHAR(100)
- `apellidop`: VARCHAR(100)
- `correo`: VARCHAR(150) (Único)
- `password`: VARCHAR(255)
- `tipo_usuario_id`: BIGINT (FK -> `tipo_usuario.id`)
- `created_at`: TIMESTAMP

### 3. `cliente`
Perfil específico de un cliente (relación 1:1 con `usuario`).
- `id`: BIGINT (PK, Auto Increment)
- `usuario_id`: BIGINT (FK -> `usuario.id`, Único)
- `telefono`: VARCHAR(20)
- `rut_dni`: VARCHAR(20) (Único)
- `fecha_nacimiento`: DATE
- `notas_preferencias`: TEXT (Alergias, tonos preferidos, notas técnicas)
- `activo`: BOOLEAN (Default TRUE)
- `created_at`: TIMESTAMP

### 4. `trabajador`
Perfil específico del profesional/especialista que atiende citas (relación 1:1 con `usuario`).
- `id`: BIGINT (PK, Auto Increment)
- `usuario_id`: BIGINT (FK -> `usuario.id`, Único)
- `rut_dni`: VARCHAR(20) (Único)
- `telefono`: VARCHAR(20)
- `cargo_especialidad`: VARCHAR(100) (Ej: Estilista Senior, Manicurista)
- `biografia`: TEXT
- `comision_porcentaje`: DECIMAL(5,2)
- `activo`: BOOLEAN (Default TRUE)
- `created_at`: TIMESTAMP

### 5. `servicio`
Catálogo de prestaciones ofrecidas en el estudio.
- `id`: BIGINT (PK, Auto Increment)
- `nombre`: VARCHAR(120) (Único)
- `descripcion`: TEXT
- `duracion_minutos`: INT (Duración estimada del servicio)
- `precio`: DECIMAL(10,2)
- `activo`: BOOLEAN (Default TRUE)

### 6. `trabajador_servicio`
Relación muchos a muchos entre trabajadores y servicios que realizan.
- `trabajador_id`: BIGINT (FK -> `trabajador.id`)
- `servicio_id`: BIGINT (FK -> `servicio.id`)
- PK compuesta: `(trabajador_id, servicio_id)`

### 7. `horario_disponibilidad`
Definición de turnos semanales de cada profesional.
- `id`: BIGINT (PK, Auto Increment)
- `trabajador_id`: BIGINT (FK -> `trabajador.id`)
- `dia_semana`: TINYINT (1=Lunes a 7=Domingo)
- `hora_inicio`: TIME
- `hora_fin`: TIME
- `hora_inicio_descanso`: TIME (Horario de colación/almuerzo)
- `hora_fin_descanso`: TIME
- `activo`: BOOLEAN (Default TRUE)

### 8. `cita`
Agendamiento central que vincula al cliente, trabajador y servicio.
- `id`: BIGINT (PK, Auto Increment)
- `codigo_reserva`: VARCHAR(20) (Único, ej: `RES-2026-0001`)
- `cliente_id`: BIGINT (FK -> `cliente.id`)
- `trabajador_id`: BIGINT (FK -> `trabajador.id`)
- `servicio_id`: BIGINT (FK -> `servicio.id`)
- `fecha_hora_inicio`: DATETIME
- `fecha_hora_fin`: DATETIME (Calculada: `inicio + duracion_servicio`)
- `estado`: ENUM (`PENDIENTE`, `CONFIRMADA`, `EN_ATENCION`, `COMPLETADA`, `CANCELADA`, `NO_ASISTIO`)
- `precio_final`: DECIMAL(10,2)
- `notas_cliente`: TEXT
- `notas_trabajador`: TEXT
- `motivo_cancelacion`: VARCHAR(255)
- `created_at`: TIMESTAMP
- `updated_at`: TIMESTAMP
