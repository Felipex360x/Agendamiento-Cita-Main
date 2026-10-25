# Nicolet Estudio

Sistema web para gestionar el agendamiento de citas de Nicolet Estudio. El proyecto separa la interfaz React del backend Spring Boot y usa MySQL como base de datos.

## Contenido

- [Tecnologías](#tecnologías)
- [Estructura](#estructura)
- [Inicio rápido en Windows](#inicio-rápido-en-windows)
- [Desarrollo local](#desarrollo-local)
- [Compartir una demo por Internet](#compartir-una-demo-por-internet)
- [API](#api)
- [Verificaciones](#verificaciones)

## Tecnologías

- **Interfaz:** React 18 y Vite.
- **Backend:** Java 17, Spring Boot 3, Spring Security y JWT.
- **Base de datos:** MySQL 8 o MariaDB.
- **Acceso temporal desde Internet:** Cloudflare Quick Tunnel.

Las vistas anteriores se conservan en `frontend/public/legacy` y se cargan desde React mediante un adaptador temporal. Spring Boot funciona como API. Durante el desarrollo local, Vite sirve la interfaz y reenvía las solicitudes de `/api` y `/doc` al backend.

## Estructura

```text
frontend/                  Aplicación React y recursos heredados
src/main/java/             API y lógica de negocio Spring Boot
src/main/resources/        Configuración del backend

db/                        Inicialización y documentación de la base de datos
docker-compose.yml          MySQL y phpMyAdmin
iniciar-demo.cmd            Lanzador de Spring, React y Cloudflare Tunnel
iniciar-demo.ps1            Lógica del lanzador
```

## Inicio rápido en Windows

Desde la carpeta del proyecto, ejecuta un único comando para iniciar la base de datos si hace falta, Spring Boot, React y el túnel público:

```powershell
.\iniciar-demo.cmd
```

Necesitas Java 17, Node.js y npm. Para iniciar MySQL automáticamente, también necesitas Docker Desktop; si ya tienes MySQL activo en el puerto `3306`, el lanzador lo reutiliza.

Cuando cloudflared muestre una dirección `https://....trycloudflare.com`, compártela con quien deba revisar la demo. Mantén el PC y las ventanas abiertas. Presiona `Ctrl+C` en la ventana principal para detener Spring Boot, React y el túnel.

La base de datos puede permanecer activa después de cerrar la aplicación.

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
npm install
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

`iniciar-demo.cmd` inicia un Quick Tunnel temporal hacia React. Comparte la dirección que imprime cloudflared; no uses `localhost` ni `127.0.0.1` en el dispositivo remoto. La dirección cambia cuando se crea un túnel nuevo y deja de funcionar cuando el túnel se cierra. Cualquier persona que tenga el enlace podrá abrir la demo.

Para usar un dominio propio, configura un túnel administrado desde tu cuenta de Cloudflare y define las variables antes de iniciar el lanzador:

```powershell
$env:TUNNEL_HOSTNAME = 'agenda.tudominio.cl'
$env:CLOUDFLARE_TUNNEL_TOKEN = '<token-del-tunnel>'
.\iniciar-demo.cmd
```

El hostname debe estar configurado en Cloudflare para ese túnel. No guardes el token en el repositorio.

## API

Con Spring Boot iniciado, la documentación interactiva está en:

- Swagger UI: `http://localhost:8080/doc/swagger-ui.html`
- API base: `/api/v2/nicolet`

Los recursos principales incluyen autenticación, citas, disponibilidad, servicios, trabajadores, clientes y usuarios. Las rutas protegidas requieren autenticación; Swagger muestra los métodos y modelos disponibles.

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
