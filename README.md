# Nicolet Estudio - Sistema de Agendamiento de Citas

Sistema integral de gestion y agendamiento de citas para Nicolet Estudio, desarrollado con arquitectura cliente-servidor basada en Spring Boot, Spring Security, JWT, MySQL y Frontend Web.

---

## Estructura del Proyecto

```text
Agendamiento-Cita-Main/
├── src/                          # Backend: Codigo fuente Java Spring Boot
│   ├── main/java/cl/nicolet/backend/
│   │   ├── config/              # Configuraciones (Swagger / OpenAPI, DataInitializer)
│   │   ├── controller/          # Controladores REST API (Auth, Citas, Servicios, Usuarios, etc.)
│   │   ├── dto/                 # Objetos de Transferencia de Datos (AuthResponseDTO, LoginRequestDTO, etc.)
│   │   ├── exception/           # Manejador global de excepciones
│   │   ├── model/               # Entidades JPA (Usuario, Cliente, Trabajador, Cita, Servicio, etc.)
│   │   ├── repository/          # Repositorios Spring Data JPA
│   │   ├── security/            # Modulo de Seguridad (JwtUtils, JwtFilter, Rate Limiting, SecurityConfig)
│   │   ├── service/             # Logica de negocio (Agendamiento, Antisolapamiento, etc.)
│   │   └── BackendApplication.java
│   └── main/resources/
│       ├── application.properties      # Configuracion de base de datos MySQL y secreto JWT
│       └── application-dev.properties  # Perfil de desarrollo alternativo
├── db/                           # Base de Datos
│   ├── init.sql                 # Script de creacion de 8 tablas y datos semilla
│   └── README.md                # Documentacion detallada de la base de datos
├── front/                        # Frontend Web (HTML / CSS / JS)
│   ├── login.html               # Pantalla de inicio de sesion y autenticacion
│   ├── index.html               # Pantalla de gestion de usuarios y roles
│   ├── citas.html               # Pantalla interactiva de agendamiento de citas
│   ├── css/
│   │   └── index.css            # Estilos visuales compartidos
│   ├── js/
│   │   ├── auth.js              # Modulo de sesion JWT, proteccion de rutas y fetch autenticado
│   │   ├── index.js             # Logica e integracion REST de usuarios
│   │   └── citas.js             # Logica e integracion REST de citas y modales rapidos
│   └── README.md                # Guia de uso del frontend
├── docker-compose.yml            # Orquestacion de MySQL 8.0 y phpMyAdmin
├── Dockerfile                    # Empaquetado del microservicio Backend
├── escalada_prioridades.html     # Tablero interactivo de seguimiento de tareas y prioridades (5 Niveles)
├── informe_integracion_webpay_nicolet.html # Informe de arquitectura oficial para pasarela Transbank Webpay
├── README_AVANCE_REQUERIMIENTOS.md # Matriz de avance de 33 requerimientos (RF y RNF) y resumen de informes
├── start.ps1                     # Script de inicio seguro en PowerShell (libera puerto 8080)
├── start.cmd                     # Script de inicio seguro en CMD (libera puerto 8080)
├── mvnw / mvnw.cmd               # Wrapper de Maven
├── pom.xml                       # Dependencias y build del proyecto
└── README.md                     # Guia principal de instalacion y ejecucion
```

---

## 📚 Documentacion y Reportes Tecnicos del Proyecto

Para conocer el estado de avance de los requerimientos y las especificaciones de diseño, consulta:

1. **[README_AVANCE_REQUERIMIENTOS.md](README_AVANCE_REQUERIMIENTOS.md):** Matriz detallada del avance de los **33 requerimientos** (21 RF y 12 RNF), resumen ejecutivo y estado de implementación.
2. **[escalada_prioridades.html](escalada_prioridades.html):** Tablero visual e interactivo de **Escalada de Prioridades** con 5 niveles de ejecución, 20 tareas con DoD y guardado de avance.
3. **[informe_integracion_webpay_nicolet.html](informe_integracion_webpay_nicolet.html):** Informe técnico completo de integración con **Transbank Webpay Plus REST v1.2** (flujos, DDL MySQL, código Java, voucher y tarjetas de prueba).

---

## Seguridad y Autenticacion

El sistema implementa una arquitectura de seguridad basada en Spring Security y JSON Web Tokens (JJWT 0.12.5):

1. **Cifrado BCrypt:** Todas las contraseñas se almacenan con hash criptografico BCrypt y sal aleatoria. Las contraseñas en texto plano no se persisten en base de datos.
2. **Sanitizacion de DTOs:** La entidad de salida `UsuarioDTO` y las respuestas del sistema nunca retornan contraseñas ni hashes al cliente.
3. **Tokens JWT Stateless:** Las peticiones a la API protegida requieren la cabecera `Authorization: Bearer <token>`.
4. **Proteccion contra Fuerza Bruta:** Se implementa control de tasa (`LoginAttemptService`) que bloquea temporalmente por 15 minutos (HTTP 429) tras 5 intentos fallidos consecutivos, evitando ataques de diccionario y sobrecarga de CPU sobre BCrypt.
5. **Anti-Enumeracion de Cuentas:** Las respuestas ante credenciales invalidas y usuarios inexistentes entregan el mismo mensaje generico y codigo HTTP 401.
6. **Validacion de Longitud Maxima:** La contraseña de entrada esta limitada a 72 caracteres para respetar la especificacion de BCrypt y mitigar ataques de denegacion de servicio (DoS).

