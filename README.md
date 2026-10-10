# Nicolet Estudio - Sistema de Agendamiento de Citas

Sistema integral de gestion y agendamiento de citas para Nicolet Estudio, desarrollado con arquitectura cliente-servidor basada en Spring Boot, Spring Security, JWT, MySQL y Frontend Web.

---

## Indice de Contenidos

1. [Estructura del Proyecto](#estructura-del-proyecto)
2. [Seguridad y Autenticacion](#seguridad-y-autenticacion)
3. [Requisitos Previos](#requisitos-previos)
4. [Guia de Instalacion y Ejecucion](#guia-de-instalacion-y-ejecucion)
5. [Documentacion de la API (Swagger UI / OpenAPI)](#documentacion-de-la-api-swagger-ui--openapi)
6. [Matriz de Avance de Requerimientos](#matriz-de-avance-de-requerimientos)
   - [Resumen Ejecutivo de Estado](#resumen-ejecutivo-de-estado)
   - [Requerimientos Funcionales (RF01 - RF21)](#requerimientos-funcionales-rf01---rf21)
   - [Requerimientos No Funcionales (RNF01 - RNF12)](#requerimientos-no-funcionales-rnf01---rnf12)
7. [Arquitectura de Integracion Transbank Webpay Plus](#arquitectura-de-integracion-transbank-webpay-plus)
   - [Diagnostico y Viabilidad](#diagnostico-y-viabilidad)
   - [Especificacion Tecnica REST v1.2](#especificacion-tecnica-rest-v12)
   - [SDK Oficial Java](#sdk-oficial-java)
   - [Modelo de Datos de Pago (MySQL)](#modelo-de-datos-de-pago-mysql)
   - [Flujo Transaccional](#flujo-transaccional)
   - [Manejo de Aborto Voluntario y Timeout (TTL)](#manejo-de-aborto-voluntario-y-timeout-ttl)
   - [Voucher Regulatorio y Matriz de Pruebas](#voucher-regulatorio-y-matriz-de-pruebas)
8. [Hoja de Ruta y Escalada de Prioridades (5 Niveles)](#hoja-de-ruta-y-escalada-de-prioridades-5-niveles)
9. [Pruebas Automatizadas](#pruebas-automatizadas)

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
│       ├── application-dev.properties  # Perfil de desarrollo alternativo
│       └── static/                     # Frontend Unificado servido por Spring Boot
│           ├── admin/                  # Panel de Administracion (JWT, Citas y Usuarios)
│           │   ├── css/admin.css       # Estilos del panel de gestion
│           │   ├── js/                 # Modulos JS (auth.js, citas.js, index.js)
│           │   ├── login.html          # Pantalla de inicio de sesion y autenticacion
│           │   ├── citas.html          # Pantalla interactiva de gestion de citas
│           │   └── usuarios.html       # Pantalla de administracion de usuarios y roles
│           ├── css/styles.css          # Estilos de la web publica del salon
│           ├── img/                    # Imagenes del salon
│           ├── js/app.js               # Logica de navegacion publica
│           ├── index.html              # Portal publico del salon (inicio)
│           └── reservar.html           # Flujo de reserva para clientas
├── db/                           # Base de Datos
│   ├── init.sql                 # Script de creacion de 8 tablas y datos semilla
│   └── README.md                # Documentacion detallada de la base de datos
├── docker-compose.yml            # Orquestacion de MySQL 8.0 y phpMyAdmin
├── Dockerfile                    # Empaquetado del microservicio Backend
├── start.ps1                     # Script de inicio seguro en PowerShell (libera puerto 8080)
├── start.cmd                     # Script de inicio seguro en CMD (libera puerto 8080)
├── mvnw / mvnw.cmd               # Wrapper de Maven
├── pom.xml                       # Dependencias y build del proyecto
└── README.md                     # Guia principal y documentacion integral del proyecto
```

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

## Guia de Instalacion y Ejecucion

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

Con el backend en ejecucion, ingresa directamente desde tu navegador:
1. **Portal Publico de Clientas:** `http://localhost:8080/` (Catalogo de servicios, equipo y reservas).
2. **Panel de Administracion:** `http://localhost:8080/admin/login.html` (o haz clic en "Acceso profesionales" en la barra superior del portal publico).
3. Tras iniciar sesion con JWT, el sistema redirige automaticamente a `http://localhost:8080/admin/citas.html` manteniendo el token de sesion para gestionar citas y usuarios.

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

## Matriz de Avance de Requerimientos

### Resumen Ejecutivo de Estado

| Metrica | Valor | Detalle |
|---|---|---|
| **Total de Requerimientos** | **33** | 21 Funcionales (RF) + 12 No Funcionales (RNF) |
| **Requerimientos Implementados** | **6 / 33 (18.2%)** | Nivel 1 Seguridad y Auth completo + Core base de Citas/Servicios |
| **En Proceso / Arquitectura Validada** | **8 / 33 (24.2%)** | Nivel 2: Pasarela Webpay Plus REST v1.2, RUT y Disponibilidad |
| **Planificados en Roadmap** | **19 / 33 (57.6%)** | Niveles 3, 4 y 5: Stock, Agenda Profesional, Fichas y Analitica |
| **Pruebas Automatizadas Backend** | **81 Tests (100% Pass)** | Tests unitarios, repositorios, servicios y seguridad Spring Boot |
| **Pasarela de Pagos Principal** | **Transbank Webpay Plus** | REST API v1.2 con SDK oficial Java 6.2.1 |

```text
Progreso General del Proyecto:
[========----------------------------------------] 24% (Implementado + Especificado)
```

### Requerimientos Funcionales (RF01 - RF21)

| Codigo | Requerimiento Funcional | Nivel / Tarea | Estado Actual | Evidencia en Codigo / Especificacion |
|---|---|---|---|---|
| **RF01** | **Auto-Registro de Clientes** con validacion de RUT chileno | Nivel 2 - T2.2 | Planificado | Algoritmo Modulo 11 `@ValidRut` y endpoint `POST /auth/registro-cliente` definidos |
| **RF02** | **Autenticacion de Usuarios** (Login con correo y contraseña) | Nivel 1 - T1.2, T1.3 | Implementado | `AuthController.java`, `JwtUtils.java`, `LoginRequestDTO.java`, `login.html` |
| **RF03** | **Control de Acceso por Roles** (ADMIN, CLIENTE, PROFESIONAL) | Nivel 1 - T1.3 | Implementado | `SecurityConfig.java`, `TipoUsuario.java`, RBAC con filtros JWT |
| **RF04** | **Consulta de Bloques Horarios Disponibles** por profesional/fecha | Nivel 2 - T2.1 | En Diseno | Endpoint `GET /citas/disponibilidad` calculado restando reservas y descansos |
| **RF05** | **Seleccion de Servicio y Profesional** antes de agendar | Nivel 2 - T2.1 | Parcial | Catalogos activos en `ServicioService` y `TrabajadorService`, selector UI base |
| **RF06** | **Pago de Reserva / Abono Minimo ($5.000 CLP)** via Webpay | Nivel 2 - T2.3, T2.4 | Arquitectura Lista | DDL `pago`, SDK `transbank-sdk-java:6.2.1`, especificacion tecnica `WebpayService.java` |
| **RF07** | **Ciclo Transaccional de Cita** (`PENDIENTE` a `CONFIRMADA`) | Nivel 2 - T2.5, T2.6 | Arquitectura Lista | `CitaService.agendar()` retiene cupo; pasa a `CONFIRMADA` solo con commit exitoso |
| **RF08** | **Vista "Mi Agenda" por Profesional** (Calendario diario/semanal) | Nivel 3 - T3.4 | Roadmap | Filtrado de citas por trabajador autenticado |
| **RF09** | **Gestion de Estados de Atencion** (`EN_ATENCION`, `COMPLETADA`, etc.) | Nivel 3 - T3.4 | Parcial | `EstadoCita.java` soporta estados; pendiente modal frontend para cambio operativo |
| **RF10** | **Ficha Tecnica Unica de Cliente** (Alergias, tipo piel/uñas) | Nivel 4 - T4.1 | Roadmap | Modelo `ficha_cliente` con codigo unico (ej: `FCH-2026-0042`) |
| **RF11** | **Registro Obligatorio de Observaciones Tecnicas** al finalizar | Nivel 4 - T4.2 | Roadmap | Modal obligatorio de cierre con notas de esmaltes, geles o tintes usados |
| **RF12** | **Consulta de Historial Estetico** en atenciones previas | Nivel 4 - T4.2 | Roadmap | Consulta cronologica de atenciones previas del cliente |
| **RF13** | **Catalogo de Insumos y Productos** (CRUD) | Nivel 3 - T3.1 | Roadmap | Entidad `producto_insumo`, unidad de medida y stock minimo |
| **RF14** | **Deduccion Automatica de Stock** por Servicio Realizado | Nivel 3 - T3.2 | Roadmap | Tabla intermedia `servicio_insumo`; hook al marcar cita `COMPLETADA` |
| **RF15** | **Alertas de Stock Critico** por debajo del umbral minimo | Nivel 3 - T3.3 | Roadmap | Endpoint `GET /productos/alertas-stock` y alerta visual en interfaz |
| **RF16** | **Control de Movimientos de Inventario** (Entradas, salidas, mermas) | Nivel 3 - T3.1 | Roadmap | Tabla `movimiento_stock` vinculada a citas o ingresos manuales |
| **RF17** | **Reporte de Clientes Atendidos por Profesional** | Nivel 5 - T5.1 | Roadmap | Endpoint estadistico agrupado por trabajador y rango de fechas |
| **RF18** | **Dashboard Gerencial y Analitica de Ingresos** | Nivel 5 - T5.1 | Roadmap | Panel analitico con suma de montos desde `pago.estado == 'APROBADO'` |
| **RF19** | **Expediente Historico 360 del Cliente** | Nivel 4 - T4.2 | Roadmap | Vista centralizada de fichas, citas, pagos y observaciones tecnicas |
| **RF20** | **Notificaciones Automaticas por Correo Electronico** | Nivel 4 - T4.3 | Roadmap | Spring Mail con plantillas HTML y comprobante Webpay integrado |
| **RF21** | **Motor de Fidelizacion de Clientes Inactivos** (>30 dias) | Nivel 5 - T5.2 | Roadmap | Job `@Scheduled` que detecta clientas sin visitas para contacto preventivo |

### Requerimientos No Funcionales (RNF01 - RNF12)

| Codigo | Requerimiento No Funcional | Nivel / Tarea | Estado Actual | Evidencia en Codigo / Especificacion |
|---|---|---|---|---|
| **RNF01** | **Seguridad en la API REST (JWT y Whitelist)** | Nivel 1 - T1.2 / Nivel 2 - T2.4 | Implementado | Filtro `JwtAuthenticationFilter`, rutas publicas parametrizadas en `SecurityConfig` |
| **RNF02** | **Almacenamiento Seguro de Contraseñas (BCrypt)** | Nivel 1 - T1.1 | Implementado | Bean `BCryptPasswordEncoder`, contraseñas semilla actualizadas en `DataInitializer` |
| **RNF03** | **Sanitizacion de Salidas JSON (Sin passwords)** | Nivel 1 - T1.1 | Implementado | `UsuarioDTO.java` y endpoints de consulta sin campos de contraseña |
| **RNF04** | **Resiliencia y Expiracion TTL (15 min por abandono)** | Nivel 2 - T2.5 | Arquitectura Lista | Cron `@Scheduled` `ExpiracionCitasJob.java` diseñado en arquitectura |
| **RNF05** | **Concurrencia y Antisolapamiento de Agenda** | Nivel 2 - T2.1 | Implementado Base | Validacion de traslape horario de citas activas en `CitaService.java` |
| **RNF06** | **Tiempos de Respuesta Optimos (<200ms en API)** | Nivel 2 - T2.1 | Implementado Base | Indices JPA en repositorios y consultas con clave foranea indexada |
| **RNF07** | **Cumplimiento PCI-DSS en Pasarela de Pago** | Nivel 2 - T2.3, T2.7 | Arquitectura Lista | El servidor jamas almacena ni procesa numeros PAN completos ni CVV |
| **RNF08** | **Voucher Regulatorio Oficial Transbank** | Nivel 2 - T2.6 | Arquitectura Lista | Especificacion visual del comprobante con los 8 campos obligatorios TBK |
| **RNF09** | **Auditoria Transaccional de Operaciones Criticas** | Nivel 5 - T5.3 | Roadmap | Tabla `auditoria_operacion` e interceptor de cambios en citas y stock |
| **RNF10** | **Disponibilidad y Tolerancia a Fallos** | Transversal | Implementado Base | Docker Compose para aislamiento de base de datos MySQL 8 y microservicio |
| **RNF11** | **Diseño Responsivo y Usabilidad (UX Movil)** | Frontend | Implementado Base | Formularios CSS adaptados a dispositivos moviles en `front/css/` |
| **RNF12** | **Respaldo Periodico de Datos (Backups MySQL)** | Nivel 5 - T5.3 | Roadmap | Script de respaldo automatizado `mysqldump` con rotacion programada |

---

## Arquitectura de Integracion Transbank Webpay Plus

### Diagnostico y Viabilidad
- **Situacion previa:** Las citas se creaban en estado `CONFIRMADA` sin exigir pago previo, exponiendo al negocio a inasistencias ("no-shows") y falta de control de caja.
- **Viabilidad tecnica:** El stack actual (Java 17 + Spring Boot 3.2.5 + MySQL 8) es plenamente compatible con los estandares REST de Transbank Developers.
- **Impacto proyectado:** Reduccion estimada del 85% de inasistencias al exigir abono minimo obligatorio de $5.000 CLP.

### Especificacion Tecnica REST v1.2
- **Ambiente de Integracion (Testing):**
  - Base URL: `https://webpay3gint.transbank.cl`
  - Codigo de Comercio: `597055555532`
  - Api Key Secret: `579B532A7440BB0C9079DED94D31EA1615BACEB56610332264630D42D0A36B1C`
- **Endpoints Nucleares de Transbank:**
  1. `POST /rswebpaytransaction/api/webpay/v1.2/transactions` (Crear transaccion: retorna `url` y `token_ws`).
  2. `PUT /rswebpaytransaction/api/webpay/v1.2/transactions/{token}` (Commit bancario obligatorio).
  3. `GET /rswebpaytransaction/api/webpay/v1.2/transactions/{token}` (Consulta de estado).
  4. `POST /rswebpaytransaction/api/webpay/v1.2/transactions/{token}/refunds` (Reversas y anulaciones).

### SDK Oficial Java
Se utiliza el SDK oficial `com.github.transbankdevelopers:transbank-sdk-java:6.2.1`:
- Inicializacion simplificada: `WebpayPlus.Transaction.buildForIntegration()`.
- Homologacion garantizada y compatibilidad completa con DTOs oficiales de Transbank.
- Cobertura ante actualizaciones regulatorias de cabeceras HTTP.

### Modelo de Datos de Pago (MySQL)
```sql
CREATE TABLE pago (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cita_id BIGINT NOT NULL,
    buy_order VARCHAR(26) NOT NULL UNIQUE,
    session_id VARCHAR(61) NOT NULL,
    token_ws VARCHAR(64) UNIQUE,
    monto DECIMAL(10,2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'INICIADO', -- INICIADO, APROBADO, RECHAZADO, ABORTADO
    codigo_autorizacion VARCHAR(10),
    ultimos_cuatro_digitos VARCHAR(4),
    tipo_pago VARCHAR(10),                          -- VD (Debito), VN (Credito), VC (Cuotas)
    codigo_respuesta INT,                           -- 0 = Aprobada
    fecha_transaccion DATETIME,
    vci VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_cita FOREIGN KEY (cita_id) REFERENCES cita(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Flujo Transaccional
```text
1. Cliente selecciona hora y servicio en frontend.
2. Backend persiste Cita en estado temporal PENDIENTE (cupo reservado por 15 min).
3. Backend ejecuta tx.create() hacia Transbank con buyOrder y monto real del servicio.
4. Frontend recibe { url, token } y ejecuta auto-submit POST al portal de Transbank.
5. Cliente paga de forma segura en servidores de Transbank (3D Secure).
6. Redireccion bancaria hacia callback publico: /api/v2/nicolet/pagos/webpay/retorno.
7. Backend invoca tx.commit(token_ws); si responseCode == 0 -> Cita CONFIRMADA y Pago APROBADO.
8. Despliegue de Voucher normativo con codigo de autorizacion y comprobante.
```

### Manejo de Aborto Voluntario y Timeout (TTL)
- **Aborto Voluntario:** Si el cliente hace clic en "Anular compra" en el portal Webpay, Transbank retorna los parametros `TBK_TOKEN` y `TBK_ORDEN_COMPRA`. El backend detecta estos parametros, marca el pago como `ABORTADO`, cancela la cita y libera el cupo inmediatamente.
- **Expiracion Automatica (`ExpiracionCitasJob`):** Tarea programada con `@Scheduled(cron = "0 */5 * * * *")` que cancela citas que permanezcan mas de 15 minutos en estado `PENDIENTE` sin confirmacion de pago.

### Voucher Regulatorio y Matriz de Pruebas
Campos obligatorios requeridos por Transbank para certificacion:
- Nombre del comercio: **Nicolet Estudio**
- Codigo de Reserva (ej: `RES-2026-F9A1B2`)
- Orden de Compra (`OC-...`)
- Codigo de Autorizacion (ej: `1213`)
- Tipo de Transaccion (`Venta Debito - VD` / `Venta Credito - VN/VC`)
- Tarjeta enmascarada (`**** **** **** 6623`)
- Fecha y hora de transaccion
- Monto Total Pagado en CLP

#### Matriz de Pruebas (Tarjetas Oficiales de Integracion)
1. **Debito Redcompra:** Tarjeta `6623 0000 0000 0001` -> Codigo de respuesta `0`, tipo `VD`.
2. **Credito Sin Cuotas:** Tarjeta `6623 0000 0000 0002` -> Codigo de respuesta `0`, tipo `VN`.
3. **Credito En Cuotas:** Tarjeta `6623 0000 0000 0003` -> Codigo de respuesta `0`, tipo `VC`.
4. **Rechazo por Fondos:** Tarjeta `6623 0000 0000 0004` -> Codigo diferente de 0, reversa automatica y cancelacion.
5. **Cancelacion por Usuario:** Cualquier tarjeta -> Retorno con `TBK_TOKEN`, liberacion inmediata de cupo.

---

## Hoja de Ruta y Escalada de Prioridades (5 Niveles)

El desarrollo del sistema se organiza de forma secuencial en 5 niveles:

```text
[Nivel 1] SEGURIDAD Y AUTH (3 Tareas) - COMPLETADO
├── T1.1: Cifrado BCrypt y Sanitizacion de DTOs
├── T1.2: Tokens JWT y Spring Security (incluye whitelist para retorno Webpay)
└── T1.3: Pantalla de Login y Control de Sesion en Frontend

[Nivel 2] CORE TRANSACCIONAL Y WEBPAY PLUS (7 Tareas) - SIGUIENTE PRIORIDAD
├── T2.1: Selector de Horarios Libres y Disponibilidad (RF04, RF05)
├── T2.2: Auto-Registro de Clientes y Validacion de RUT Modulo 11 (RF01)
├── T2.3: Webpay Fase 1: SDK Oficial, Esquema DDL y Entidad de Pago (RF06, RNF07)
├── T2.4: Webpay Fase 2: Backend WebpayService, Endpoints REST y Aborto (RF06, RNF01)
├── T2.5: Webpay Fase 3: Cita PENDIENTE y Cron TTL de Expiracion (RF07, RNF04)
├── T2.6: Webpay Fase 4: Frontend (Auto-Submit POST) y Voucher Oficial (RF06, RF07)
└── T2.7: Webpay Fase 5: Matriz de 5 Tarjetas Test y Homologacion TBK (RNF07, RNF08)

[Nivel 3] AGENDA Y CONTROL DE INSUMOS (4 Tareas) - SPRINT 3
├── T3.1: CRUD de Insumos y Registro de Movimientos de Stock (RF13, RF16)
├── T3.2: Asociacion Servicio-Insumos y Deduccion Automatica (RF14)
├── T3.3: Alertas de Stock Critico en Dashboard (RF15)
└── T3.4: Vista "Mi Agenda" por Profesional y Gestion de Estados (RF08, RF09)

[Nivel 4] FICHA TECNICA Y NOTIFICACIONES (3 Tareas) - SPRINT 4
├── T4.1: Ficha Tecnica Unica de Cliente (Expediente Estetico) (RF10)
├── T4.2: Observaciones Tecnicas Obligatorias e Historial 360 (RF11, RF12, RF19)
└── T4.3: Notificaciones por Correo Electronico con Voucher Webpay (RF20)

[Nivel 5] GESTION, AUDITORIA Y CALIDAD (3 Tareas) - SPRINT 5
├── T5.1: Dashboard Gerencial, Ingresos Webpay y Conciliacion (RF17, RF18)
├── T5.2: Motor de Fidelizacion de Clientes Inactivos (@Scheduled) (RF21)
└── T5.3: Auditoria Transaccional y Backups Automaticos MySQL (RNF09, RNF12)
```

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
