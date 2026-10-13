#!/usr/bin/env bash
set -euo pipefail
: "\${REDIS_URL:?Set REDIS_URL}"
: "\${ALIS_JWT_SECRET:?Set ALIS_JWT_SECRET}"
: "\${ALIS_DB_URL:?Set ALIS_DB_URL}"
: "\${ALIS_DB_USER:?Set ALIS_DB_USER}"
: "\${ALIS_DB_PASSWORD:?Set ALIS_DB_PASSWORD}"
PORT="\${PORT:-10000}"
python3 - "\$PORT" <<'PY'
import re,sys
from pathlib import Path
p=Path("/usr/local/tomcat/conf/server.xml"); x=p.read_text()
x,c=re.subn(r'(<Server\b[^>]*?\bport=")-?\d+(")',lambda m:f"{m.group(1)}-1{m.group(2)}",x,count=1,flags=re.S)
if c!=1: raise SystemExit("shutdown socket not found")
x,c=re.subn(r'(<Connector\b[^>]*?\bport=")8080(")',lambda m:f"{m.group(1)}{sys.argv[1]}{m.group(2)}",x,count=1,flags=re.S)
if c!=1: raise SystemExit("HTTP connector not found")
p.write_text(x)
PY
export CATALINA_OPTS="\${CATALINA_OPTS:-} -XX:+UseSerialGC -XX:MaxRAMPercentage=42.0"
exec /usr/bin/supervisord -n -c /etc/supervisor/supervisord.conf
