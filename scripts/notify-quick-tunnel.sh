#!/bin/sh
# Temporary helper: watch cloudflared Quick Tunnel URL and post once to Discord.
set -eu

CONTAINER="${CLOUDFLARED_CONTAINER:-bottabot_cloudflared}"
WEBHOOK_URL="${DISCORD_WEBHOOK_URL:-}"
MAX_WAIT_SEC="${NOTIFY_MAX_WAIT_SEC:-90}"

if [ -z "$WEBHOOK_URL" ]; then
  echo "[tunnel-notify] DISCORD_WEBHOOK_URL is empty; skip notify"
  exit 0
fi

echo "[tunnel-notify] waiting for Quick Tunnel URL from ${CONTAINER}..."

URL=""
i=0
while [ "$i" -lt "$MAX_WAIT_SEC" ]; do
  if docker inspect "$CONTAINER" >/dev/null 2>&1; then
    URL=$(docker logs "$CONTAINER" 2>&1 | grep -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' | tail -n 1 || true)
    if [ -n "$URL" ]; then
      break
    fi
  fi
  i=$((i + 1))
  sleep 1
done

if [ -z "$URL" ]; then
  echo "[tunnel-notify] timed out waiting for trycloudflare.com URL"
  exit 1
fi

echo "[tunnel-notify] found ${URL}; posting to Discord..."

# busybox wget (docker:cli image)
wget -q -O- \
  --header='Content-Type: application/json' \
  --post-data="{\"content\":\"🚀 Quick Tunnel ready: ${URL}\"}" \
  "$WEBHOOK_URL" >/dev/null

echo "[tunnel-notify] notified Discord"