### Cuentas Semilla para Desarrollo y Pruebas

El sistema inicializa automaticamente las siguientes cuentas en la base de datos a traves de `DataInitializer`:

| Rol | Correo | Contraseña | Descripcion |
|---|---|---|---|
| Administrador (Garantizado) | `admin.test@nicolet.cl` | `AdminTest123!` | Cuenta inmune a bloqueos con restablecimiento automatico |
| Administrador | `admin@nicolet.cl` | `Admin123!` | Administrador estandar del sistema |
| Especialista | `camila.silva@estudio.cl` | `pro2026` | Profesional asignada para atencion de citas |
| Cliente | `martina.contreras@gmail.com` | `123412` | Cliente registrado con citas previas |

---

## Requisitos Previos

1. **Java Development Kit (JDK 17 o superior):**
   - Instalacion en Windows (PowerShell):
     ```powershell
     winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-source-agreements --accept-package-agreements
     ```
   - Verificacion:
     ```bash
     java -version
     ```

2. **Base de Datos (MySQL 8.0 o Docker):**
   - Opcion Docker: Docker Desktop instalado.
   - Opcion Local: MySQL Server local en puerto 3306.

---

## Guia Paso a Paso para Ejecutar el Proyecto

### Paso 1: Iniciar la Base de Datos con Docker

Desde la raiz del proyecto:
```bash
docker compose up -d
```
Esto creara:
- Contenedor MySQL en puerto `3306` (Base de datos: `universidad_backend`).
- Panel phpMyAdmin en `http://localhost:8081` (Usuario: `desarrollador` / Contraseña: `password_seguro_123`).

### Paso 2: Iniciar el Backend (Spring Boot)

En Windows PowerShell:
```powershell
.\start.ps1
```
O directamente con Maven Wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```
El servidor estara disponible en `http://localhost:8080`.

### Paso 3: Abrir el Frontend

Abre en tu navegador:
1. `front/login.html`: Pantalla de inicio de sesion (permite ingreso con credenciales o mediante el boton de acceso rapido de prueba).
2. Tras iniciar sesion, el sistema redirige automaticamente a `front/citas.html` manteniendo el token de sesion.

---

## Documentacion de la API (Swagger UI / OpenAPI)

Con el backend en ejecucion, la documentacion interactiva se encuentra disponible en:

**http://localhost:8080/doc/swagger-ui.html**

### Endpoints Principales

#### 1. Autenticacion
| Metodo | Endpoint | Descripcion |
|---|---|---|
| `POST` | `/api/v2/nicolet/auth/login` | Inicia sesion, valida credenciales y entrega token JWT |
| `GET` | `/api/v2/nicolet/auth/me` | Retorna los datos del usuario actualmente autenticado |

#### 2. Citas (Agendamiento Central)
| Metodo | Endpoint | Descripcion |
|---|---|---|
| `GET` | `/api/v2/nicolet/citas` | Lista todas las citas agendadas |
| `GET` | `/api/v2/nicolet/citas/{id}` | Busca una cita por ID unico |
| `GET` | `/api/v2/nicolet/citas/codigo/{codigo}` | Busca cita por codigo de reserva (ej: `RES-2026-0001`) |
| `GET` | `/api/v2/nicolet/citas/trabajador/{id}` | Agenda de un profesional especifico |
| `GET` | `/api/v2/nicolet/citas/cliente/{id}` | Historial de citas de un cliente |
| `POST` | `/api/v2/nicolet/citas` | Agenda una nueva cita con validacion antisolapamiento |
| `PATCH` | `/api/v2/nicolet/citas/{id}/estado` | Actualiza estado de una cita (`COMPLETADA`, `CANCELADA`) |

#### 3. Servicios
| Metodo | Endpoint | Descripcion |
|---|---|---|
| `GET` | `/api/v2/nicolet/servicios` | Catalogo de servicios activos |
| `GET` | `/api/v2/nicolet/servicios/{id}` | Detalle de un servicio por ID |
| `POST` | `/api/v2/nicolet/servicios` | Registra un nuevo servicio |
| `PUT` | `/api/v2/nicolet/servicios/{id}` | Actualiza un servicio |
| `DELETE` | `/api/v2/nicolet/servicios/{id}` | Baja logica de un servicio |

#### 4. Clientes y Trabajadores
| Metodo | Endpoint | Descripcion |
|---|---|---|
| `GET` | `/api/v2/nicolet/clientes` | Lista todos los clientes |
| `POST` | `/api/v2/nicolet/clientes` | Crea perfil especializado de cliente |
| `GET` | `/api/v2/nicolet/trabajadores` | Lista todos los trabajadores activos |
| `POST` | `/api/v2/nicolet/trabajadores` | Crea perfil especializado de trabajador |

#### 5. Usuarios y Roles
| Metodo | Endpoint | Descripcion |
|---|---|---|
| `GET` / `POST` | `/api/v2/nicolet/usuarios` | Gestion de cuentas de usuario base (sin password en salida) |
| `GET` / `POST` | `/api/v2/nicolet/tipos-usuario` | Gestion de roles (`ADMINISTRADOR`, `CLIENTE`, `PROFESIONAL`) |

---

## Pruebas Automatizadas

El proyecto incluye 81 pruebas automatizadas unitarias, de integracion y de seguridad que verifican modelos, repositorios, servicios, controladores y politicas de proteccion de autenticacion:

```powershell
.\mvnw.cmd test
```

Resultado de ejecucion:
```text
[INFO] Results:
[INFO] Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
