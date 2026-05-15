# Coolify Setup: Python FastAPI Stack

This guide explains how to deploy the **repo-python** application using Coolify. This is a lightweight Python backend using the FastAPI framework and Uvicorn server.

## 1. Project Configuration

In Coolify, create a new **Application** and set the base directory to `/repo-python`.

### Build Configuration
- **Build Pack**: Docker Compose
- **Docker Compose Location**: `/repo-python/docker-compose.yml`
- **Internal Port**: 3000 (Mapped in `main:app` startup command)

## 2. Dockerfile Details

The Python stack uses the `python:3.11-slim` image for a small footprint:
- It installs requirements from `requirements.txt`.
- It copies the application code.
- It starts the server using: `uvicorn main:app --host 0.0.0.0 --port 3000`

## 3. Environment Variables

If your FastAPI app requires a database (like PostgreSQL or MongoDB) or external API keys, define them in Coolify:

| Variable | Description |
| :--- | :--- |
| `DATABASE_URL` | Connection string for your database. |
| `SECRET_KEY` | Used for JWT authentication or encryption. |

## 4. API Documentation

One of the best features of FastAPI is the automatic docs. Once deployed, you can access your interactive documentation at:
- `https://your-api-domain.com/docs` (Swagger UI)
- `https://your-api-domain.com/redoc` (ReDoc)

## 5. Scaling with Coolify

To improve performance on multi-core servers, you can increase the number of worker processes. 
Modified `CMD` example for `Dockerfile`:
```dockerfile
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "3000", "--workers", "4"]
```
Alternatively, set the `WORKERS` count as an environment variable and use it in your startup command.

## 6. Health Checks

FastAPI's `"GET /"` endpoint returns `{"message": "Hello from FastAPI"}` by default, which is perfect for Coolify's HTTP health checks.
