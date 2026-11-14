#!/usr/bin/env bash
# Actualiza APP_FRONTEND_URL en .env con la IP pública actual de la instancia (metadatos de EC2).
# El servicio systemd "nicolet" lo ejecuta antes de levantar los contenedores: así, si la IP cambió
# al reiniciar (sin IP elástica), los enlaces de los correos siguen apuntando a la dirección correcta.
# Solo toca el valor si es una IP (http://x.x.x.x) o el marcador del script de arranque; si usas
# un dominio propio no lo modifica. Nunca falla el arranque. PUBLIC_IP permite forzar la IP (pruebas).
set -u
ENV_FILE="${1:-$(dirname "$0")/../.env}"
[ -f "$ENV_FILE" ] || exit 0

ip="${PUBLIC_IP:-}"
if [ -z "$ip" ]; then
  for _ in 1 2 3 4 5; do
    token="$(curl -fsS -m 3 -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' 2>/dev/null || true)"
    [ -n "$token" ] && ip="$(curl -fsS -m 3 -H "X-aws-ec2-metadata-token: $token" http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || true)"
    [ -n "$ip" ] && break
    sleep 3
  done
  [ -z "$ip" ] && ip="$(curl -fsS -m 3 https://checkip.amazonaws.com 2>/dev/null || true)"
fi
printf '%s' "$ip" | grep -Eq '^[0-9]{1,3}(\.[0-9]{1,3}){3}$' || exit 0

current="$(grep '^APP_FRONTEND_URL=' "$ENV_FILE" | head -n1 | cut -d= -f2-)"
case "$current" in
  http://CAMBIAR_IP_ELASTICA | http://[0-9]*.[0-9]*.[0-9]*.[0-9]*) ;;
  *) exit 0 ;;
esac
[ "$current" = "http://$ip" ] && exit 0

sed -i "s|^APP_FRONTEND_URL=.*|APP_FRONTEND_URL=http://$ip|" "$ENV_FILE"
echo "[nicolet] APP_FRONTEND_URL actualizado a http://$ip"
exit 0
