# Nicolet Estudio - Sistema de Agendamiento de Citas

Sistema integral de gestión y agendamiento de citas para **Nicolet Estudio**, desarrollado con arquitectura cliente-servidor basada en **Spring Boot**, **MySQL** y **Frontend Web**.

---

## Estructura del Proyecto

```text
Agendamiento-Cita-Main/
├── src/                          # Backend: Código fuente Java Spring Boot
│   ├── main/java/cl/nicolet/backend/
│   │   ├── config/              # Configuraciones (Swagger / OpenAPI, DataInitializer)
│   │   ├── controller/          # Controladores REST API (Usuarios, Citas, Servicios, etc.)
│   │   ├── dto/                 # Objetos de Transferencia de Datos (DTOs)
│   │   ├── exception/           # Manejador global de excepciones
│   │   ├── model/               # Entidades JPA (Usuario, Cliente, Trabajador, Cita, etc.)
│   │   ├── repository/          # Repositorios Spring Data JPA
│   │   ├── service/             # Lógica de negocio (Agendamiento, Antisolapamiento, etc.)
│   │   └── BackendApplication.java
│   └── main/resources/
│       ├── application.properties      # Configuración de base de datos MySQL (MySQLDialect)
│       └── application-dev.properties  # Perfil de desarrollo alternativo (H2)
├── db/                           # Base de Datos
│   ├── init.sql                 # Script de creación de 8 tablas y datos semilla
│   └── README.md                # Documentación detallada de la base de datos
├── front/                        # Frontend Web (HTML / CSS / JS)
│   ├── index.html               # Pantalla de gestión de usuarios
│   ├── citas.html               # Pantalla interactiva de agendamiento de citas
│   ├── css/
│   │   └── index.css            # Estilos visuales compartidos
│   ├── js/
│   │   ├── index.js             # Lógica e integración REST de usuarios
│   │   └── citas.js             # Lógica e integración REST de citas y modales rápidos
│   └── README.md                # Guía de uso del frontend
├── docker-compose.yml            # Orquestación de MySQL 8.0 y phpMyAdmin
├── Dockerfile                    # Empaquetado del microservicio Backend
├── start.ps1                     # Script de inicio seguro en PowerShell (libera puerto 8080)
├── start.cmd                     # Script de inicio seguro en CMD (libera puerto 8080)
├── mvnw / mvnw.cmd               # Wrapper de Maven (no requiere instalar Maven)
├── pom.xml                       # Dependencias y build del proyecto
└── README.md                     # Guía principal de instalación y ejecución
```

---

## Requisitos Previos y Dependencias

Para ejecutar este proyecto en cualquier computadora, se necesitan los siguientes componentes:

### 1. Java Development Kit (JDK 17)
- **Requerido**: Java 17 o superior (Eclipse Temurin / OpenJDK).
- **Instalación rápida en Windows (PowerShell)**:
  ```powershell
  winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-source-agreements --accept-package-agreements
  ```
- **Verificación**:
  ```bash
  java -version
  ```

---

### 2. Motor de Base de Datos (MySQL o Docker)
Elige una de las siguientes alternativas:

#### Opción A: Usando Docker Desktop (Recomendada)
- Instala Docker Desktop si no lo tienes: [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)

#### Opción B: MySQL Server Local (Nativo)
- **Instalación rápida en Windows (PowerShell)**:
  ```powershell
  winget install --id Oracle.MySQL -e --accept-source-agreements --accept-package-agreements
  ```

---

## Guía Paso a Paso para Ejecutar el Proyecto

### Paso 1: Clonar el Repositorio
```bash
git clone https://github.com/Felipex360x/Agendamiento-Cita-Main.git
cd Agendamiento-Cita-Main
```

---

### Paso 2: Iniciar la Base de Datos

