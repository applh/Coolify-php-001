# PHP Coolify Starter

A modern, polished plain PHP boilerplate ready for deployment on [Coolify](https://coolify.io/).

## Features

- **PHP 8.5-FPM**: Modern, high-performance PHP engine.
- **Nginx**: High-performance web server.
- **Public Directory**: Document root moved to `/public` for enhanced security.
- **Docker & Nixpacks Ready**: Includes `Dockerfile`, `docker-compose.yml`, and `nixpacks.toml` for maximum compatibility with Coolify.
- **Modern UI**: Polished landing page using Tailwind CSS.
- **Coolify Optimized**: Configured for seamless git-based deployment.

## Deployment to Coolify

### Option A: Using Docker Compose (Recommended)
1. **New Resource**: Click `+ New` -> `Service`.
2. **Select Repository**: Select this repo.
3. Coolify will automatically use the `docker-compose.yml`.

### Option B: Using Nixpacks (Auto-detection)
If you created an **Application** instead of a **Service**:
1. Coolify will detect the `composer.json` and `nixpacks.toml`.
2. The `nixpacks.toml` explicitly tells Coolify to use **PHP 8.5** and set the document root to `/public`.
3. **Environment Variable**: Ensure you add `NIXPACKS_PHP_ROOT` as `/public` in the Coolify UI if you didn't use the provided config.

## Environment Variables

You can add environment variables in the Coolify UI under the **Environment Variables** tab of your service. 

1. **APP_ENV**: Set to `production` or `staging`.
2. **COOLIFY_APP_SECRET**: Set any sensitive key.

In your PHP code, access them like this:
```php
$secret = getenv('COOLIFY_APP_SECRET');
// or
$env = $_ENV['APP_ENV'];
```

These variables are automatically passed to the PHP-FPM container by Docker Compose.

## Project Structure

- `public/index.php`: The main entry point (Document Root).
- `nginx.conf`: Nginx server configuration.
- `Dockerfile`: PHP-FPM container configuration.
- `docker-compose.yml`: Local and remote orchestration config.
- `metadata.json`: Internal configuration for Google AI Studio (safe to delete after exporting).

## Development vs Production Files

When deploying to **Coolify**, the following files are **not necessary** and can be safely deleted after exporting the project:

- `metadata.json`: Used by Google AI Studio for environment configuration.
- `.env.example`: Should be used as a template to create your `.env` in Coolify, but the example file itself isn't needed by the running app.
- `package.json` & `package-lock.json`: Only present if you used Node.js tools during development in AI Studio. A pure PHP app doesn't require them.

The core production files are inside `public/`, `Dockerfile`, `nginx.conf`, and `docker-compose.yml`.

## Why is metadata.json here?

The `metadata.json` file is a configuration file used by **Google AI Studio** to manage the application's name, description, and required permissions within the AI Studio preview environment. 

When you export this repository to GitHub or download it for production use on **Coolify**, you can safely delete this file as it is not used by PHP, Docker, or Nginx.
