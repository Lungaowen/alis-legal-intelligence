#!/usr/bin/env bash
set -euo pipefail

# Never fall back to the Redis URL hard-coded in the legacy Python config.
: "${REDIS_URL:?Set REDIS_URL to the internal URL of a Render Key Value instance}"

PORT="${PORT:-10000}"
if ! [[ "$PORT" =~ ^[0-9]+$ ]]; then
  echo "PORT must be numeric; received: $PORT" >&2
  exit 1
fi

# Render should only see the public HTTP connector. Disable Tomcat's separate
# shutdown socket so platform port detection cannot mistake it for HTTP.
python3 - "$PORT" <<'PY'
import re
import sys
from pathlib import Path

port = sys.argv[1]
path = Path("/usr/local/tomcat/conf/server.xml")
config = path.read_text(encoding="utf-8")

config, server_count = re.subn(
    r'(<Server\b[^>]*?\bport=")-?\d+(")',
    lambda match: f"{match.group(1)}-1{match.group(2)}",
    config,
    count=1,
    flags=re.DOTALL,
)
if server_count != 1:
    raise SystemExit("Could not disable Tomcat's shutdown socket")

config, connector_count = re.subn(
    r'(<Connector\b[^>]*?\bport=")8080(")',
    lambda match: f"{match.group(1)}{port}{match.group(2)}",
    config,
    count=1,
    flags=re.DOTALL,
)
if connector_count != 1:
    raise SystemExit("Could not find Tomcat's default HTTP connector on port 8080")

path.write_text(config, encoding="utf-8")
PY

# Keep the JVM conservative; Java, FastAPI, and Celery share this instance.
export CATALINA_OPTS="${CATALINA_OPTS:-} -XX:+UseSerialGC -XX:MaxRAMPercentage=42.0"

exec /usr/bin/supervisord -n -c /etc/supervisor/supervisord.conf
