#!/usr/bin/env bash
set -euo pipefail

# Never fall back to the Redis URL hard-coded in the legacy Python config.
: "${REDIS_URL:?Set REDIS_URL to the internal URL of a Render Key Value instance}"

PORT="${PORT:-10000}"
if ! [[ "$PORT" =~ ^[0-9]+$ ]]; then
  echo "PORT must be numeric; received: $PORT" >&2
  exit 1
fi

# Render routes HTTP traffic to PORT. Update only Tomcat's HTTP connector.
python3 - "$PORT" <<'PY'
import re
import sys
from pathlib import Path

port = sys.argv[1]
path = Path("/usr/local/tomcat/conf/server.xml")
config = path.read_text(encoding="utf-8")
updated, count = re.subn(
    r'(<Connector\b[^>]*?\bport=")8080(")',
    lambda match: f"{match.group(1)}{port}{match.group(2)}",
    config,
    count=1,
    flags=re.DOTALL,
)
if count != 1:
    raise SystemExit("Could not find Tomcat's default HTTP connector on port 8080")
path.write_text(updated, encoding="utf-8")
PY

# Keep the JVM conservative; Java, FastAPI, and Celery share this instance.
export CATALINA_OPTS="${CATALINA_OPTS:-} -XX:+UseSerialGC -XX:MaxRAMPercentage=42.0"

exec /usr/bin/supervisord -n -c /etc/supervisor/supervisord.conf
