#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

BACKEND="${1:-}"
PROFILE="${2:-core}"

if [[ "$BACKEND" != "netty" && "$BACKEND" != "pekko-http" ]]; then
  echo "Usage: $0 <netty|pekko-http> [core|full|all]" >&2
  exit 2
fi

if [[ "$PROFILE" != "core" && "$PROFILE" != "full" && "$PROFILE" != "all" ]]; then
  echo "Usage: $0 <netty|pekko-http> [core|full|all]" >&2
  exit 2
fi

if ! command -v "${AUTOBAHN_DOCKER:-docker}" >/dev/null 2>&1; then
  echo "Docker is required to run the Autobahn WebSocket testsuite." >&2
  exit 1
fi

export AUTOBAHN_DOCKER_USER="${AUTOBAHN_DOCKER_USER:-$(id -u):$(id -g)}"

cd "$REPOSITORY_ROOT"
exec sbt "Play-Integration-Test / Test / runMain play.it.http.websocket.AutobahnWebSocketConformance $BACKEND $PROFILE"
