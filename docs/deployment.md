# Deployment Guide

This application is designed to be deployed using Docker, making it highly compatible with platforms like **Coolify**, Railway, or self-hosted VPS environments.

## 1. Prerequisites
- Docker and Docker Compose installed on your server.
- A public GitHub repository containing this code.

## 2. Environment Variables
Ensure the following variables are configured in your deployment platform:

| Variable | Description |
| :--- | :--- |
| `PORT` | The port the server will listen on (default is 3000). |
| `NODE_ENV` | Set to `production`. |
| `GEMINI_API_KEY` | Your Google AI Studio API key for media generation. |

## 3. Docker Compose Setup (Coolify / VPS)

You can use the provided `docker-compose.yml` file. It ensures data persistence for both the SQLite database and your multi-site PHP content.

```yaml
services:
  php-cms-manager:
    build: .
    ports:
      - "${PORT:-3000}:3000"
    volumes:
      - cms_data:/app/cms.db
      - php_content:/app/repo-php/content
    environment:
      - NODE_ENV=production
      - GEMINI_API_KEY=${GEMINI_API_KEY}
    restart: always

volumes:
  cms_data:
  php_content:
```

## 4. Manual Docker Build & Run
If you prefer building manually:

```bash
# Build the image
docker build -t php-cms-manager .

# Run the container with persistent storage
docker run -d \
  -p 3000:3000 \
  -v $(pwd)/cms.db:/app/cms.db \
  -v $(pwd)/repo-php/content:/app/repo-php/content \
  -e GEMINI_API_KEY=your_key_here \
  --name cms-manager \
  php-cms-manager
```

## 5. Persistence Note
**CRITICAL:** The `/app/repo-php/content` directory contains all your website data. Always use a Docker Volume to map this directory to your host machine to avoid data loss during container restarts or updates.
