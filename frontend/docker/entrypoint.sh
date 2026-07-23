#!/bin/sh
set -eu

api_url="${VITE_API_BASE_URL:-}"
ws_url="${VITE_WS_URL:-/ws}"

cat > /usr/share/nginx/html/env.js <<EOF
window.__CBT_PULSE_GRID_CONFIG__ = {
  VITE_API_BASE_URL: "${api_url}",
  VITE_WS_URL: "${ws_url}"
};
EOF
