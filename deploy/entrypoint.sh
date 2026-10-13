#!/usr/bin/env bash
set -euo pipefail

: "${REDIS_URL:?Set REDIS_URL}"

PORT="${PORT:-10000}"
python3 - "$PORT" <<'PY'
import sys
from pathlib import Path

port = sys.argv[1]
path = Path("/usr/local/tomcat/conf/server.xml")
config = path.read_text(encoding="utf-8")

old_server = 'port="8005"'
old_connector = 'port="8080"'

if old_server not in config:
    raise SystemExit("Tomcat shutdown port 8005 was not found")
config = config.replace(old_server, 'port="-1"', 1)

if old_connector not in config:
    raise SystemExit("Tomcat HTTP connector port 8080 was not found")
config = config.replace(old_connector, f'port="{port}"', 1)

path.write_text(config, encoding="utf-8")
PY

export CATALINA_OPTS="${CATALINA_OPTS:-} -XX:+UseSerialGC -XX:MaxRAMPercentage=42.0"

exec /usr/bin/supervisord -n -c /etc/supervisor/supervisord.conf
