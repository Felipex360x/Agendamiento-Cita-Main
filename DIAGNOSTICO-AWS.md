# Diagnóstico del despliegue en AWS EC2 (13-nov-2026)

Servidor probado desde fuera: `http://52.87.43.82` (nginx → backend Spring → MySQL, con `docker-compose.prod.yml`).
No se tuvo acceso SSH a la instancia, así que todo lo de abajo sale de pruebas HTTP y de leer el código.

## Resumen
El backend y la base de datos **sí están levantados y conectados**. Lo que falla es la **configuración inicial**, no la infraestructura.

| Prueba | Resultado |
|---|---|
| `GET /` | 200, el frontend carga |
| `GET /actuator/health` | 200 `{"status":"UP"}` (backend arriba y con conexión a MySQL) |
| `GET /api/v2/nicolet/servicios` | 200, devuelve los 3 servicios de `db/init.sql` (la DB responde) |
| `GET /api/v2/nicolet/trabajadores` | 200 pero `[]` |
| `POST /auth/login` con credenciales falsas | 401 normal |
| Rutas protegidas sin token | 401 normal |

## Problemas encontrados (de más a menos grave)

### 1. No hay profesionales ni administrador utilizable → la app "no funciona"
- `/trabajadores` devuelve `[]`: sin profesionales no se puede agendar ninguna cita ni ver disponibilidad.
- `db/init.sql` no crea usuarios a propósito. El admin lo crea `AdminInicialInitializer` al arrancar **solo si** `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` están definidos; si no, usa `admin@nicolet.cl` con una **contraseña aleatoria que se imprime una sola vez en el log** del contenedor.
- `docker-compose.prod.yml` **no define** `APP_ADMIN_EMAIL` ni `APP_ADMIN_PASSWORD`, así que la contraseña quedó en el log y probablemente no se anotó.
- **Arreglo:** agregar ambas variables al servicio `backend` (idealmente leídas de un `.env` en el EC2, no escritas en el compose). Si ya existe un admin con contraseña perdida, hay que recuperarla del log (`sudo docker logs nicolet_backend | grep -i administrador`) o resetearla en la DB. Luego entrar al panel y crear los profesionales. `APP_DEMO_DATA_ENABLED=true` cargaría datos de ejemplo, pero no se recomienda en un servidor expuesto.

### 2. Caracteres rotos en la base (mojibake)
- `/servicios` devuelve `CutÃ­culas`, `exfoliaciÃ³n`, `duraciÃ³n`. `init.sql` está en UTF-8 pero el cliente `mysql` del entrypoint lo lee como latin1.
- **Arreglo:** poner `SET NAMES utf8mb4;` como primera línea de `db/init.sql` y agregar al servicio mysql `command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci`.
- **Ojo:** `init.sql` solo corre con el volumen vacío y el `ON DUPLICATE KEY UPDATE` no corrige `nombre`/`descripcion`. Hay que borrar el volumen (`sudo docker compose -f docker-compose.prod.yml down -v`, **destruye todos los datos de MySQL**) o hacer un `UPDATE` manual de las 3 filas de `servicio`.

### 3. `TRUSTED_PROXIES` no coincide con la topología
- El compose pone `TRUSTED_PROXIES: 127.0.0.1,::1`, pero el backend recibe las peticiones desde el contenedor nginx (IP de la red Docker, tipo `172.x.x.x`), que no está en esa lista.
- Consecuencia: `ClientIpResolver` ignora `X-Forwarded-For` y todos los usuarios comparten la IP del nginx. Afecta al límite de intentos de login y al límite de 10 registros por hora (`MAX_REGISTERS_PER_HOUR`): pueden bloquearse entre ellos. No lo pude confirmar del todo desde fuera.
- **Arreglo:** poner la subred de la red Docker en `TRUSTED_PROXIES` (p. ej. `172.16.0.0/12`) o fijar una subred en `nicolet_net`.

### 4. Correo no funciona
- `MAIL_PASSWORD: ""` en el compose. Sin contraseña de aplicación de Gmail no se envía ningún correo (confirmaciones, recuperación). No tumba el backend (`management.health.mail.enabled=false`) pero esas funciones fallarán.

### 5. Seguridad / mantenimiento
- `docker-compose.prod.yml` está en git con la contraseña root de MySQL, la de `desarrollador` y el `JWT_SECRET` en texto plano. Hay que **rotarlos** y moverlos a un `.env` del servidor (`${VAR}` en el compose; el `.env` no se sube).
- `APP_FRONTEND_URL` y el workflow usan la IP `52.87.43.82` (en el workflow viene de `secrets.EC2_HOST`). Si no es una IP elástica, cambia al reiniciar la instancia y rompe enlaces de correo y el CD.
- El `Dockerfile` del backend ejecuta `mvn clean package` **con pruebas** dentro del `docker compose up --build`, y el front hace `npm ci` + build. En una instancia pequeña (t2/t3.micro, 1 GB de RAM) esto puede quedarse sin memoria o tardar mucho en cada deploy. Conviene agregar swap o usar una instancia con más RAM. No lo pude verificar sin acceso al servidor.
- `depends_on` no espera a que MySQL esté listo (no hay `healthcheck`); funciona gracias a `restart: always`, pero el backend puede reiniciarse un par de veces en el primer arranque.

## Qué se necesita para confirmar del todo (requiere acceso al EC2)
```
sudo docker ps
sudo docker logs nicolet_backend --tail 200
sudo docker logs nicolet_mysql --tail 100
free -m
```
