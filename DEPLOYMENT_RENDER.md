# Combined ALIS deployment on Render

This Docker service runs three processes in one container:

- Java/Tomcat: the current `alis-legal-intelligence` WAR, publicly served at `/alis/`.
- Python FastAPI: `alis_python_backend`, bound to `127.0.0.1:8000` so it is not exposed directly to the internet.
- Celery worker: processes Python document tasks from Redis.

## Important scope note

These files package the source currently present in `Lungaowen/alis-legal-intelligence` (the Java Servlet/JSP WAR) and `Lungaowen/alis_python_backend` (the FastAPI/Celery document processor). This is not a deployment of the newer Java triage API or the Next.js triage UI. The frontend's triage workflow needs that newer backend code to be present before this container can serve triage endpoints.

The Python source is pinned to commit `929c8aeb69e81bb8b7df60de7c936806dc277f1f` on the security branch, which removes a hard-coded Redis fallback. Review/merge the accompanying Python change when convenient: https://github.com/Lungaowen/alis_python_backend/pull/1. The credential previously present in source history must be rotated at its original provider; deleting it from the current file does not erase Git history.

## Render setup

Create a **Web Service** from this repository:

- Runtime: Docker
- Branch: `deploy/render-combined`
- Dockerfile path: `./Dockerfile`
- Docker context: repository root
- Region: Frankfurt (same region as Redis)
- Health-check path: `/alis/` (if configurable)
- Plan: currently free for a smoke test; Java, API and worker share memory, so upgrade before production if you see restarts or out-of-memory errors.

The Docker build fetches a pinned Python commit for reproducible builds. After merging a newer Python commit, update both the branch in the `git clone --branch ...` command and `ALIS_PYTHON_COMMIT` to the exact source commit, then redeploy.

## Required environment variables

Set these on the Render service. Do not place credentials in source files.

| Variable | Value |
| --- | --- |
| `REDIS_URL` | Internal connection URL for a Render Key Value instance in the same region. Required; the container exits instead of using a source-code credential fallback. |
| `ALIS_DB_URL` | JDBC URL for a real PostgreSQL database, e.g. `jdbc:postgresql://HOST:5432/DATABASE?sslmode=require`. |
| `ALIS_DB_USER` | Database username. |
| `ALIS_DB_PASSWORD` | Database password. |
| `GROQ_API_KEY` | Required for Python document analysis. |
| `AWS_ACCESS_KEY_ID` | Required if report storage uses S3. |
| `AWS_SECRET_ACCESS_KEY` | Required if report storage uses S3. |
| `ALIS_S3_BUCKET` | S3 bucket used for generated reports. |
| `ALIS_S3_REGION` | S3 region; defaults to `us-east-1`. |
| `JAVA_CALLBACK_SECRET` | Optional shared secret for callbacks from the Python worker. |
| `MAX_FILE_SIZE_MB` | Optional upload limit; defaults to `50`. |
| `MAX_PAGES` | Optional PDF page limit; defaults to `500`. |

The Java connection class reads the database connection from `ALIS_DB_URL`, `ALIS_DB_USER`, and `ALIS_DB_PASSWORD`. A PostgreSQL database with the ALIS schema must be initialized before login, registration or persistent document features will work. The current Render values use a localhost database placeholder, so replace them with a real database before relying on those features.

## Runtime addresses

- Public Java app: `https://alis-java-python.onrender.com/alis/`
- Python API (internal to this container): `http://127.0.0.1:8000`
- Python processing request: `POST http://127.0.0.1:8000/api/process`
- Python task status: `GET http://127.0.0.1:8000/api/status/{task_id}`

Only Tomcat binds to Render's public `PORT`. FastAPI is intentionally loopback-only. This deployment co-locates the processes; the existing Java Servlet application does not automatically gain the newer triage endpoints merely by sharing a container.

## Persistence

Render's container filesystem is ephemeral. The worker's local `reports/` folder is only temporary. Configure S3 environment variables for reports that must persist between deploys. Do not store uploaded legal documents on the container's local filesystem as the only copy.
