# Estado de Avance de Requerimientos e Informe Técnico
## Nicolet Estudio - Sistema de Agendamiento de Citas

Este documento consolida el **estado de avance de los 33 requerimientos del sistema** (21 Requerimientos Funcionales y 12 Requerimientos No Funcionales), el resumen ejecutivo de la **Escalada de Prioridades** ([`escalada_prioridades.html`](escalada_prioridades.html)) y la especificación del **Informe Técnico de Integración Transbank Webpay Plus** ([`informe_integracion_webpay_nicolet.html`](informe_integracion_webpay_nicolet.html)).

---

##  Índice
1. [Resumen Ejecutivo de Estado](#-resumen-ejecutivo-de-estado)
2. [Matriz de Avance: Requerimientos Funcionales (RF01 - RF21)](#-matriz-de-avance-requerimientos-funcionales-rf01---rf21)
3. [Matriz de Avance: Requerimientos No Funcionales (RNF01 - RNF12)](#-matriz-de-avance-requerimientos-no-funcionales-rnf01---rnf12)
4. [Contenido del Informe de Integración Webpay Plus](#-contenido-del-informe-de-integración-webpay-plus)
5. [Estructura de la Escalada de Prioridades (5 Niveles / 20 Tareas)](#-estructura-de-la-escalada-de-prioridades-5-niveles--20-tareas)
6. [Plan de Acción para el Siguiente Sprint](#-plan-de-acción-para-el-siguiente-sprint)

---

##  Resumen Ejecutivo de Estado

| Métrica | Valor | Detalle |
|---|---|---|
| **Total de Requerimientos** | **33** | 21 Funcionales (RF) + 12 No Funcionales (RNF) |
| **Requerimientos Implementados** | **6 / 33 (18.2%)** | Nivel 1 Seguridad & Auth completo + Core base de Citas/Servicios |
| **En Proceso / Arquitectura Validada** | **8 / 33 (24.2%)** | Nivel 2: Pasarela Webpay Plus REST v1.2, RUT y Disponibilidad |
| **Planificados en Roadmap** | **19 / 33 (57.6%)** | Niveles 3, 4 y 5: Stock, Agenda Profesional, Fichas y Analítica |
| **Pruebas Automatizadas Backend** | **81 Tests (100% Pass)** | Tests unitarios, repositorios, servicios y seguridad Spring Boot |
| **Pasarela de Pagos Principal** | **Transbank Webpay Plus** | REST API v1.2 con SDK oficial Java 6.2.1 |

```
Progreso General del Proyecto:

[████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 24% (Implementado + Especificado)
```

---

##  Matriz de Avance: Requerimientos Funcionales (RF01 - RF21)

| Código | Requerimiento Funcional | Nivel / Tarea | Estado Actual | Evidencia en Código / Informe |
|---|---|---|---|---|
| **RF01** | **Auto-Registro de Clientes** con validación de RUT chileno | Nivel 2 • T2.2 | 🟡 Planificado | Algoritmo Módulo 11 `@ValidRut` y endpoint `POST /auth/registro-cliente` definidos |
| **RF02** | **Autenticación de Usuarios** (Login con correo y contraseña) | Nivel 1 • T1.2, T1.3 | 🟢 **Implementado** | `AuthController.java`, `JwtUtils.java`, `LoginRequestDTO.java`, `login.html` |
| **RF03** | **Control de Acceso por Roles** (ADMIN, CLIENTE, PROFESIONAL) | Nivel 1 • T1.3 | 🟢 **Implementado** | `SecurityConfig.java`, `TipoUsuario.java`, RBAC con filtros JWT |
| **RF04** | **Consulta de Bloques Horarios Disponibles** por profesional/fecha | Nivel 2 • T2.1 | 🟡 En Diseño | Endpoint `GET /citas/disponibilidad` calculado restando reservas y descansos |
| **RF05** | **Selección de Servicio y Profesional** antes de agendar | Nivel 2 • T2.1 | 🟢 **Parcial** | Catálogos activos en `ServicioService` y `TrabajadorService`, selector UI base |
| **RF06** | **Pago de Reserva / Abono Mínimo ($5.000 CLP)** vía Webpay | Nivel 2 • T2.3, T2.4 | 🟡 **Arquitectura Lista** | DDL `pago`, SDK `transbank-sdk-java:6.2.1`, `WebpayService.java` en informe técnico |
| **RF07** | **Ciclo Transaccional de Cita** (`PENDIENTE` a `CONFIRMADA`) | Nivel 2 • T2.5, T2.6 | 🟡 **Arquitectura Lista** | `CitaService.agendar()` retiene cupo; pasa a `CONFIRMADA` solo con commit exitoso |
| **RF08** | **Vista "Mi Agenda" por Profesional** (Calendario diario/semanal) | Nivel 3 • T3.4 | ⚪ Roadmap | Filtrado de citas por trabajador autenticado |
| **RF09** | **Gestión de Estados de Atención** (`EN_ATENCION`, `COMPLETADA`, etc.) | Nivel 3 • T3.4 | 🟢 **Parcial** | `EstadoCita.java` soporta estados; falta modal frontend para cambio operativo |
| **RF10** | **Ficha Técnica Única de Cliente** (Alergias, tipo piel/uñas) | Nivel 4 • T4.1 | ⚪ Roadmap | Modelo `ficha_cliente` con código único (ej: `FCH-2026-0042`) |
| **RF11** | **Registro Obligatorio de Observaciones Técnicas** al finalizar | Nivel 4 • T4.2 | ⚪ Roadmap | Modal obligatorio de cierre con notas de esmaltes, geles o tintes usados |
| **RF12** | **Consulta de Historial Estético** en atenciones previas | Nivel 4 • T4.2 | ⚪ Roadmap | Consulta cronológica de atenciones previas del cliente |
| **RF13** | **Catálogo de Insumos y Productos** (CRUD) | Nivel 3 • T3.1 | ⚪ Roadmap | Entidad `producto_insumo`, unidad de medida y stock mínimo |
| **RF14** | **Deducción Automática de Stock** por Servicio Realizado | Nivel 3 • T3.2 | ⚪ Roadmap | Tabla intermedia `servicio_insumo`; hook al marcar cita `COMPLETADA` |
| **RF15** | **Alertas de Stock Crítico** por debajo del umbral mínimo | Nivel 3 • T3.3 | ⚪ Roadmap | Endpoint `GET /productos/alertas-stock` y campana visual en interfaz |
| **RF16** | **Control de Movimientos de Inventario** (Entradas, salidas, mermas) | Nivel 3 • T3.1 | ⚪ Roadmap | Tabla `movimiento_stock` vinculada a citas o ingresos manuales |
| **RF17** | **Reporte de Clientes Atendidos por Profesional** | Nivel 5 • T5.1 | ⚪ Roadmap | Endpoint estadístico agrupado por trabajador y rango de fechas |
| **RF18** | **Dashboard Gerencial y Analítica de Ingresos** | Nivel 5 • T5.1 | ⚪ Roadmap | Panel Chart.js con suma de montos desde `pago.estado == 'APROBADO'` |
| **RF19** | **Expediente Histórico 360° del Cliente** | Nivel 4 • T4.2 | ⚪ Roadmap | Vista centralizada de fichas, citas, pagos y observaciones técnicas |
| **RF20** | **Notificaciones Automáticas por Correo Electrónico** | Nivel 4 • T4.3 | ⚪ Roadmap | Spring Mail con plantillas HTML y comprobante Webpay integrado |
| **RF21** | **Motor de Fidelización de Clientes Inactivos** (>30 días) | Nivel 5 • T5.2 | ⚪ Roadmap | Job `@Scheduled` que detecta clientas sin visitas para contacto preventivo |

---

##  Matriz de Avance: Requerimientos No Funcionales (RNF01 - RNF12)

| Código | Requerimiento No Funcional | Nivel / Tarea | Estado Actual | Evidencia en Código / Informe |
|---|---|---|---|---|
| **RNF01** | **Seguridad en la API REST (JWT & Whitelist)** | Nivel 1 • T1.2 / Nivel 2 • T2.4 | 🟢 **Implementado** | Filtro `JwtAuthenticationFilter`, rutas públicas parametrizadas en `SecurityConfig` |
| **RNF02** | **Almacenamiento Seguro de Contraseñas (BCrypt)** | Nivel 1 • T1.1 | 🟢 **Implementado** | Bean `BCryptPasswordEncoder`, contraseñas semilla actualizadas en `DataInitializer` |
| **RNF03** | **Sanitización de Salidas JSON (Sin passwords)** | Nivel 1 • T1.1 | 🟢 **Implementado** | `UsuarioDTO.java` y endpoints de consulta sin campos de contraseña |
| **RNF04** | **Resiliencia & Expiración TTL (15 min por abandono)** | Nivel 2 • T2.5 | 🟡 **Arquitectura Lista** | Cron `@Scheduled` `ExpiracionCitasJob.java` diseñado en informe técnico |
| **RNF05** | **Concurrencia & Antisolapamiento de Agenda** | Nivel 2 • T2.1 | 🟢 **Implementado Base** | Validación de traslape horario de citas activas en `CitaService.java` |
| **RNF06** | **Tiempos de Respuesta Óptimos (<200ms en API)** | Nivel 2 • T2.1 | 🟢 **Implementado Base** | Índices JPA en repositorios y consultas con clave foránea indexada |
| **RNF07** | **Cumplimiento PCI-DSS en Pasarela de Pago** | Nivel 2 • T2.3, T2.7 | 🟡 **Arquitectura Lista** | El servidor jamás almacena ni procesa números PAN completos ni CVV |
| **RNF08** | **Voucher Regulatorio Oficial Transbank** | Nivel 2 • T2.6 | 🟡 **Arquitectura Lista** | Especificación visual del comprobante con los 8 campos obligatorios TBK |
| **RNF09** | **Auditoría Transaccional de Operaciones Críticas** | Nivel 5 • T5.3 | ⚪ Roadmap | Tabla `auditoria_operacion` e interceptor de cambios en citas y stock |
| **RNF10** | **Disponibilidad y Tolerancia a Fallos** | Transversal | 🟢 **Implementado Base** | Docker Compose para aislamiento de base de datos MySQL 8 y microservicio |
| **RNF11** | **Diseño Responsivo y Usabilidad (UX Móvil)** | Frontend | 🟢 **Implementado Base** | Formularios CSS adaptados a dispositivos móviles en `front/css/` |
| **RNF12** | **Respaldo Periódico de Datos (Backups MySQL)** | Nivel 5 • T5.3 | ⚪ Roadmap | Script de respaldo automatizado `mysqldump` con rotación |

---

##  Integración Webpay Plus



### 1. Diagnóstico AS-IS y Viabilidad Técnica
* **Problema actual:** Las citas se crean en estado `CONFIRMADA` sin exigir pago previo, exponiendo al negocio a inasistencias ("no-shows") y falta de control de caja.
* **Viabilidad:** El stack actual (**Java 17 + Spring Boot 3.2.5 + MySQL 8**) es 100% compatible con los estándares REST de Transbank.
* **Impacto proyectado:** Reducción estimada del **85% de inasistencias** al exigir abono mínimo obligatorio de **$5.000 CLP**.

### 2. Especificación Técnica de Webpay Plus REST v1.2
* **Ambiente de Integración (Testing):**
  * Base URL: `https://webpay3gint.transbank.cl`
  * Código de Comercio: `597055555532`
  * Api Key Secret: `579B532A7440BB0C9079DED94D31EA1615BACEB56610332264630D42D0A36B1C`
* **Endpoints Nucleares de Transbank:**
  1. `POST /rswebpaytransaction/api/webpay/v1.2/transactions` (Crear transacción: retorna `url` y `token_ws`).
  2. `PUT /rswebpaytransaction/api/webpay/v1.2/transactions/{token}` (Commit bancario obligatorio).
  3. `GET /rswebpaytransaction/api/webpay/v1.2/transactions/{token}` (Consulta de estado).
  4. `POST /rswebpaytransaction/api/webpay/v1.2/transactions/{token}/refunds` (Reversas y anulaciones).

### 3. Decisión de Arquitectura: SDK Oficial Java
* Se seleccionó el SDK oficial **`com.github.transbankdevelopers:transbank-sdk-java:6.2.1`** sobre FeignClient manual debido a:
  * Inicialización simple: `WebpayPlus.Transaction.buildForIntegration()`.
  * Homologación garantizada y soporte de DTOs oficiales de Transbank.
  * Cero riesgo de desactualización frente a cambios normativos de headers.

### 4. Modelo de Datos de Pago (Script MySQL)
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
    tipo_pago VARCHAR(10),                          -- VD (Débito), VN (Crédito), VC (Cuotas)
    codigo_respuesta INT,                           -- 0 = Aprobada
    fecha_transaccion DATETIME,
    vci VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_cita FOREIGN KEY (cita_id) REFERENCES cita(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5. Flujo Transaccional en 8 Pasos
```
1. Cliente selecciona hora y servicio en frontend.
2. Backend persiste Cita en estado temporal PENDIENTE (cupo reservado por 15 min).
3. Backend ejecuta tx.create() hacia Transbank con buyOrder y monto real del servicio.
4. Frontend recibe { url, token } y ejecuta auto-submit POST al portal de Transbank.
5. Cliente paga de forma segura en Transbank (servidores seguros TBK / 3D Secure).
6. Redirección bancaria hacia nuestro callback público: /api/v2/nicolet/pagos/webpay/retorno.
7. Backend invoca tx.commit(token_ws); si responseCode == 0 -> Cita CONFIRMADA y Pago APROBADO.
8. Despliegue de Voucher normativo con código de autorización y opción de descarga PDF.
```

### 6. Manejo de Aborto Voluntario y Timeout (Cron Job TTL)
* **Aborto Voluntario:** Si el cliente hace clic en *"Anular compra"* en Webpay, Transbank retorna los parámetros `TBK_TOKEN` y `TBK_ORDEN_COMPRA`. El backend detecta estos parámetros, marca el pago como `ABORTADO`, cancela la cita y libera el cupo inmediatamente.
* **Expiración Automática (`ExpiracionCitasJob`):** Cron Job con `@Scheduled(cron = "0 */5 * * * *")` que cancela citas que lleven más de 15 minutos en estado `PENDIENTE` sin haberse pagado.

### 7. Comprobante de Pago Electrónico (Voucher Normativo)
El informe define los 8 campos obligatorios por normativa Transbank para aprobar la certificación:
* Nombre del comercio: **Nicolet Estudio**
* Código de Reserva (ej: `RES-2026-F9A1B2`)
* Orden de Compra (`OC-...`)
* Código de Autorización (ej: `1213`)
* Tipo de Transacción (`Venta Débito - VD` / `Venta Crédito - VN/VC`)
* Tarjeta enmascarada (`**** **** **** 6623`)
* Fecha y hora de transacción
* Monto Total Pagado en CLP

### 8. Matriz de Pruebas Oficial de Certificación (5 Tarjetas Test)
1. **Débito Redcompra:** Tarjeta `6623 0000 0000 0001` &rarr; Código de respuesta `0`, tipo `VD`.
2. **Crédito Sin Cuotas:** Tarjeta `6623 0000 0000 0002` &rarr; Código de respuesta `0`, tipo `VN`.
3. **Crédito En Cuotas:** Tarjeta `6623 0000 0000 0003` &rarr; Código de respuesta `0`, tipo `VC`.
4. **Rechazo por Fondos:** Tarjeta `6623 0000 0000 0004` &rarr; Código `!= 0`, reversa automática y cancelación.
5. **Cancelación por Usuario:** Cualquier tarjeta &rarr; Retorno con `TBK_TOKEN`, liberación inmediata de cupo.

---

## Estructura de la Escalada de Prioridades (5 Niveles / 20 Tareas)


```
[Nivel 1] SEGURIDAD & AUTH (3 Tareas) • 100% COMPLETADO 🟢
├── T1.1: Cifrado BCrypt y Sanitización de DTOs
├── T1.2: Tokens JWT y Spring Security (incluye whitelist para retorno Webpay)
└── T1.3: Pantalla de Login y Control de Sesión en Frontend

[Nivel 2] CORE TRANSACCIONAL & WEBPAY PLUS (7 Tareas) • SIGUIENTE PRIORIDAD 🟠
├── T2.1: Selector de Horarios Libres y Disponibilidad (RF04, RF05)
├── T2.2: Auto-Registro de Clientes y Validación de RUT Módulo 11 (RF01)
├── T2.3: Webpay Fase 1: SDK Oficial, Esquema DDL y Entidad de Pago (RF06, RNF07)
├── T2.4: Webpay Fase 2: Backend WebpayService, Endpoints REST y Aborto (RF06, RNF01)
├── T2.5: Webpay Fase 3: Cita PENDIENTE y Cron TTL de Expiración (RF07, RNF04)
├── T2.6: Webpay Fase 4: Frontend (Auto-Submit POST) y Voucher Oficial (RF06, RF07)
└── T2.7: Webpay Fase 5: Matriz de 5 Tarjetas Test y Homologación TBK (RNF07, RNF08)

[Nivel 3] AGENDA & CONTROL DE INSUMOS (4 Tareas) • SPRINT 3 🔵
├── T3.1: CRUD de Insumos y Registro de Movimientos de Stock (RF13, RF16)
├── T3.2: Asociación Servicio-Insumos y Deducción Automática (RF14)
├── T3.3: Alertas de Stock Crítico en Dashboard (RF15)
└── T3.4: Vista "Mi Agenda" por Profesional y Gestión de Estados (RF08, RF09)

[Nivel 4] FICHA TÉCNICA & NOTIFICACIONES (3 Tareas) • SPRINT 4 🟢
├── T4.1: Ficha Técnica Única de Cliente (Expediente Estético) (RF10)
├── T4.2: Observaciones Técnicas Obligatorias e Historial 360° (RF11, RF12, RF19)
└── T4.3: Notificaciones por Correo Electrónico con Voucher Webpay (RF20)

[Nivel 5] GESTIÓN, AUDITORÍA & CALIDAD (3 Tareas) • SPRINT 5 ⚪
├── T5.1: Dashboard Gerencial, Ingresos Webpay y Conciliación (RF17, RF18)
├── T5.2: Motor de Fidelización de Clientes Inactivos (@Scheduled) (RF21)
└── T5.3: Auditoría Transaccional y Backups Automáticos MySQL (RNF09, RNF12)
```

---

##  Plan de Acción para el Siguiente Sprint

El foco inmediato de desarrollo es el **Nivel 2 (Core Transaccional & Pasarela Webpay Plus)**:

1. **Paso 1 (T2.3):** Incorporar `transbank-sdk-java:6.2.1` a `pom.xml` y ejecutar script SQL para la tabla `pago`.
2. **Paso 2 (T2.4):** Crear `WebpayService.java` y `PagoController.java` conectando con el ambiente de integración de Transbank.
3. **Paso 3 (T2.5):** Adaptar `CitaService.java` para persistir citas en estado `PENDIENTE` y activar el job `ExpiracionCitasJob.java`.
4. **Paso 4 (T2.6):** Actualizar `front/js/citas.js` con el formulario auto-submit y crear la vista de voucher regulatorio.
5. **Paso 5 (T2.7):** Ejecutar la batería de pruebas con las 5 tarjetas de test oficiales y consolidar evidencias de homologación.

---

*Documento técnico de seguimiento para Nicolet Estudio • Basado en el Informe de Requerimientos y el Informe de Integración Webpay Plus • Octubre 2026*
