#!/usr/bin/env bash
# Levanta todo Nicolet Estudio (MySQL + backend + frontend) en una instancia EC2 de forma automática.
#
# Qué hace (es idempotente: se puede ejecutar varias veces):
#   1. Instala Docker y git si faltan, y habilita Docker al iniciar el sistema.
#   2. Agrega swap si la instancia tiene poca RAM (el build de Maven/Node se queda sin memoria en 1 GB).
#   3. Clona o actualiza el repositorio en ~/app (la misma ruta que usa el CD de GitHub Actions).
#   4. Crea ~/app/.env con secretos aleatorios si no existe (nunca pisa uno existente).
#   5. Instala el servicio systemd "nicolet": en cada arranque de la instancia (también al encender un
#      lab de AWS Academy) actualiza la IP pública en .env y levanta todo sin intervención.
#   6. Construye y levanta los contenedores y espera a que el backend responda.
#
# Uso A: como "User data" al crear la instancia (corre una vez, como root, en el primer arranque):
#
#     #!/bin/bash
#     curl -fsSL https://raw.githubusercontent.com/Felipex360x/Agendamiento-Cita-Main/main/scripts/ec2-bootstrap.sh | bash
#
# Uso B: a mano en una instancia ya creada:
#
#     sudo bash ~/app/scripts/ec2-bootstrap.sh
#
# Variables opcionales (se pasan antes del comando): REPO_URL, BRANCH, APP_USER, RESET_DB=1.
# Requisitos: Ubuntu o Amazon Linux 2023, salida a Internet y los puertos 80 (HTTP) y 22 (SSH) abiertos
# en el grupo de seguridad. Usa una IP elástica para que la dirección no cambie al reiniciar.

set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/Felipex360x/Agendamiento-Cita-Main.git}"
BRANCH="${BRANCH:-main}"
COMPOSE_FILE="docker-compose.prod.yml"

