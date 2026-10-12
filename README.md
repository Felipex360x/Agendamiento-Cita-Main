# Nicolet Estudio - Sistema de Agendamiento de Citas

Sistema integral de gestión y agendamiento de citas para Nicolet Estudio, desarrollado con arquitectura cliente-servidor basada en Spring Boot, Spring Security, JWT, MariaDB/MySQL y Frontend Web responsivo.

---

## Índice de Contenidos

1. [Estructura del Proyecto](#estructura-del-proyecto)
2. [Módulos del Panel de Administración (`/admin`)](#módulos-del-panel-de-administración-admin)
   - [Aislamiento y Navegación Interna](#aislamiento-y-navegación-interna)
   - [Centralización de Estilos en `admin.css`](#centralización-de-estilos-en-admincss)
   - [Descripción de Vistas Administrativas](#descripción-de-vistas-administrativas)
3. [Seguridad y Autenticación](#seguridad-y-autenticación)
   - [Políticas y Cifrado](#políticas-y-cifrado)
   - [Cuentas Semilla Preconfiguradas](#cuentas-semilla-preconfiguradas)
4. [Requisitos Previos](#requisitos-previos)
5. [Guía de Instalación y Ejecución](#guía-de-instalación-y-ejecución)
6. [Documentación de la API (Swagger UI / OpenAPI)](#documentación-de-la-api-swagger-ui--openapi)
7. [Matriz de Avance y Cumplimiento de Requerimientos](#matriz-de-avance-y-cumplimiento-de-requerimientos)
   - [Resumen Ejecutivo de Estado](#resumen-ejecutivo-de-estado)
   - [Requerimientos Funcionales (RF01 - RF21)](#requerimientos-funcionales-rf01---rf21)
   - [Requerimientos No Funcionales (RNF01 - RNF12)](#requerimientos-no-funcionales-rnf01---rnf12)
8. [Arquitectura de Integración Transbank Webpay Plus](#arquitectura-de-integración-transbank-webpay-plus)
9. [Hoja de Ruta y Escalada de Prioridades](#hoja-de-ruta-y-escalada-de-prioridades)
10. [Pruebas Automatizadas](#pruebas-automatizadas)

---

## Estructura del Proyecto

```text
Agendamiento-Cita-Main/
├── src/                          # Backend: Código fuente Java Spring Boot
│   ├── main/java/cl/nicolet/backend/
│   │   ├── config/              # Configuraciones (OpenAPI/Swagger, DataInitializer con seeds)
│   │   ├── controller/          # Controladores REST API (Auth, Citas, Servicios, Disponibilidad, etc.)
│   │   ├── dto/                 # DTOs (CitaCreateDTO, BloqueHorarioDTO, UsuarioCreateDTO, etc.)
│   │   ├── exception/           # Manejador global de excepciones HTTP
│   │   ├── model/               # Entidades JPA (Usuario, Cliente, Trabajador, Cita, Servicio, etc.)
│   │   ├── repository/          # Repositorios Spring Data JPA con queries indexadas
│   │   ├── security/            # Seguridad (JwtUtils, JwtFilter, Rate Limiting, SecurityConfig)
│   │   ├── service/             # Lógica de negocio (Agendamiento, Antisolapamiento, Disponibilidad)
│   │   └── BackendApplication.java
│   └── main/resources/
│       ├── application.properties      # Configuración de base de datos MySQL y secreto JWT
│       ├── application-dev.properties  # Perfil de desarrollo
│       └── static/                     # Frontend Unificado servido por Spring Boot
│           ├── admin/                  # Panel Operativo y de Administración (Exclusivo Admin/Staff)
│           │   ├── css/admin.css       # Hoja de estilos centralizada (Sin <style> inline en HTML)
│           │   ├── js/                 # Controladores Frontend JavaScript
│           │   │   ├── auth.js         # Módulo de sesión JWT, authenticatedFetch y badges
│           │   │   ├── citas.js        # Lógica de gestión de citas y visualización de usuarios
│           │   │   ├── disponibilidad.js # Motor interactivo del simulador de horarios
│           │   │   ├── historial.js    # Ficha técnica y tratamientos de clientas por trabajador
│           │   │   ├── index.js        # CRUD y búsqueda de usuarios y roles
│           │   │   ├── panel-trabajador.js # Portal para especialistas y confirmación de citas
│           │   │   ├── reserva-cliente.js # Flujo guiado de reserva para clientes con registro
│           │   │   └── servicios.js    # CRUD de servicios y catálogo comercial
│           │   ├── citas.html          # Gestión y agendamiento general de citas (Vista admin)
│           │   ├── disponibilidad.html # Simulador en tiempo real de disponibilidad por especialista
│           │   ├── historial.html      # Historial de tratamientos de clientas por trabajador
│           │   ├── login.html          # Acceso al sistema y registro de usuarios
│           │   ├── panel-trabajador.html # Portal exclusivo para trabajadores (Confirmar citas)
│           │   ├── pruebas.html        # Redirección oficial hacia disponibilidad.html
│           │   ├── reserva-cliente.html # Flujo guiado para agendar citas desde la perspectiva cliente
│           │   ├── servicios.html      # Creación y administración del catálogo de servicios
│           │   └── usuarios.html       # Administración de usuarios y tipos de rol
│           ├── css/styles.css          # Estilos del portal público
│           ├── img/                    # Galería visual del salón
│           ├── js/app.js               # Navegación del portal público
│           ├── index.html              # Portal de bienvenida público
│           └── reservar.html           # Reservas públicas
├── db/                           # Base de Datos
│   ├── init.sql                 # Script DDL de 8 tablas relacionales y datos semilla
│   └── README.md                # Diccionario de datos
├── docker-compose.yml            # Orquestación de MySQL 8.0 y phpMyAdmin
├── Dockerfile                    # Contenerización Docker multi-stage del backend
├── start.ps1                     # Script de inicio seguro en PowerShell (libera puerto 8080)
├── start.cmd                     # Script de inicio seguro en CMD (libera puerto 8080)
├── mvnw / mvnw.cmd               # Wrapper Maven
├── pom.xml                       # Dependencias Maven y configuración del build
└── README.md                     # Documentación integral del proyecto
```

---

## Módulos del Panel de Administración (`/admin`)

El espacio administrativo dentro de `src/main/resources/static/admin/` fue reestructurado y expandido para cumplir de forma estricta con todas las directrices operativas del negocio:

### Aislamiento y Navegación Interna
- **Regla de Conectividad Estricta:** Todas las vistas HTML dentro de `static/admin/` se conectan **exclusivamente entre sí** a través del `navbar` unificado y sus botones internos. Se eliminaron todos los enlaces directos hacia páginas externas (`../index.html`, `../servicios.html`, etc.).
- El isotipo/marca `nicolett. STUDIO FANTASY • ADMIN` en la cabecera actúa como enlace raíz al panel de citas (`citas.html`).
- El pie de página administrativo mantiene la identidad corporativa y enlaces estrictamente contextuales.

### Centralización de Estilos en `admin.css`
- **Eliminación Total de CSS Inline:** Se extrajeron todas las etiquetas `<style>` internas de los archivos HTML administrativos (`citas.html`, `usuarios.html`, `login.html`, etc.).
- Todo el diseño (grillas horarias, chips de estados, selectores de tratamientos, tarjetas de especialistas, modales y banners) se centralizó de manera modular en `static/admin/css/admin.css`.

### Descripción de Vistas Administrativas

| Archivo HTML | Controlador JS | Funcionalidad y Reglas de Negocio Implementadas |
|---|---|---|
| **`citas.html`** | `js/citas.js` | **Gestión Central de Citas:** Permite programar nuevas atenciones, cambiar estados (`COMPLETADA`, `CANCELADA`) y auditar la base de datos.<br>*(Requisito Cumplido)*: Despliega en vivo dos paneles con los usuarios registrados con rol **Trabajador** y **Cliente**, sincronizados automáticamente desde la API. |
| **`reserva-cliente.html`** | `js/reserva-cliente.js` | **Agendamiento Guiado para Clientes:** Flujo interactivo de 4 pasos:<br>1. Selección de tratamiento/servicio del catálogo.<br>2. Selección de especialista/trabajador.<br>3. Selección de fecha y bloque horario en tiempo real (solo bloques libres).<br>4. Si el cliente está autenticado agenda directamente; si no lo está, despliega formulario de registro inline, crea la cuenta y agenda la cita con estado `PENDIENTE`. |
| **`servicios.html`** | `js/servicios.js` | **Catálogo y Creación de Servicios:** Permite registrar nuevos tratamientos (`POST /servicios`), editar tarifa y duración, y dar de baja lógica.<br>*(Requisito Cumplido)*: Al crear un servicio, notifica con enlace directo y lo deja disponible de inmediato en todos los selectores de agendamiento. |
| **`disponibilidad.html`** | `js/disponibilidad.js` | **Simulador de Disponibilidad:** Conectado directamente a los trabajadores registrados (`/trabajadores`) y al motor de cálculo de jornada (`/disponibilidad`). Muestra bloques libres (verdes), descansos (amarillos) y horas ocupadas (grises). Permite seleccionar un bloque libre y pasar directo a agendar. |
| **`panel-trabajador.html`** | `js/panel-trabajador.js` | **Portal Exclusivo de Trabajadores:** Protegido con control de acceso por rol (restringe a clientes). Identifica al profesional en sesión y despliega sus citas asignadas.<br>*(Requisito Cumplido)*: Permite al trabajador **confirmar la cita con la clienta** (`PATCH /citas/{id}/estado` a `CONFIRMADA`), iniciar atención o cancelar. |
| **`historial.html`** | `js/historial.js` | **Historial de Tratamientos por Trabajador:** Permite filtrar por especialista para auditar todos los tratamientos realizados, clientas únicas atendidas e ingresos generados.<br>*(Requisito Cumplido)*: Modal de **Ficha Técnica de la Clienta** con el historial cronológico de todas las sesiones atendidas por ese profesional. |
| **`usuarios.html`** | `js/index.js` | **Gestión de Cuentas y Roles:** CRUD completo de usuarios con asignación de roles (`ADMINISTRADOR`, `CLIENTE`, `PROFESIONAL`). Sincroniza automáticamente los perfiles de `Cliente` y `Trabajador` en base de datos. |
| **`login.html`** | `js/auth.js` | **Acceso y Registro Unificado:** Autenticación JWT con protección ante fuerza bruta y cuentas de prueba rápidas preconfiguradas. |
| **`pruebas.html`** | - | Redirección transparente hacia el nuevo simulador oficial de disponibilidad (`disponibilidad.html`). |

---

## Seguridad y Autenticación

### Políticas y Cifrado
1. **Cifrado Criptográfico BCrypt:** Contraseñas protegidas con sal aleatoria. Las contraseñas en texto plano se migran y hashean automáticamente mediante `DataInitializer`.
2. **Sanitización de DTOs:** La entidad `UsuarioDTO` y los endpoints del sistema jamás exponen contraseñas ni hashes al cliente.
3. **JWT Stateless:** Peticiones protegidas exigen cabecera `Authorization: Bearer <token>`. Rutas públicas delimitadas en `SecurityConfig`: autenticación (`/auth/**`), catálogo de servicios (`GET /servicios/**`), catálogo de trabajadores (`GET /trabajadores/**`), disponibilidad (`GET /disponibilidad/**`) y documentación Swagger.
4. **Protección Anti-Fuerza Bruta:** `LoginAttemptService` bloquea temporalmente por 15 minutos (HTTP 429) tras 5 intentos fallidos consecutivos sobre un mismo correo.
5. **Anti-Enumeración:** Mismo mensaje y código HTTP 401 para credenciales inválidas y usuarios inexistentes.

### Cuentas Semilla Preconfiguradas

| Rol | Correo | Contraseña | Perfil Asociado |
|---|---|---|---|
| Administrador Garantizado | `admin.test@nicolet.cl` | `AdminTest123!` | Acceso total / Sin riesgo de bloqueo |
| Administrador Principal | `admin@nicolet.cl` | `Admin123!` | Administración general del salón |
| Administrador Titular | `maximo.rojas@estudio.cl` | `password_seguro_123` | Titular del estudio |
| Profesional / Trabajador | `camila.silva@estudio.cl` | `pro2026` | Especialista en Estilismo y Manicura (Turnos Lun-Vie 09:00 a 18:00) |
| Cliente Registrado | `martina.contreras@gmail.com` | `123412` | Clienta con citas e historial técnico |
| Cliente Pruebas | `marit@gmail.com` | `123456` | Clienta con cita PENDIENTE asignada |
| Cliente Pruebas | `gale@gmail.com` | `123456` | Cliente con perfil registrado para pruebas |
| Administrador Pruebas | `prueba1235@gmail.com` | `123456` | Administrador alternativo |

### Datos Semilla y Escenarios de Prueba en Base de Datos

Tanto `db/init.sql` como `DataInitializer.java` pueblan y sincronizan automáticamente la base de datos al iniciar:

| Registro / Código | Estado | Cliente | Especialista | Servicio | Propósito de Prueba en Módulos Admin |
|---|---|---|---|---|---|
| **RES-2026-0001** | `CONFIRMADA` | Martina Contreras | Camila Silva | Manicura Rusa | Visualización en agenda y simulación de disponibilidad |
| **RES-2026-0002** | `PENDIENTE` | Martina Soto (`marit@gmail.com`) | Camila Silva | Corte de Cabello | **Portal Trabajadores (`panel-trabajador.html`):** Permite a Camila presionar *"✓ Confirmar Cita con la Clienta"* pasando a `CONFIRMADA` |
| **RES-2026-0003** | `COMPLETADA` | Martina Contreras (`martina.contreras@gmail.com`) | Camila Silva | Tratamiento Facial | **Historial de Tratamientos (`historial.html`):** Permite auditar ingresos acumulados, notas técnicas y consultar la Ficha Técnica de la clienta |

---

## Requisitos Previos

1. **Java Development Kit (JDK 17 o superior):**
   ```bash
   java -version
   ```
2. **Base de Datos (MySQL 8.0 o Docker Desktop):**
   Puerto `3306` disponible para MySQL.

---

## Guía de Instalación y Ejecución

### 1. Iniciar la Base de Datos con Docker
```bash
docker compose up -d
```
- Contenedor MySQL 8.0 en puerto `3306` (Base de datos: `universidad_backend`).
- Panel phpMyAdmin en `http://localhost:8081` (Usuario: `desarrollador` / Contraseña: `password_seguro_123`).

### 2. Iniciar el Backend Spring Boot
En Windows PowerShell:
```powershell
.\start.ps1
```
O directamente con Maven Wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```
El servidor quedará activo en `http://localhost:8080`.

### 3. Acceso a las Aplicaciones
- **Portal Público:** `http://localhost:8080/`
- **Panel de Administración (Acceso Directo):** `http://localhost:8080/admin/citas.html` (redirige a login si no hay token).
- **Iniciar Sesión:** `http://localhost:8080/admin/login.html`
- **Agendamiento Cliente:** `http://localhost:8080/admin/reserva-cliente.html`
- **Portal de Trabajadores:** `http://localhost:8080/admin/panel-trabajador.html`
- **Simulador de Disponibilidad:** `http://localhost:8080/admin/disponibilidad.html`
- **Catálogo de Servicios:** `http://localhost:8080/admin/servicios.html`
- **Historial de Tratamientos:** `http://localhost:8080/admin/historial.html`

---

## Documentación de la API (Swagger UI / OpenAPI)

Disponible con el servidor iniciado en:

**http://localhost:8080/doc/swagger-ui.html**

### Endpoints Principales

| Módulo | Método | Endpoint | Descripción |
|---|---|---|---|
| **Autenticación** | `POST` | `/api/v2/nicolet/auth/login` | Login con credenciales, entrega token JWT y rol |
| | `POST` | `/api/v2/nicolet/auth/register` | Registro de nuevos clientes con generación de token |
| | `GET` | `/api/v2/nicolet/auth/me` | Consulta de perfil del usuario logueado |
| **Disponibilidad** | `GET` | `/api/v2/nicolet/disponibilidad` | Cálculo en vivo de bloques horarios según jornada, descansos y citas |
| **Citas** | `GET` | `/api/v2/nicolet/citas` | Listado general de citas |
| | `POST` | `/api/v2/nicolet/citas` | Agendamiento de cita con validación antisolapamiento |
| | `GET` | `/api/v2/nicolet/citas/trabajador/{id}` | Citas asignadas a un especialista específico |
| | `GET` | `/api/v2/nicolet/citas/cliente/{id}` | Historial de citas de una clienta |
| | `PATCH` | `/api/v2/nicolet/citas/{id}/estado` | Cambio de estado (`CONFIRMADA`, `EN_ATENCION`, `COMPLETADA`, `CANCELADA`) |
| **Servicios** | `GET` | `/api/v2/nicolet/servicios` | Catálogo de servicios activos |
| | `POST` | `/api/v2/nicolet/servicios` | Creación de nuevo servicio en el catálogo |
| | `PUT` | `/api/v2/nicolet/servicios/{id}` | Actualización de servicio |
| | `DELETE` | `/api/v2/nicolet/servicios/{id}` | Baja lógica de servicio |
| **Trabajadores** | `GET` | `/api/v2/nicolet/trabajadores` | Catálogo de profesionales activos con especialidad y bio |
| **Clientes** | `GET` | `/api/v2/nicolet/clientes` | Catálogo de clientes registrados con contacto |
| **Usuarios** | `GET`/`POST`/`PUT`/`DELETE` | `/api/v2/nicolet/usuarios` | CRUD de usuarios del sistema |

---

## Matriz de Avance y Cumplimiento de Requerimientos

### Resumen Ejecutivo de Estado

| Métrica | Valor | Detalle |
|---|---|---|
| **Total de Requerimientos** | **33** | 21 Funcionales (RF) + 12 No Funcionales (RNF) |
| **Requerimientos Implementados** | **14 / 33 (42.4%)** | Seguridad, Disponibilidad, Servicios, Citas, Portal Trabajador, Historial, Clientes |
| **En Proceso / Arquitectura Validada** | **6 / 33 (18.2%)** | Nivel 2: Pasarela Webpay Plus REST v1.2 y abonos |
| **Planificados en Roadmap** | **13 / 33 (39.4%)** | Inventario de insumos, fidelización automática y conciliación bancaria |
| **Pruebas Automatizadas Backend** | **94 Tests (100% Pass)** | Tests unitarios, de integración, repositorios, controladores y seguridad |

### Requerimientos Funcionales (RF01 - RF21)

| Código | Requerimiento Funcional | Nivel / Tarea | Estado Actual | Evidencia en Código e Interfaz |
|---|---|---|---|---|
| **RF01** | **Auto-Registro de Clientes** en flujo de agendamiento | Nivel 2 - T2.2 | **Implementado** | Formulario inline en `reserva-cliente.html`, `auth.js` (`register()`), sincronización en `ClienteService.java` |
| **RF02** | **Autenticación de Usuarios** (Login correo y contraseña) | Nivel 1 - T1.2 | **Implementado** | `AuthController.java`, `JwtUtils.java`, `login.html`, `auth.js` |
| **RF03** | **Control de Acceso por Roles** (ADMIN, CLIENTE, PROFESIONAL) | Nivel 1 - T1.3 | **Implementado** | `SecurityConfig.java`, restricción en `panel-trabajador.html` y filtros JWT |
| **RF04** | **Consulta de Bloques Horarios Disponibles** en vivo | Nivel 2 - T2.1 | **Implementado** | `DisponibilidadService.java`, `disponibilidad.html`, `reserva-cliente.html` |
| **RF05** | **Selección de Servicio y Profesional** antes de agendar | Nivel 2 - T2.1 | **Implementado** | Flujo guiado en `reserva-cliente.html` y selectores en `citas.html` |
| **RF06** | **Pago de Reserva / Abono Mínimo ($5.000 CLP)** vía Webpay | Nivel 2 - T2.3 | Arquitectura Lista | DDL `pago`, SDK `transbank-sdk-java:6.2.1` |
| **RF07** | **Ciclo Transaccional de Cita** (`PENDIENTE` a `CONFIRMADA`) | Nivel 2 - T2.5 | **Implementado** | Citas se crean en `PENDIENTE` en `reserva-cliente.html`; trabajador confirma en `panel-trabajador.html` |
| **RF08** | **Vista "Mi Agenda" por Profesional** | Nivel 3 - T3.4 | **Implementado** | `panel-trabajador.html` lista citas asignadas al especialista en sesión |
| **RF09** | **Gestión de Estados de Atención** (`EN_ATENCION`, `COMPLETADA`) | Nivel 3 - T3.4 | **Implementado** | Botones de transición de estado en `panel-trabajador.html` y `citas.html` |
| **RF10** | **Ficha Técnica Única de Cliente** | Nivel 4 - T4.1 | **Implementado** | Modal de seguimiento en `historial.html` con expediente de sesiones previas |
| **RF11** | **Registro de Observaciones Técnicas** en atenciones | Nivel 4 - T4.2 | **Implementado** | Campo `notasTrabajador` y visualización en `historial.html` |
| **RF12** | **Consulta de Historial Estético** por especialista | Nivel 4 - T4.2 | **Implementado** | Vista centralizada en `historial.html` agrupada por trabajador y clienta |
| **RF13** | **Catálogo de Insumos y Productos** (CRUD) | Nivel 3 - T3.1 | Roadmap | Entidad `producto_insumo`, unidad de medida y stock mínimo |
| **RF14** | **Deducción Automática de Stock** por Servicio Realizado | Nivel 3 - T3.2 | Roadmap | Tabla intermedia `servicio_insumo` |
| **RF15** | **Alertas de Stock Crítico** por debajo del umbral | Nivel 3 - T3.3 | Roadmap | Endpoint `GET /productos/alertas-stock` |
| **RF16** | **Control de Movimientos de Inventario** | Nivel 3 - T3.1 | Roadmap | Tabla `movimiento_stock` vinculada a citas |
| **RF17** | **Reporte de Clientas Atendidas por Profesional** | Nivel 5 - T5.1 | **Implementado** | KPIs de clientas únicas, tratamientos y ventas acumuladas en `historial.html` |
| **RF18** | **Dashboard Gerencial y Métricas** | Nivel 5 - T5.1 | **Implementado Base** | KPIs de citas, ingresos estimados y catálogo en `citas.html` y `historial.html` |
| **RF19** | **Expediente Histórico del Cliente** | Nivel 4 - T4.2 | **Implementado** | Modal interactivo en `historial.html` con todas las atenciones previas de la clienta |
| **RF20** | **Notificaciones Automáticas por Correo** | Nivel 4 - T4.3 | Roadmap | Integración con Spring Mail |
| **RF21** | **Motor de Fidelización de Clientes Inactivos** | Nivel 5 - T5.2 | Roadmap | Job programado `@Scheduled` para clientas inactivas |

### Requerimientos No Funcionales (RNF01 - RNF12)

| Código | Requerimiento No Funcional | Estado | Evidencia |
|---|---|---|---|
| **RNF01** | Seguridad REST con JWT y Whitelist | **Implementado** | `SecurityConfig.java`, `JwtAuthenticationFilter.java` |
| **RNF02** | Cifrado Seguro de Contraseñas (BCrypt) | **Implementado** | `BCryptPasswordEncoder`, actualización automática en `DataInitializer` |
| **RNF03** | Sanitización de Salidas JSON (Sin contraseñas) | **Implementado** | `UsuarioDTO.java`, ausencia de password en respuestas |
| **RNF04** | Resiliencia y TTL | Arquitectura Lista | DTOs de expiración diseñados |
| **RNF05** | Concurrencia y Antisolapamiento de Horarios | **Implementado** | Query indexada `existeSolapamiento()` y validación en `CitaService.java` |
| **RNF06** | Rendimiento y Tiempos de Respuesta (<200ms) | **Implementado** | Índices en MySQL y latencia medida en `disponibilidad.js` |
| **RNF07** | Cumplimiento PCI-DSS | Arquitectura Lista | Redirección 3D Secure sin almacenamiento de tarjetas |
| **RNF08** | Voucher Regulatorio Oficial | Arquitectura Lista | Especificación en pantalla de comprobante |
| **RNF09** | Auditoría de Operaciones | Roadmap | Trazabilidad en logs y base de datos |
| **RNF10** | Disponibilidad y Dockerización | **Implementado** | `docker-compose.yml`, `Dockerfile` |
| **RNF11** | Diseño Responsivo y CSS Centralizado | **Implementado** | `admin.css` unificado sin `<style>` inline, adaptado a desktop y móvil |
| **RNF12** | Respaldo Periódico de Datos | Roadmap | Script programado `mysqldump` |

---

## Requerimientos Faltantes para Finalizar la Plataforma Web

A continuación se detalla la lista de requerimientos funcionales, no funcionales e integraciones de interfaz que restan para dar por completado el 100% del alcance del sistema:

### 1. Pasarela de Pago y Transaccionalidad (Nivel 2)
* **RF06 - Integración Transbank Webpay Plus REST v1.2:**
  * Implementar flujo de pago con el SDK oficial (`transbank-sdk-java:6.2.1`).
  * Endpoints backend para iniciar transacción (`POST /api/v2/nicolet/pagos/iniciar`) y confirmar retorno con token WS (`POST /api/v2/nicolet/pagos/confirmar`).
  * Cobro de abono de garantía ($5.000 CLP) o pago completo del tratamiento seleccionado.
  * Persistencia en tabla `pago` (código de autorización, monto, últimos 4 dígitos, fecha/hora).
  * Transición automática de la cita a `CONFIRMADA` tras la aprobación bancaria.
* **RNF04 - Bloqueo Temporal de Horario con TTL (15 minutos):**
  * Mecanismo de bloqueo provisional del bloque horario mientras la clienta se encuentra en la pasarela de Webpay.
  * Si la transacción es rechazada o excede el TTL de 15 minutos, liberar automáticamente el bloque para otras clientas.
* **RNF07 - Cumplimiento Normativo PCI-DSS:**
  * Redirección segura 3D Secure / Webpay Plus sin capturar ni almacenar números de tarjeta bancaria en el backend del estudio.

### 2. Módulo de Gestión de Inventario e Insumos (Nivel 3)
* **RF13 - Catálogo y CRUD de Insumos/Productos:**
  * Tabla `producto_insumo` con campos: nombre, categoría, unidad de medida (ml, gr, unidades), stock actual y stock mínimo.
  * Interfaz administrativa `static/admin/inventario.html` para consultar, crear y actualizar existencias.
* **RF14 - Deducción Automática de Stock por Servicio Realizado:**
  * Tabla asociativa `servicio_insumo` que define la "receta técnica" de insumos consumidos por cada tratamiento.
  * Descuento automático de existencias en el inventario cuando una cita cambia al estado `COMPLETADA`.
* **RF15 - Alertas Visuales de Stock Crítico:**
  * Endpoint `GET /api/v2/nicolet/productos/alertas-stock` que liste insumos con existencia menor o igual al umbral mínimo.
  * Badges y avisos de color ámbar/rojo en el panel administrativo.
* **RF16 - Kardex y Trazabilidad de Movimientos:**
  * Tabla `movimiento_stock` para registrar entradas (compras), salidas automáticas (servicios prestados) y mermas manuales con usuario y fecha.

### 3. Ficha Técnica y Notificaciones (Nivel 4)
* **RF20 - Notificaciones Automáticas por Correo Electrónico:**
  * Integración con `spring-boot-starter-mail` (servidor SMTP / SendGrid).
  * Envío automático de correo con comprobante de reserva a la clienta tras agendar.
  * Notificación al especialista asignado sobre nuevas citas pendientes o confirmadas.
  * Tarea programada para envío de recordatorio 24 horas antes de la cita.
* **RNF08 - Generación y Descarga de Comprobante / Voucher PDF Oficial:**
  * Exportación en PDF descargable (OpenPDF / iText) con resumen de la cita, especialista, fecha, abono pagado y código QR de validación.

### 4. Inteligencia de Negocio y Seguridad Avanzada (Nivel 5)
* **RF21 - Motor Automatizado de Fidelización de Clientes Inactivos:**
  * Job programado con Spring (`@Scheduled`) para detectar clientas sin atenciones en los últimos 60 o 90 días.
  * Generación de alertas en el panel para campañas de re-contacto y promociones personalizadas.
* **RNF09 - Auditoría Integral de Operaciones:**
  * Tabla de bitácora `auditoria_operacion` para trazabilidad de cancelaciones de citas, modificaciones de precios y ajustes manuales de inventario (con usuario, IP y timestamp).
* **RNF12 - Respaldo Periódico Automatizado de Base de Datos:**
  * Script automatizado con `mysqldump` programado en el servidor para resguardo continuo de la base de datos MySQL.

### 5. Integraciones Frontend Pendientes en la Web
* **Enlace Público de Agendamiento:** Conectar el botón *"Reservar Cita"* de la página principal del salón (`static/index.html`) directamente con el asistente guiado de reserva de clientes (`static/admin/reserva-cliente.html` o widget embebido).
* **Botón de Descarga de Voucher:** Incorporar botón de descarga de PDF en el comprobante final de agendamiento.
* **Pantalla de Inventario:** Crear el archivo `static/admin/inventario.html` con su respectivo `inventario.js` enlazado a la barra de navegación administrativa.

---

## Pruebas Automatizadas

El proyecto cuenta con **94 pruebas automatizadas** que validan la capa de datos, lógica de negocio de agendamiento, cálculo de disponibilidad horaria, catálogo de servicios, perfiles de usuarios y seguridad con Spring Security y JWT:

```powershell
.\mvnw.cmd test
```

Resultado de ejecución verificado:
```text
[INFO] Results:
[INFO] Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
