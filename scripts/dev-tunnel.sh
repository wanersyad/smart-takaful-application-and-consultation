#!/usr/bin/env bash
# Run the app locally behind a public Cloudflare quick tunnel so ToyyibPay can reach
# /payment/callback. The trycloudflare.com URL changes on every run, so this script
# writes the fresh URL into APP_BASE_URL in .env before starting Spring Boot.
#
# Usage: ./scripts/dev-tunnel.sh      (Ctrl+C stops both the app and the tunnel)
set -euo pipefail

cd "$(dirname "$0")/.."

PORT="$(grep -E '^PORT=' .env | cut -d= -f2)"
PORT="${PORT:-8081}"
LOG="$(mktemp -t cloudflared)"

if ! command -v cloudflared >/dev/null; then
  echo "cloudflared not found. Install it with: brew install cloudflared" >&2
  exit 1
fi

if lsof -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port $PORT is already in use. Stop the running app first." >&2
  exit 1
fi

cloudflared tunnel --no-autoupdate --url "http://localhost:$PORT" >"$LOG" 2>&1 &
TUNNEL_PID=$!
# On exit, stop the tunnel and point APP_BASE_URL back at localhost so a dead tunnel URL is never left behind.
trap 'kill $TUNNEL_PID 2>/dev/null; rm -f "$LOG"; sed -i "" "s|^APP_BASE_URL=.*|APP_BASE_URL=http://localhost:$PORT|" .env' EXIT

echo "Starting Cloudflare tunnel..."
URL=""
for _ in $(seq 1 30); do
  URL="$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' "$LOG" | head -1 || true)"
  [ -n "$URL" ] && break
  sleep 1
done

if [ -z "$URL" ]; then
  echo "Tunnel did not start. cloudflared output:" >&2
  cat "$LOG" >&2
  exit 1
fi

sed -i '' "s|^APP_BASE_URL=.*|APP_BASE_URL=$URL|" .env

echo
echo "  Public URL : $URL"
echo "  Callback   : $URL/payment/callback"
echo "  Local URL  : http://localhost:$PORT"
echo

./mvnw spring-boot:run