log() { printf '\n[bootstrap] %s\n' "$*"; }
die() { printf '\n[bootstrap] ERROR: %s\n' "$*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "Ejecútalo como root (sudo bash ec2-bootstrap.sh)."

# Usuario dueño de ~/app: el mismo que usa el CD por SSH (EC2_USER).
if [ -z "${APP_USER:-}" ]; then
  for candidate in ubuntu ec2-user; do
    if id "$candidate" >/dev/null 2>&1; then APP_USER="$candidate"; break; fi
  done
fi
[ -n "${APP_USER:-}" ] || die "No encontré el usuario ubuntu ni ec2-user. Define APP_USER=<usuario>."
APP_HOME="$(getent passwd "$APP_USER" | cut -d: -f6)"
APP_DIR="$APP_HOME/app"
as_app() { runuser -u "$APP_USER" -- "$@"; }

# --- 1. Docker y git -------------------------------------------------------------------------------
install_packages() {
  if command -v apt-get >/dev/null 2>&1; then
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -y
    command -v git >/dev/null 2>&1 || apt-get install -y git
    command -v curl >/dev/null 2>&1 || apt-get install -y curl
    command -v docker >/dev/null 2>&1 || { curl -fsSL https://get.docker.com | sh; }
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y git docker
  else
    die "Sistema no soportado: solo Ubuntu/Debian (apt) o Amazon Linux 2023 (dnf)."
  fi
}

install_compose_plugin() {
  docker compose version >/dev/null 2>&1 && return 0
  log "Instalando el plugin docker compose"
  local arch dest="/usr/local/lib/docker/cli-plugins"
  arch="$(uname -m)"
  mkdir -p "$dest"
  curl -fsSL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-${arch}" -o "$dest/docker-compose"
  chmod +x "$dest/docker-compose"
  docker compose version >/dev/null 2>&1 || die "No se pudo instalar docker compose."
}

log "Instalando dependencias (Docker, git)"
install_packages
systemctl enable --now docker
install_compose_plugin
usermod -aG docker "$APP_USER" || true

# --- 2. Swap si hay poca memoria -------------------------------------------------------------------
mem_mb="$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)"
if [ "$mem_mb" -lt 2000 ] && ! swapon --show --noheadings | grep -q .; then
  log "RAM de ${mem_mb} MB: creando 2 GB de swap para poder compilar"
  fallocate -l 2G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# --- 3. Código --------------------------------------------------------------------------------------
if [ -d "$APP_DIR/.git" ]; then
  log "Actualizando el repositorio en $APP_DIR"
  as_app git -C "$APP_DIR" fetch origin "$BRANCH"
  as_app git -C "$APP_DIR" checkout "$BRANCH"
  as_app git -C "$APP_DIR" pull --ff-only origin "$BRANCH"
else
  log "Clonando $REPO_URL en $APP_DIR"
  as_app git clone --branch "$BRANCH" "$REPO_URL" "$APP_DIR"
fi
cd "$APP_DIR"

# --- 4. Archivo .env con secretos aleatorios -------------------------------------------------------
if [ ! -f .env ]; then
  # Si ya existe un volumen de MySQL, las claves del .env nuevo no coincidirían con las guardadas en él.
  if docker volume ls -q | grep -q 'mysql_prod_data$'; then
    if [ "${RESET_DB:-0}" = "1" ]; then
      log "RESET_DB=1: se borra el volumen de MySQL (se pierden todos los datos)"
      docker compose -f "$COMPOSE_FILE" down -v || true
    else
      die "Existe un volumen de MySQL creado con otras claves y no hay .env. Crea ~/app/.env con esas claves, o si NO necesitas los datos vuelve a ejecutar con RESET_DB=1 (borra la base)."
    fi
  fi

  log "Generando $APP_DIR/.env con secretos aleatorios"
  rand() { openssl rand -base64 36 | tr -d '/+=\n' | cut -c1-32; }

  # IP pública (IMDSv2); si no responde queda un marcador que debes editar a mano.
  public_ip=""
  token="$(curl -fsS -m 3 -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' 2>/dev/null || true)"
  if [ -n "$token" ]; then
    public_ip="$(curl -fsS -m 3 -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || true)"
  fi
  front_url="http://${public_ip:-CAMBIAR_IP_ELASTICA}"

  umask 077
  {
    echo "MYSQL_ROOT_PASSWORD=$(rand)"
    echo "MYSQL_DATABASE=universidad_backend"
    echo "MYSQL_USER=desarrollador"
    echo "MYSQL_PASSWORD=$(rand)"
    echo "JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')"
    echo "APP_FRONTEND_URL=${front_url}"
    echo "TRUSTED_PROXIES=127.0.0.1,::1,172.16.0.0/12"
    echo "APP_ADMIN_EMAIL=admin@nicolet.cl"
    echo "APP_ADMIN_PASSWORD=$(rand)"
    echo "APP_ADMIN_RESET_PASSWORD=false"
    echo "APP_DEMO_DATA_ENABLED=false"
    echo "MAIL_HOST=smtp.gmail.com"
    echo "MAIL_PORT=587"
    echo "MAIL_USERNAME=nacuna888@gmail.com"
    echo "MAIL_PASSWORD="
  } > .env
  chown "$APP_USER":"$APP_USER" .env
  chmod 600 .env
  log "Listo. El correo y la contraseña del administrador están en $APP_DIR/.env (APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD)."
  [ -n "$public_ip" ] || log "AVISO: no pude leer la IP pública; edita APP_FRONTEND_URL en $APP_DIR/.env."
else
  log "Ya existe $APP_DIR/.env: no se modifica"
fi

# --- 5. Servicio systemd: levanta todo en cada arranque de la instancia -----------------------------
log "Instalando el servicio systemd 'nicolet'"
cat > /etc/systemd/system/nicolet.service <<UNIT
[Unit]
Description=Nicolet Estudio (docker compose)
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$APP_DIR
ExecStartPre=-/bin/bash $APP_DIR/scripts/refresh-public-ip.sh $APP_DIR/.env
ExecStart=/usr/bin/docker compose -f $COMPOSE_FILE up -d
ExecStop=/usr/bin/docker compose -f $COMPOSE_FILE stop
TimeoutStartSec=900

[Install]
WantedBy=multi-user.target
UNIT
# En algunas distribuciones docker queda en otra ruta.
docker_bin="$(command -v docker)"
sed -i "s|/usr/bin/docker|$docker_bin|g" /etc/systemd/system/nicolet.service
systemctl daemon-reload
systemctl enable nicolet.service

# --- 6. Construir, levantar y comprobar -------------------------------------------------------------
log "Construyendo y levantando los contenedores (la primera vez tarda varios minutos)"
docker compose -f "$COMPOSE_FILE" up -d --build

log "Esperando a que el backend responda"
for i in $(seq 1 60); do
  if curl -fsS -m 5 http://localhost/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    log "Todo arriba: http://localhost/actuator/health responde UP"
    docker compose -f "$COMPOSE_FILE" ps
    exit 0
  fi
  sleep 5
done

docker compose -f "$COMPOSE_FILE" ps || true
die "El backend no respondió en 5 minutos. Revisa: sudo docker logs nicolet_backend --tail 100"
