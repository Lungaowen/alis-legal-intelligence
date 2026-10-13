# Combined ALIS deployment: Java WAR + Python FastAPI + Celery worker.
# The Python backend is public and pinned to a known commit for repeatable builds.

FROM maven:3.9-eclipse-temurin-17 AS java-build
WORKDIR /build/java
COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package \
    && test -s target/alis.war

FROM tomcat:10.1-jdk17-temurin
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       ca-certificates git python3 python3-venv build-essential supervisor \
    && rm -rf /var/lib/apt/lists/*

ARG ALIS_PYTHON_COMMIT=57df7c9dfcfb7ee21937c9714c890bbe94214b22
RUN git clone --depth 1 --branch main https://github.com/Lungaowen/alis_python_backend.git /opt/alis-python \
    && test "$(git -C /opt/alis-python rev-parse HEAD)" = "${ALIS_PYTHON_COMMIT}"

RUN python3 -m venv /opt/alis-venv \
    && /opt/alis-venv/bin/python -m pip install --upgrade pip \
    && /opt/alis-venv/bin/pip install --no-cache-dir -r /opt/alis-python/requirements.txt

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=java-build /build/java/target/alis.war /usr/local/tomcat/webapps/alis.war
COPY deploy/supervisord.conf /etc/supervisor/conf.d/alis.conf
COPY deploy/entrypoint.sh /usr/local/bin/alis-entrypoint

RUN chmod +x /usr/local/bin/alis-entrypoint \
    && mkdir -p /var/log/supervisor /opt/alis-python/reports

EXPOSE 10000
ENTRYPOINT ["/usr/local/bin/alis-entrypoint"]
