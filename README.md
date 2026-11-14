# Nicolett Studio Fantasy: Belleza a tu ritmo y sin barreras

Sistema web para gestionar el agendamiento de citas de Nicolett Studio Fantasy: Belleza a tu ritmo y sin barreras. El proyecto separa la interfaz React del backend Spring Boot y usa MySQL como base de datos.

## Contenido

- [Tecnologías](#tecnologías)
- [Estructura](#estructura)
- [Inicio rápido en Windows](#inicio-rápido-en-windows)
- [Inicio rápido en Linux](#inicio-rápido-en-linux)
- [Desarrollo local](#desarrollo-local)
- [Compartir una demo por Internet](#compartir-una-demo-por-internet)
- [API](#api)
- [Verificaciones](#verificaciones)

## Tecnologías

- **Interfaz:** React 18 y Vite.
- **Backend:** Java 17, Spring Boot 3.2.5 (rama sin soporte abierto: ver «Mantenimiento y versiones»), Spring Security y JWT.
- **Base de datos:** MySQL 8 o MariaDB.
- **Acceso temporal desde Internet:** Cloudflare Quick Tunnel.

La interfaz es React: todas las páginas se renderizan con componentes de `frontend/src`. Spring Boot funciona como API. Durante el desarrollo local, Vite sirve la interfaz y reenvía `/api`, `/doc` y solo la sonda `/actuator/health` al backend.

## Estructura

```text
frontend/                       Aplicación React (Vite)
  index.html                    HTML de entrada
  src/
    pages/                      Páginas (una por ruta)
    layouts/                    Estructuras comunes: sitio público (SiteShell) y panel (AdminShell)
    components/                 Componentes reutilizables
    api/                        Cliente HTTP de la API
    auth/                       Contexto de sesión
    utils/                      Fechas, estadísticas y utilidades de texto
    styles/                     Hojas de estilo
    assets/                     Imágenes e ícono
    __tests__/                  Pruebas (Vitest)
    routes.js                   Registro único de rutas
src/main/java/                  API Spring Boot (controller, service, repository, model, dto, security…)
src/main/resources/             Configuración y plantillas de correo
src/test/                       Pruebas del backend (JUnit)
db/                             Inicialización y documentación de la base de datos
scripts/start.cmd, start.ps1    Inicia solo el backend en Windows
.github/workflows/ci.yml        Integración continua (backend y frontend)
docker-compose.yml              MySQL y phpMyAdmin
iniciar-demo.cmd                Lanzador de Spring, React y Cloudflare Tunnel (Windows)
iniciar-demo.ps1                Lógica del lanzador de Windows
iniciar.sh                      Lanzador para Linux (base de datos si hace falta, Spring y React)
```

## Inicio rápido en Windows

Desde la carpeta del proyecto, ejecuta un único comando para iniciar la base de datos si hace falta, Spring Boot, React y el túnel público:

```powershell
.\iniciar-demo.cmd
```

Necesitas Java 17 y Node.js (`^22.22.2`, `^24.15.0` o `>=26`; se usa la versión de `frontend/.nvmrc`, hoy 26) con npm. Para iniciar MySQL automáticamente, también necesitas Docker Desktop; si ya tienes MySQL activo en el puerto `3306`, el lanzador lo reutiliza.

Cuando cloudflared muestre una dirección `https://....trycloudflare.com`, compártela con quien deba revisar la demo. Mantén el PC y las ventanas abiertas. Presiona `Ctrl+C` en la ventana principal para detener Spring Boot, React y el túnel.

La base de datos puede permanecer activa después de cerrar la aplicación.

## Inicio rápido en Linux

```bash
./iniciar.sh
```

Inicia la base de datos con Docker solo si no hay una disponible en `127.0.0.1:3306` (si ya hay MySQL, la reutiliza), luego Spring Boot y React, y espera a que respondan de verdad (Spring, Vite y la salud de Spring a través de Vite, cada una con tiempo máximo). Los registros quedan en `logs/`. Ctrl+C detiene solo Spring Boot y React; la base de datos queda activa.

- Necesita Java 17+, Node.js (ver `frontend/.nvmrc`), `curl` y `ss`; Docker solo si debe iniciar MySQL. Instala las dependencias del frontend (`npm ci`) si faltan.
- Si el puerto 8080 o 5173 está ocupado, informa quién lo usa y termina: **no detiene procesos ajenos**.
- Opciones: `--sin-docker` (no intenta iniciar MySQL) y `--ayuda`. Variables: `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD` (por defecto las de `docker-compose.yml`) y `JWT_SECRET` (si falta, se genera uno por ejecución).
- No incluye el túnel de Cloudflare: para compartir la demo, en otra terminal ejecuta `cloudflared tunnel --url http://127.0.0.1:5173`.

## Desarrollo local

Si prefieres ejecutar cada parte por separado, inicia primero MySQL y luego Spring Boot:

```powershell
docker compose up -d mysql_usuario
$env:DB_USERNAME = 'desarrollador'
$env:DB_PASSWORD = 'password_seguro_123'
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd spring-boot:run
```

En otra terminal, inicia React:

```powershell
cd frontend
npm ci
npm run dev
```

Abre `http://localhost:5173`. El backend queda disponible en `http://localhost:8080`. La configuración de conexión a la base de datos está en `src/main/resources/application.properties` y permite definir host, puerto, usuario y contraseña mediante variables de entorno.

Para generar los archivos de producción de React:

```powershell
cd frontend
npm run build
```

El resultado queda en `frontend/dist`.

## Compartir una demo por Internet

`iniciar-demo.cmd` espera la salud real de Spring y de Vite (con tiempo máximo) antes de abrir el túnel, escribe los registros en `logs/` y se detiene si algún proceso cae. Para iniciar solo el backend en Windows usa `scripts\start.cmd`: no detiene procesos que usen el puerto 8080, informa quién lo ocupa y termina.

`iniciar-demo.cmd` inicia un Quick Tunnel temporal hacia React. Comparte la dirección que imprime cloudflared; no uses `localhost` ni `127.0.0.1` en el dispositivo remoto. La dirección cambia cuando se crea un túnel nuevo y deja de funcionar cuando el túnel se cierra. Cualquier persona que tenga el enlace podrá abrir la demo.

Para usar un dominio propio, configura un túnel administrado desde tu cuenta de Cloudflare y define las variables antes de iniciar el lanzador:

```powershell
$env:TUNNEL_HOSTNAME = 'agenda.tudominio.cl'
$env:CLOUDFLARE_TUNNEL_TOKEN = '<token-del-tunnel>'
.\iniciar-demo.cmd
```

El hostname debe estar configurado en Cloudflare para ese túnel. No guardes el token en el repositorio.

## Despliegue en AWS EC2

El script `scripts/ec2-bootstrap.sh` instala Docker, clona el repositorio en `~/app`, genera `~/app/.env` con secretos aleatorios (la clave del administrador queda en `APP_ADMIN_PASSWORD`), instala el servicio `systemd` `nicolet` y levanta MySQL, backend y frontend. Es idempotente y no pisa un `.env` existente.

- **Instancia nueva:** pega esto en *Advanced details > User data* al lanzarla (corre una sola vez, como root):

  ```bash
  #!/bin/bash
  curl -fsSL https://raw.githubusercontent.com/Felipex360x/Agendamiento-Cita-Main/main/scripts/ec2-bootstrap.sh | bash
  ```

- **Instancia ya creada:** `sudo bash ~/app/scripts/ec2-bootstrap.sh`.
- **Reinicios:** el servicio `nicolet` y `restart: always` levantan todo solo al iniciar la instancia.
- **AWS Academy:** al iniciar el lab y encender la instancia todo sube solo (el *User data* corre una vez; el servicio `nicolet` corre en cada arranque). Asocia una IP elástica; si la IP cambia, el servicio actualiza `APP_FRONTEND_URL` solo, pero el secret `EC2_HOST` del deploy hay que cambiarlo a mano.
- **Requisitos:** puertos 80 y 22 abiertos en el grupo de seguridad e IP elástica. Los despliegues siguientes los hace el job `deploy` de GitHub Actions (`git pull` + `docker compose up -d --build`).
- Revisa el progreso con `sudo tail -f /var/log/cloud-init-output.log` y el estado con `sudo docker ps`. La plantilla de variables es `.env.prod.example`.

## API

Con Spring Boot iniciado, la documentación interactiva está en:

- Swagger UI: `http://localhost:8080/doc/swagger-ui.html`
- API base: `/api/v2/nicolet`

Los recursos principales incluyen autenticación, citas, disponibilidad, servicios, trabajadores, clientes y usuarios. Las rutas protegidas requieren autenticación; Swagger muestra los métodos y modelos disponibles.

## Contratos de la API

**Zona horaria y fechas.** Las fechas y horas de la API son *hora de pared* del negocio, en la zona `app.timezone` (por defecto `America/Santiago`; `VITE_BUSINESS_TIMEZONE` en el frontend debe coincidir). «Hoy» y «hora pasada» se calculan con esa zona en servidor y navegador. Las columnas siguen siendo `DATETIME`.

**Errores.** Toda respuesta de error incluye `status`, `codigo` (`VALIDACION`, `CONFLICTO`, `NO_ENCONTRADO`, `SOLICITUD_INVALIDA`…), `mensaje` y `error` (igual a `mensaje`, por compatibilidad). Las validaciones añaden `errores: {campo: mensaje}` y repiten cada campo al nivel superior (comportamiento anterior; preferir `errores`). Conflictos de integridad → 409.

**Sesión.** El JWT identifica al usuario por su ID permanente (`ver=2`); los permisos se leen siempre de la base de datos. Los tokens del formato anterior (subject = correo) se rechazan con 401 y el frontend cierra la sesión. Cerrar sesión en el navegador no revoca copias del token (no hay lista de revocación). Una cuenta *sin rol* no tiene permisos: puede existir pero no iniciar sesión (403) ni usar la API protegida.

**Citas.** Estados: `PENDIENTE → CONFIRMADA | CANCELADA`; `CONFIRMADA → EN_ATENCION | COMPLETADA | CANCELADA | NO_ASISTIO`; `EN_ATENCION → COMPLETADA | CANCELADA`. `COMPLETADA`, `CANCELADA` y `NO_ASISTIO` son finales (no se reactivan; se crea otra reserva). Cancelar exige motivo (máx. 255). Las reservas de un profesional se serializan con un bloqueo de su fila y solo se aceptan si el profesional ofrece el servicio. Cambiar la jornada no cancela citas: `PUT /horarios/trabajador/{id}` informa en la cabecera `X-Citas-Fuera-De-Jornada` cuántas quedan fuera.

**Límite de intentos de login.** El intento se reserva de forma atómica antes de verificar la contraseña. La IP del cliente se toma de `X-Forwarded-For` solo si la conexión directa viene de un proxy de confianza (`app.security.trusted-proxies` / variable `TRUSTED_PROXIES`, por defecto loopback, que cubre Cloudflare → Vite → Spring). Con otro despliegue, configura esa lista; los contadores viven en memoria de cada instancia.

## Mantenimiento y versiones

- Spring Boot 3.2.5 ya no tiene soporte abierto. Las ramas con soporte (4.0 hasta 2026‑12‑31 y 4.1 hasta 2027‑07‑31, según páginas de fin de soporte; confirmar en spring.io) implican Spring Framework 7, Hibernate 7 y Jackson 3, y obligan a migrar las anotaciones `@MockBean`/`@SpyBean` de las pruebas. Se planifica como un cambio aparte, coordinado con la decisión de base de datos.
- Node: `engines` y `.nvmrc` fijan el runtime; CI usa el mismo `.nvmrc`. En Node ≥ 25 el `localStorage` global es `undefined` sin `--localstorage-file`; `frontend/src/test/setup.js` lo suple en las pruebas.
- Tras editar código Java, ejecuta `mvnw clean test` (una compilación incremental obsoleta puede impedir que el contexto de Spring cargue).

## Verificaciones

Backend:

```powershell
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm test
npm run build
```