#### Si usas Docker:
Desde la raíz del proyecto, ejecuta:
```bash
docker compose up -d
```
Esto creará automáticamente:
- El contenedor **`mysql_usuario`** en el puerto `3306`.
- La base de datos `universidad_backend` con las 8 tablas de [`db/init.sql`](db/init.sql).
- Panel **phpMyAdmin** en [http://localhost:8081](http://localhost:8081) (`desarrollador` / `password_seguro_123`).

---

### Paso 3: Iniciar el Backend (Spring Boot)

#### Opción Recomendada (Inicio Seguro con auto-liberación de puerto 8080):
- En Windows PowerShell:
  ```powershell
  .\start.ps1
  ```
- En Windows CMD:
  ```cmd
  start.cmd
  ```

#### Opción Estándar con Maven Wrapper:
- En Windows:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- En Linux o macOS:
  ```bash
  ./mvnw spring-boot:run
  ```

El servidor iniciará en el puerto `8080`.  
> **Nota:** La aplicación incluye un componente **`DataInitializer`** que verifica y crea automáticamente los datos iniciales necesarios (servicios, clientes, profesionales, turnos de disponibilidad y citas) al momento del arranque.

---

### Paso 4: Abrir la Interfaz Visual (Frontend)

Ve a la carpeta [`front/`](front/) y abre en tu navegador:
* **[`index.html`](front/index.html):** Gestión y CRUD interactivo de usuarios y roles.
* **[`citas.html`](front/citas.html):** Módulo de agendamiento de citas, visualización de agenda y modales de registro rápido de clientes y profesionales.

---

## Documentación Interactiva de la API (Swagger UI)

Con el backend iniciado, puedes probar y consultar todos los endpoints REST directamente desde el explorador:

**[http://localhost:8080/doc/swagger-ui.html](http://localhost:8080/doc/swagger-ui.html)**

### Endpoints Principales:

#### 1. Citas (Agendamiento Central)
| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v2/nicolet/citas` | Lista todas las citas agendadas |
| `GET` | `/api/v2/nicolet/citas/{id}` | Busca una cita por su ID único |
| `GET` | `/api/v2/nicolet/citas/codigo/{codigo}` | Busca cita por código de reserva (ej: `RES-2026-0001`) |
| `GET` | `/api/v2/nicolet/citas/trabajador/{id}` | Agenda de un profesional específico |
| `GET` | `/api/v2/nicolet/citas/cliente/{id}` | Historial de citas de un cliente |
| `POST` | `/api/v2/nicolet/citas` | Agenda una nueva cita (con validación antisolapamiento y duración automática) |
| `PATCH` | `/api/v2/nicolet/citas/{id}/estado` | Cambia estado de una cita (`COMPLETADA`, `CANCELADA`) |

#### 2. Servicios
| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v2/nicolet/servicios` | Catálogo de servicios activos |
| `GET` | `/api/v2/nicolet/servicios/{id}` | Detalle de un servicio por ID |
| `POST` | `/api/v2/nicolet/servicios` | Registra un nuevo servicio |
| `PUT` | `/api/v2/nicolet/servicios/{id}` | Actualiza datos de un servicio |
| `DELETE` | `/api/v2/nicolet/servicios/{id}` | Desactiva un servicio (baja lógica) |

#### 3. Clientes y Trabajadores
| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v2/nicolet/clientes` | Lista todos los clientes |
| `POST` | `/api/v2/nicolet/clientes` | Crea perfil especializado de cliente |
| `GET` | `/api/v2/nicolet/trabajadores` | Lista todos los trabajadores activos |
| `POST` | `/api/v2/nicolet/trabajadores` | Crea perfil especializado de trabajador con servicios |

#### 4. Usuarios y Roles
| Método | Endpoint | Descripción |
|---|---|---|
| `GET` / `POST` | `/api/v2/nicolet/usuarios` | CRUD de cuentas de usuario base |
| `GET` / `POST` | `/api/v2/nicolet/tipos-usuario` | Gestión de roles (`ADMINISTRADOR`, `CLIENTE`, `PROFESIONAL`) |

---

## Ejecución de Pruebas Automatizadas

El proyecto cuenta con una suite completa de **61 pruebas unitarias y de integración** (Modelos, Repositorios, Servicios y Controladores MockMvc):

```powershell
.\mvnw.cmd test
```

Resultado esperado:
```text
[INFO] Results:
[INFO] Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## Flujo de Trabajo con Git y GitHub

1. **Sincronizar cambios:** `git pull origin main`
2. **Crear ramas de trabajo:** `git checkout -b feature/nombre-tarea`
3. **Guardar y subir cambios:** `git add .` $\rightarrow$ `git commit -m "..."` $\rightarrow$ `git push -u origin feature/nombre-tarea`
4. **Integración:** Crear Pull Request hacia `Test` o `main`.
