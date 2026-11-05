#!/usr/bin/env bash
# Lanzador solo para Linux: base de datos (Docker, solo si hace falta) + Spring Boot + React (Vite).
#
#   ./iniciar.sh                 inicia todo y espera a que esté listo
#   ./iniciar.sh --sin-docker    no intenta iniciar MySQL (exige que ya esté disponible)
#   ./iniciar.sh --ayuda
#
# Variables opcionales: DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD, JWT_SECRET.
# Nunca detiene procesos ajenos ni borra datos/volúmenes. Ctrl+C detiene solo lo que inició este script
# (la base de datos queda activa).

set -u -o pipefail

[ "$(uname -s)" = "Linux" ] || { echo "Este lanzador es solo para Linux." >&2; exit 1; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONT="$ROOT/frontend"
LOGS="$ROOT/logs"
BACK_PORT=8080
FRONT_PORT=5173
USE_DOCKER=1

for arg in "$@"; do
  case "$arg" in
    --sin-docker) USE_DOCKER=0 ;;
    -h|--ayuda) sed -n '2,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Opción desconocida: $arg (usa --ayuda)" >&2; exit 2 ;;
  esac
done

info() { printf '\033[36m%s\033[0m\n' "$*"; }
ok()   { printf '\033[32m%s\033[0m\n' "$*"; }
warn() { printf '\033[33m%s\033[0m\n' "$*" >&2; }
die()  { printf '\033[31mError: %s\033[0m\n' "$*" >&2; exit 1; }

PIDS=()   # líderes de grupo de los procesos que inició este script
cleanup() {
  trap - EXIT INT TERM
  [ "${#PIDS[@]}" -eq 0 ] && return
  info "Deteniendo los procesos iniciados por este script..."
  for pgid in "${PIDS[@]}"; do kill -TERM -- "-$pgid" 2>/dev/null; done
  for _ in $(seq 1 20); do
    alive=0
    for pgid in "${PIDS[@]}"; do kill -0 -- "-$pgid" 2>/dev/null && alive=1; done
    [ "$alive" -eq 0 ] && break
    sleep 0.5
  done
  for pgid in "${PIDS[@]}"; do kill -KILL -- "-$pgid" 2>/dev/null; done
  ok "Listo. La base de datos no se detuvo."
}
trap cleanup EXIT
trap 'exit 0' INT TERM

# ---------------------------------------------------------------- utilidades

port_busy()  { ss -ltn "sport = :$1" 2>/dev/null | tail -n +2 | grep -q .; }
who_uses()   { ss -ltnp "sport = :$1" 2>/dev/null | tail -n +2; }
tcp_open()   { timeout 2 bash -c "exec 3<>/dev/tcp/$1/$2" 2>/dev/null; }

wait_http() { # nombre url segundos pid log
  local name="$1" url="$2" limit="$3" pid="${4:-}" log="${5:-}" waited=0
  while [ "$waited" -lt "$limit" ]; do
    if [ -n "$pid" ] && ! kill -0 "$pid" 2>/dev/null; then
      [ -n "$log" ] && { echo "--- últimas líneas de $log ---" >&2; tail -n 20 "$log" >&2; }
      die "$name terminó antes de estar listo."
    fi
    if curl -fsS -o /dev/null --max-time 3 "$url" 2>/dev/null; then ok "$name listo ($url)"; return 0; fi
    sleep 2; waited=$((waited + 2))
  done
  [ -n "$log" ] && { echo "--- últimas líneas de $log ---" >&2; tail -n 20 "$log" >&2; }
  die "$name no respondió en ${limit}s ($url)."
}

start_group() { # log comando...  -> imprime el PGID del grupo nuevo
  local log="$1"; shift
  setsid "$@" >"$log" 2>&1 &
  local pid=$!
  sleep 0.3
  ps -o pgid= -p "$pid" 2>/dev/null | tr -d ' ' || echo "$pid"
}

# ---------------------------------------------------------------- requisitos

for tool in curl ss setsid timeout; do
  command -v "$tool" >/dev/null || die "Falta la herramienta '$tool'."
done

command -v java >/dev/null || die "No se encontró Java. Instala un JDK 17 o superior."
JAVA_MAJOR="$(java -version 2>&1 | awk -F'"' '/version/ {split($2, v, "."); print (v[1] == "1" ? v[2] : v[1]); exit}')"
[ "${JAVA_MAJOR:-0}" -ge 17 ] 2>/dev/null || die "Se requiere Java 17 o superior (detectado: ${JAVA_MAJOR:-desconocido})."

command -v node >/dev/null && command -v npm >/dev/null || die "Faltan Node.js y npm (ver frontend/.nvmrc)."
NODE_VER="$(node -p 'process.versions.node')"
node_ok() { # ^22.22.2 || ^24.15.0 || >=26.0.0
  local v="$1" major="${1%%.*}"
  [ "$major" -ge 26 ] && return 0
  [ "$major" -eq 22 ] && [ "$(printf '%s\n22.22.2\n' "$v" | sort -V | head -1)" = "22.22.2" ] && return 0
  [ "$major" -eq 24 ] && [ "$(printf '%s\n24.15.0\n' "$v" | sort -V | head -1)" = "24.15.0" ] && return 0
  return 1
}
node_ok "$NODE_VER" || warn "Node $NODE_VER no cumple 'engines' (^22.22.2 || ^24.15.0 || >=26). Puede funcionar, pero no está soportado."

