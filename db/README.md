# Base de Datos - Backend Usuario

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

##  Cómo iniciar la Base de Datos con Docker

Desde la raíz del proyecto:

```bash
docker compose up -d
```

Esto levantará automáticamente:
1. El contenedor **`mysql_usuario`** en el puerto `3306`.
2. El script de inicialización [`init.sql`](init.sql) que crea la base de datos y las tablas `tipo_usuario` y `usuario` con datos de prueba.
3. El panel visual **phpMyAdmin** accesible desde el navegador en:
 **http://localhost:8081** (Usuario: `desarrollador` / Contraseña: `password_seguro_123`).

Para detener la base de datos:
```bash
docker compose down
```

---

##  Estructura de Tablas

### `tipo_usuario`
- `id`: BIGINT (Primary Key, Auto Increment)
- `nombre`: VARCHAR(50) (Único: `ADMINISTRADOR`, `CLIENTE`, `PROFESIONAL`)
- `descripcion`: VARCHAR(255)

### `usuario`
- `id`: BIGINT (Primary Key, Auto Increment)
- `nombre`: VARCHAR(100)
- `apellidop`: VARCHAR(100)
- `correo`: VARCHAR(150) (Único)
- `password`: VARCHAR(255)
- `tipo_usuario_id`: BIGINT (Foreign Key hacia `tipo_usuario.id`)
