# Base de Datos - Nicolett Studio Fantasy (Agendamiento de Citas)

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
| Contraseña | la de `DB_PASSWORD` en `.env` (hay un valor por defecto solo para desarrollo local) |
| Root Password | la de `MYSQL_ROOT_PASSWORD` en `.env` |

---

## Como Iniciar la Base de Datos con Docker

Desde la raiz del proyecto:

```bash
docker compose up -d
```

Esto levantara:
1. El contenedor `mysql_usuario` en el puerto `3306`, publicado solo en `127.0.0.1` (no es accesible desde otros equipos de la red).
2. El script de inicializacion `init.sql` con el esquema completo, los roles y el catalogo base de servicios. No crea cuentas de usuario.

El panel phpMyAdmin es opcional y no se inicia por defecto:

```bash
docker compose --profile herramientas up -d
```

Queda en http://localhost:8081 (solo desde este equipo) y pide el usuario y la contraseña de la base de datos.

Al iniciar el backend, las bases MySQL existentes también actualizan `cliente.usuario_id` para permitir perfiles de clientes invitados sin cuenta.

Para reiniciar con el esquema limpio en Docker:
```bash
docker compose down -v
docker compose up -d
```

---

## Cuentas y Contraseñas

El repositorio no contiene cuentas de usuario ni contraseñas (tampoco sus hashes).

- **Primer administrador**: si la base no tiene ningun administrador, el backend crea uno al arrancar con `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`. Si no defines la contraseña, genera una al azar y la muestra una unica vez en la consola. Si ya existe un administrador no modifica nada.
- **Datos de ejemplo**: con `APP_DEMO_DATA_ENABLED=true` se cargan clientes, profesionales y citas de demostracion. Su contraseña es la de `APP_DEMO_PASSWORD`; si no la defines se genera una al azar en cada arranque y se muestra en la consola. No actives los datos de ejemplo en un entorno expuesto a Internet.
- Las contraseñas se guardan siempre como hash BCrypt.

Si tu base se creo con una version anterior de `init.sql`, conserva las cuentas antiguas con contraseñas que estuvieron publicadas: cambialas o eliminalas (o recrea el volumen con `docker compose down -v`, que borra todos los datos).

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