# ---------------------------------------------------------------- puertos

for port in "$BACK_PORT" "$FRONT_PORT"; do
  if port_busy "$port"; then
    warn "El puerto $port ya está en uso:"
    who_uses "$port" >&2
    die "No se detuvo ningún proceso. Libera el puerto $port (o cierra esa aplicación) y vuelve a intentar."
  fi
done

# ---------------------------------------------------------------- variables

if [ -f "$ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
  info "Variables cargadas desde .env"
fi

export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-3306}"
export DB_USERNAME="${DB_USERNAME:-desarrollador}"      # usuario definido en docker-compose.yml
export DB_PASSWORD="${DB_PASSWORD:-password_seguro_123}" # ídem; cámbialo si usas otra base
export MAIL_HOST="${MAIL_HOST:-smtp.gmail.com}"
export MAIL_PORT="${MAIL_PORT:-587}"
export MAIL_USERNAME="${MAIL_USERNAME:-nacuna888@gmail.com}"
export MAIL_PASSWORD="${MAIL_PASSWORD:-}"
if [ -z "${JWT_SECRET:-}" ]; then
  if command -v openssl >/dev/null; then JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
  else JWT_SECRET="$(head -c 48 /dev/urandom | base64 -w0)"; fi
  info "JWT_SECRET generado para esta ejecución (las sesiones no sobreviven a un reinicio)."
fi
export JWT_SECRET

mkdir -p "$LOGS"

# ---------------------------------------------------------------- base de datos

if tcp_open "$DB_HOST" "$DB_PORT"; then
  ok "Base de datos disponible en $DB_HOST:$DB_PORT (se reutiliza)."
else
  [ "$USE_DOCKER" -eq 1 ] || die "No hay base de datos en $DB_HOST:$DB_PORT y se indicó --sin-docker."
  case "$DB_HOST" in 127.0.0.1|localhost) ;; *) die "No hay base de datos en $DB_HOST:$DB_PORT y no es local; no se puede iniciar con Docker." ;; esac
  command -v docker >/dev/null || die "No hay base de datos en $DB_HOST:$DB_PORT y Docker no está disponible."
  info "Iniciando MySQL con Docker Compose (solo el servicio mysql_usuario)..."
  (cd "$ROOT" && docker compose up -d mysql_usuario) || die "No se pudo iniciar MySQL con Docker Compose."
  waited=0
  until docker exec mysql_usuario mysqladmin ping --silent >/dev/null 2>&1 && tcp_open "$DB_HOST" "$DB_PORT"; do
    waited=$((waited + 2)); [ "$waited" -ge 120 ] && die "MySQL no quedó disponible en 120 s. Revisa: docker logs mysql_usuario"
    sleep 2
  done
  ok "MySQL listo."
fi

# ---------------------------------------------------------------- dependencias del frontend

if [ ! -x "$FRONT/node_modules/.bin/vite" ]; then
  info "Instalando dependencias del frontend (npm ci)..."
  (cd "$FRONT" && npm ci --no-audit --no-fund) >"$LOGS/npm-ci.log" 2>&1 \
    || { tail -n 20 "$LOGS/npm-ci.log" >&2; die "Falló 'npm ci' (ver logs/npm-ci.log)."; }
fi

# ---------------------------------------------------------------- servicios

info "Iniciando Spring Boot y React..."
BACK_PGID="$(cd "$ROOT" && start_group "$LOGS/backend.log" ./mvnw -B -ntp spring-boot:run)"
PIDS+=("$BACK_PGID")
FRONT_PGID="$(cd "$FRONT" && start_group "$LOGS/frontend.log" npm run dev -- --host 127.0.0.1 --port "$FRONT_PORT" --strictPort)"
PIDS+=("$FRONT_PGID")

wait_http "Spring Boot"  "http://127.0.0.1:$BACK_PORT/actuator/health"  180 "$BACK_PGID"  "$LOGS/backend.log"
wait_http "Frontend"     "http://127.0.0.1:$FRONT_PORT/"                 60 "$FRONT_PGID" "$LOGS/frontend.log"
wait_http "Salud de Spring vía Vite" "http://127.0.0.1:$FRONT_PORT/actuator/health" 30

echo
ok "Todo listo:"
echo "  Aplicación:  http://localhost:$FRONT_PORT"
echo "  API:         http://localhost:$BACK_PORT/api/v2/nicolet"
echo "  Registros:   $LOGS/backend.log y $LOGS/frontend.log"
echo "Presiona Ctrl+C para detener Spring Boot y React."

while true; do
  sleep 2
  kill -0 "$BACK_PGID"  2>/dev/null || { tail -n 20 "$LOGS/backend.log"  >&2; die "Spring Boot se detuvo."; }
  kill -0 "$FRONT_PGID" 2>/dev/null || { tail -n 20 "$LOGS/frontend.log" >&2; die "El frontend se detuvo."; }
done
