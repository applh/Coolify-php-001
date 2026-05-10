# PHP Multi-Site Starter

A minimalist, high-performance PHP boilerplate optimized for hosting multiple dynamic or static PHP websites within a single **Coolify** instance.

## Features
- **Multi-domain Support**: Serve an unlimited number of websites from a single container cluster.
- **Dynamic Routing**: Automatically maps domains to their respective folders in the `content/` directory.
- **Custom Mapping**: Optionally map specific subdomains or entirely different domains to existing folders.
- **PHP 8.5-FPM**: Uses a stable, high-performance PHP 8.5 Docker container without bloat.
- **Nginx proxy**: Pre-configured reverse proxy serving as a fast static asset server and request router.
- **Developer Friendly**: Use environment variables to test specific sites locally without `/etc/hosts` modifications.

## Architecture & Workflow
The system uses a simple yet powerful routing mechanism located in `repo-php/public/index.php`. 
1. The user visits `https://your-domain.com`.
2. Traffic goes through Coolify -> Nginx -> PHP-FPM.
3. `index.php` checks the incoming `Host` header.
4. It first checks `my-data/config.php` (if it exists) to see if the domain is explicitly mapped to a folder.
5. If not, it attempts to find a folder matching the domain name precisely inside `my-data/your-domain.com/` (or falls back to `content/your-domain.com/`).
6. If the folder exists, its local `index.php` is included, serving the site seamlessly.

## Quick Start
To create a new site:
1. Create a folder in `repo-php/content/` (e.g., `repo-php/content/my-new-site.com`).
2. Add an `index.php` file inside it.
3. Point your DNS for `my-new-site.com` to your Coolify server.
4. Add the domain `https://my-new-site.com` to the Coolify Application settings.
5. Once deployed, the new site template will be automatically copied to `my-data/` on the first boot (if the persistent volume hasn't been initialized yet) or you can create it directly in the persistent volume via SFTP.

---

## Deployment to Coolify

This project is structured so that the core PHP backend lives inside the `repo-php` directory. Follow these exact steps to deploy to Coolify:

1. **Create Resource**: In your Coolify dashboard, select **Create New Resource** and choose **Application**.
2. **Source**: Choose your Git provider (e.g., GitHub) and select the repository containing this code.
3. **Configuration Settings**:
   - **Build Pack**: Select `Docker Compose`.
   - **Base Directory**: **IMPORTANT:** Set this to `/repo-php`. Because the Docker configuration and application files are located in this subfolder, Coolify needs this explicitly set as the root for the build.
   - **Docker Compose File**: Set to `docker-compose.yml`.
4. **Environment Variables**: Customize your environment variables in the Coolify UI. You can specify `ACTIVE_SITE_OVERRIDE` if you need to test a specific site without having the live domain mapped yet.
5. **Domains & Routing**: Configure the FQDNs (Fully Qualified Domain Names) in Coolify. If you want this one application to serve multiple domains using the built-in multi-domain support, ensure all corresponding domains (like `https://site1.com`, `https://site2.com`) are routed to this Coolify application.
6. **Deploy**: Click the **Deploy** button. Coolify will parse the Docker Compose file, build both the `app` (PHP-FPM) and `web` (Nginx) containers, and start them together.

## Local Development
If you are developing locally without custom domains or modified hosts, set the following environment variable in your `.env` file to force load a specific site directory:
```env
ACTIVE_SITE_OVERRIDE="sambazen.net"
```

## Custom Domain Mapping
To explicitly map custom domain names (or multiple domains/subdomains) to a single site folder inside your data directory, create a `config.php` file in the `my-data/` volume (on first boot, `repo-php/content/config.php.example` is copied to `my-data/config.php`).

```php
<?php
// my-data/config.php
return [
    'www.example.com' => 'site1.com',
    'sales.example.com' => 'site1.com',
    'landing.example.com' => 'site2.com'
];
```
This configuration bypasses the default behavior, allowing advanced multi-tenant routing from a single codebase.

## Persistence
User data and site configurations are separated into their own persistent Docker volume mounted at `/var/www/html/my-data`.
If this volume is empty (or missing `config.php`), the container automatically initializes it by copying the contents of the `repo-php/content/` directory into `my-data/` on startup. This allows deploying standard templates while letting users safely edit files inside `my-data` without being overwritten by Git updates.

---

## Project Structure
```text
.
├── repo-php/
│   ├── my-data/                # Persistent volume for user templates & config
│   │   ├── config.php          # Domain routing config
│   │   └── site1.com/          # Site files
│   ├── content/                # Seed templates loaded into my-data on first boot
│   │   └── config.php.example  
│   ├── public/                 # Document Root for Nginx
│   │   ├── css/, js/           # Shared static assets 
│   │   └── index.php           # Master routing script
│   ├── nginx.conf              # Nginx server configuration for the web container
│   ├── Dockerfile              # PHP-FPM container configuration
│   ├── Dockerfile.web          # Nginx container configuration
│   └── docker-compose.yml      # Local and remote orchestration config
├── AGENTS.md                   # AI Agent rules for website generation
└── metadata.json               # Internal configuration for Google AI Studio
```

## Troubleshooting

### Web Domain & Routing Issues
If your domain is not resolving to the correct site (e.g., getting a 404 or the wrong content), check the server logs in Coolify:
1. Navigate to your application in the Coolify dashboard.
2. Click on the **Logs** tab.
3. Check the logs for the `web` (Nginx) container. Ensure that Nginx is receiving the incoming requests and look for `404` errors. 
4. Check the logs for the `app` (PHP) container for any backend warnings or errors.

### Debugging PHP Host Resolution
In `repo-php/public/index.php`, the active site folder is determined by the `$_SERVER['HTTP_HOST']` variable. If you are using a reverse proxy (like Coolify's built-in Traefik proxy), ensure that the `Host` header is genuinely reaching the container.
- If you're encountering the **"404 - Site Not Configured"** error, it means the host header doesn't match a folder name inside `/my-data` (or `/content`) and there's no matching override in `config.php`.
- You can debug this by temporarily modifying `repo-php/public/index.php` to include `error_log("Incoming Host: " . $_SERVER['HTTP_HOST']);` and observing the Coolify app logs to see what domain Nginx/PHP is actually processing.
