# Deployment Guide

This application is designed to be deployed using Docker, making it highly compatible with platforms like **Coolify**, Railway, or self-hosted VPS environments.

## 1. Prerequisites
- Docker and Docker Compose installed on your server.
- A public GitHub repository containing this code.

## 2. Environment Variables
Ensure the following variables are configured in your deployment platform:

### Core Node.js Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `PORT` | The port the server will listen on. | 3000 |
| `NODE_ENV` | Set to `production` for optimized delivery. | `development` |
| `GEMINI_API_KEY` | **Required** for AI Media Generation. | - |

### PHP & Deployment Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `APP_DATA_RESET` | Set to `"true"` to force-reset `my-data` volume from `content/` on deploy. | `"false"` |
| `APP_ADMIN_PASSKEY` | Passkey for accessing the browser-based PHP Admin area. | - |
| `ACTIVE_SITE_OVERRIDE` | Force the PHP router to serve a specific site folder. | - |

## 3. Coolify Stack-Specific Guides

We have prepared detailed setup instructions for each technology stack. Select the guide that matches your repository:

- **[PHP Multisite Stack](./coolify-setup-php.md)**: Optimized Apache/PHP setup with persistent volume mapping for multi-site data.
- **[React Frontend Stack](./coolify-setup-react.md)**: Vite-based React deployment using a Node.js serve container.
- **[Vue Frontend Stack](./coolify-setup-vue.md)**: Vite-based Vue deployment with specialized static serving.
- **[Python API Stack](./coolify-setup-python.md)**: FastAPI deployment using Uvicorn with worker scaling tips.

## 4. General Docker Compose Setup (Coolify / VPS)

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

## 5. Performance Scaling
For production deployments on multi-core servers, we recommend using PM2 Cluster Mode to utilize all available CPU cores.

See the [Node.js Multi-Core Scaling Guide](./coolify-node-scaling.md) for detailed instructions.

## 6. Persistence Note & Volume Initialization
**CRITICAL:** The `/app/repo-php/my-data` directory (which maps to `/var/www/html/my-data` in the PHP container) should be persistent.

### Automatic Synchronization
The `entrypoint.sh` script handles the initial setup of your persistent volume:
1. **First Boot:** If `my-data` is empty or missing `config.php`, it automatically copies all site templates from the `content/` directory into the volume.
2. **Forced Reset:** By setting `APP_DATA_RESET="true"` in your environment variables, the system will **WIPE** the `my-data` volume and re-sync it from the bundled `content/` directory on every deployment. This is useful for rolling back manual changes or resetting to the repository structure.

Always use a Docker Volume to map `my-data` to your host machine to ensure your changes persist across container updates.
