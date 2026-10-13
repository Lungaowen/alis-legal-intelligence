# Combined ALIS deployment on Render

This Docker service runs three processes in one container:

- Java/Tomcat: the current `alis-legal-intelligence` WAR, publicly served at `/alis/`.
- Python FastAPI: `alis_python_backend`, bound to `127.0.0.1:8000` so it is not exposed directly to the internet.
- Celery worker: processes Python document tasks from Redis.

## Important scope note

These files package the source currently present in `Lungaowen/alis-legal-intelligence` (the Java Servlet/JSP WAR) and `Lungaowen/alis_python_backend` (the FastAPI/Celery processor). This is not a deployment of the newer Java triage API. The frontend's triage workflow needs that newer backend code to be restored/available before this container can serve the triage endpoints.

## Render setup

Create a new **Web Service** from this repository:

- Runtime: Docker
- Branch: `deploy/render-combined`
- Dockerfile path: `./Dockerfile`
- Docker context: repository root
- Region: Frankfurt (or the same region as the Redis instance)
- Health-check path: `/alis/` (if configurable)
- Plan: start with the free plan for a smoke test; the Java, API, and worker share memory, so upgrade before production if you see restarts or out-of-memory errors.

The Docker build downloads the public Python repository at a pinned commit. After changing the Python backend, update `ALIS_PYTHON_COMMIT` in the Dockerfile to the new `main` commit SHA and redeploy.

## Required environment variables

Set these on the Render service. Do not place credentials in source files.

| Variable | Value |
| --- | --- |
| `REDIS_URL` | Internal connection URL for a Render Key Value instance in the same region. Required; the container exits rather than using the old fallback URL. |
| `ALIS_DB_URL` | JDBC URL for the PostgreSQL database, e.g. `jdbc:postgresql://HOST:5432/DATABASE?sslmode=require`. |
| `ALIS_DB_USER` | Database username. |
| `ALIS_DB_PASSWORD` | Database password. |
| `GROQ_API_KEY` | Required for the configured Groq-based Python document analysis. |
| `AWS_ACCESS_KEY_ID` | Optional, if the Python service stores reports in S3. |
| `AWS_SECRET_ACCESS_KEY` | Optional, if the Python service stores reports in S3. |
| `ALIS_S3_BUCKET` | Optional S3 bucket used for generated reports. |
| `ALIS_S3_REGION` | S3 region; defaults to `us-east-1`. |
| `JAVA_CALLBACK_SECRET` | Optional shared secret for callbacks from the Python worker. |
| `MAX_FILE_SIZE_MB` | Optional upload limit; defaults to `50`. |
| `MAX_PAGES` | Optional PDF page limit; defaults to `500`. |

The Java connection class now reads the database connection from `ALIS_DB_URL`, `ALIS_DB_USER`, and `ALIS_DB_PASSWORD`. You still need a PostgreSQL database with the ALIS schema initialized before login, registration, or persistent document features will work.

## Runtime addresses

- Public Java app: `https://YOUR-RENDER-SERVICE.onrender.com/alis/`
- Internal Python API from another process in this same container: `http://127.0.0.1:8000`
- Python processing request: `POST http://127.0.0.1:8000/api/process`
- Python task status: `GET http://127.0.0.1:8000/api/status/{task_id}`

Only Tomcat binds to Render's public `PORT`. FastAPI is intentionally loopback-only. This deployment co-locates the processes; the existing Java Servlet application does not automatically gain the newer triage endpoints merely by sharing a container.

## Persistence

Render's container filesystem is ephemeral. The worker's local `reports/` folder is only temporary. Configure S3 environment variables for reports that must persist between deploys. Do not store uploaded legal documents on the container's local filesystem as the only copy.
