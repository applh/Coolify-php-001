# PHP Multi-Site Starter

A minimalist, high-performance PHP boilerplate optimized for **Coolify**.

## Features
- **Multi-domain Support**: Serve different sites from the `content/` folder based on the domain name.
- **PHP 8.5-FPM**: Uses a stable, high-performance PHP 8.5 version in a Docker container.
- **Nginx**: Pre-configured reverse proxy.
- **Developer Friendly**: Use `ACTIVE_SITE_OVERRIDE` environment variable to test sites locally.

## Architecture
- `repo-php/public/index.php`: The main router. It detects the domain and loads the corresponding `index.php` from `/content/{domain}/`.
- `repo-php/content/`: Contains subdirectories for each site. Each must have an `index.php`.

## Deployment to Coolify

This project is structured so the PHP backend is contained within the `repo-php` directory. Follow these exact steps to deploy to Coolify:

1. **Create Resource**: In your Coolify dashboard, select **Create New Resource** and choose **Application**.
2. **Source**: Choose your Git provider (e.g., GitHub) and select the repository containing this code.
3. **Configuration Settings**:
   - **Build Pack**: Select `Docker Compose`.
   - **Base Directory**: **IMPORTANT:** Set this to `/repo-php`. Because the Docker configuration and application files are located in this subfolder, Coolify needs this explicitly set as the root for the build.
   - **Docker Compose File**: Set to `docker-compose.yml`.
4. **Environment Variables**: Customize your environment variables in the Coolify UI. You can specify `ACTIVE_SITE_OVERRIDE` if you need to test a specific site without having the live domain mapped yet.
5. **Domains & Routing**: Configure the FQDNs (Fully Qualified Domain Names) in Coolify. If you want this one application to serve multiple domains using the built-in multi-domain support, ensure all corresponding domains (like `site1.com`, `site2.com`) are routed to this Coolify application.
6. **Deploy**: Click the **Deploy** button. Coolify will parse the Docker Compose file, build both the `app` (PHP-FPM) and `web` (Nginx) containers, and start them together.

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
- `AGENTS.md`: AI Agent rules to follow when generating or modifying websites inside this project.
- `package.json`: AI Studio build configuration.

## Troubleshooting

### Web Domain & Routing Issues
If your domain is not resolving to the correct site (e.g., getting a 404 or the wrong content), check the server logs in Coolify:
1. Navigate to your application in the Coolify dashboard.
2. Click on the **Logs** tab.
3. Check the logs for the `web` (Nginx) container. Ensure that Nginx is receiving the incoming requests and look for any 404 errors. 
4. Check the logs for the `app` (PHP) container for any backend warnings or errors.

**Debugging PHP Host Resolution:**
In `repo-php/public/index.php`, the active site folder is determined by the `$_SERVER['HTTP_HOST']` variable. If you are using a reverse proxy (like Coolify's built-in Traefik proxy), ensure that the `Host` header is genuinely reaching the container.
- If you're encountering the "404 - Site Not Configured" error, it means the host header doesn't match a folder name inside `/content`.
- You can debug this by temporarily modifying `repo-php/public/index.php` to include `error_log("Incoming Host: " . $_SERVER['HTTP_HOST']);` and observing the Coolify app logs to see what domain Nginx/PHP is actually processing.
