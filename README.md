# PHP Multi-Site Starter

A minimalist, high-performance PHP boilerplate optimized for **Coolify**.

## Features
- **Multi-domain Support**: Serve different sites from the `content/` folder based on the domain name.
- **PHP 8.4-FPM**: Uses a stable, high-performance PHP 8.4 version in a Docker container.
- **Nginx**: Pre-configured reverse proxy.
- **Developer Friendly**: Use `ACTIVE_SITE_OVERRIDE` environment variable to test sites locally.

## Architecture
- `repo-php/public/index.php`: The main router. It detects the domain and loads the corresponding `index.php` from `/content/{domain}/`.
- `repo-php/content/`: Contains subdirectories for each site. Each must have an `index.php`.

## Deployment to Coolify
1. Create a new **Application**.
2. Connect your Git repository.
3. Set the **Build Pack** to `Docker Compose`.
4. Deploy!

## Local Development
If you are developing without custom domains, set the following environment variable in your `.env` file to force a specific site:
```env
ACTIVE_SITE_OVERRIDE="site2.com"
```

## Persistence
To persist the `content/` folder (allowing live updates via SFTP without Git pushes), add a persistent volume in Coolify pointing to `/var/www/html/content` (mapped to `repo-php/content` in your repository).

## Project Structure
- `repo-php/`: The main PHP application directory (Base Directory for Coolify).
- `repo-php/public/index.php`: The main entry point (Document Root).
- `repo-php/nginx.conf`: Nginx server configuration for the `web` container.
- `repo-php/Dockerfile`: PHP-FPM container configuration.
- `repo-php/Dockerfile.web`: Nginx container configuration.
- `repo-php/docker-compose.yml`: Local and remote orchestration config.
- `metadata.json`: Internal configuration for Google AI Studio.
- `package.json`: AI Studio build configuration.
