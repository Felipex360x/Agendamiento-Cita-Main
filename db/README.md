# Base de Datos - Nicolet Estudio (Agendamiento de Citas)

Este directorio contiene los scripts y la configuracion para inicializar la base de datos MySQL requerida por el servicio backend.

---

## Parametros de Conexion

| Parametro | Valor |
|---|---|
| Motor | MySQL 8.0 |
| Host local | `localhost` (puerto `3306`) |
| Host en Docker | `mysql_usuario` (puerto `3306`) |
| Nombre Base de Datos | `universidad_backend` |
| Usuario | `desarrollador` |
| Contraseña | `password_seguro_123` |
| Root Password | `root_password_123` |

---

## Como Iniciar la Base de Datos con Docker

Desde la raiz del proyecto:

```bash
docker compose up -d
```

Esto levantara:
1. El contenedor `mysql_usuario` en el puerto `3306`.
2. El script de inicializacion `init.sql` con el esquema completo de agendamiento y datos semilla.
3. El panel visual phpMyAdmin accesible desde:
   http://localhost:8081 (Usuario: `desarrollador` / Contraseña: `password_seguro_123`).

Para reiniciar con el esquema limpio en Docker:
```bash
docker compose down -v
docker compose up -d
```

---

## Seguridad y Almacenamiento de Contraseñas

Las contraseñas de las cuentas de usuario se almacenan exclusivamente como hashes criptograficos BCrypt (`$2a$...`). El sistema no almacena ni expone contraseñas en texto plano:

- El componente `DataInitializer` del backend verifica en el arranque que todas las cuentas base posean su contraseña debidamente cifrada con BCrypt.
- Si detecta contraseñas legadas en texto plano, las actualiza de forma automatica y transparente.
- Las cuentas de prueba administrativas (`admin.test@nicolet.cl` y `admin@nicolet.cl`) cuentan con sincronizacion garantizada de credenciales.

---

## Estructura de Tablas del Sistema

### 1. `tipo_usuario`
Roles del sistema: `ADMINISTRADOR`, `CLIENTE`, `PROFESIONAL`.
- `id`: BIGINT (PK, Auto Increment)
- `nombre`: VARCHAR(50) (Unico)
- `descripcion`: VARCHAR(255)

### 2. `usuario`
Credenciales y cuenta base del sistema.
- `id`: BIGINT (PK, Auto Increment)
- `nombre`: VARCHAR(100)
- `apellidop`: VARCHAR(100)
- `correo`: VARCHAR(150) (Unico)
- `password`: VARCHAR(255) (Hash BCrypt)
- `tipo_usuario_id`: BIGINT (FK -> `tipo_usuario.id`)
- `created_at`: TIMESTAMP

### 3. `cliente`
Perfil especifico de un cliente (relacion 1:1 con `usuario`).
- `id`: BIGINT (PK, Auto Increment)
- `usuario_id`: BIGINT (FK -> `usuario.id`, Unico)
- `telefono`: VARCHAR(20)
- `rut_dni`: VARCHAR(20) (Unico)
- `fecha_nacimiento`: DATE
- `notas_preferencias`: TEXT
- `activo`: BOOLEAN (Default TRUE)
- `created_at`: TIMESTAMP

### 4. `trabajador`
Perfil especifico del profesional que atiende citas (relacion 1:1 con `usuario`).
- `id`: BIGINT (PK, Auto Increment)
- `usuario_id`: BIGINT (FK -> `usuario.id`, Unico)
- `rut_dni`: VARCHAR(20) (Unico)
- `telefono`: VARCHAR(20)
- `cargo_especialidad`: VARCHAR(100)
- `biografia`: TEXT
- `comision_porcentaje`: DECIMAL(5,2)
- `activo`: BOOLEAN (Default TRUE)
- `created_at`: TIMESTAMP

### 5. `servicio`
Catalogo de prestaciones ofrecidas en el estudio.
- `id`: BIGINT (PK, Auto Increment)
- `nombre`: VARCHAR(120) (Unico)
- `descripcion`: TEXT
- `duracion_minutos`: INT (Duracion estimada del servicio)
- `precio`: DECIMAL(10,2)
- `activo`: BOOLEAN (Default TRUE)

### 6. `trabajador_servicio`
Relacion de asignacion entre trabajadores y servicios que realizan.
- `trabajador_id`: BIGINT (FK -> `trabajador.id`)
- `servicio_id`: BIGINT (FK -> `servicio.id`)
- PK compuesta: `(trabajador_id, servicio_id)`

### 7. `horario_disponibilidad`
Definicion de turnos semanales de cada profesional.
- `id`: BIGINT (PK, Auto Increment)
- `trabajador_id`: BIGINT (FK -> `trabajador.id`)
- `dia_semana`: TINYINT (1=Lunes a 7=Domingo)
- `hora_inicio`: TIME
- `hora_fin`: TIME
- `hora_inicio_descanso`: TIME
- `hora_fin_descanso`: TIME
- `activo`: BOOLEAN (Default TRUE)

### 8. `cita`
Agendamiento central que vincula al cliente, trabajador y servicio.
- `id`: BIGINT (PK, Auto Increment)
- `codigo_reserva`: VARCHAR(20) (Unico, ej: `RES-2026-0001`)
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
