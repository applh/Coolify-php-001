# Deployment & Persistence Guide

## Coolify Deployment (Recommended)

### Using Docker Compose
The project includes a `docker-compose.yml` that sets up two services: `app` (PHP-FPM) and `web` (Nginx).

1. Create a new **Application** in Coolify.
2. Select your Git repository.
3. Set the **Build Pack** to `Docker Compose`.

---

## Data Persistence (Volumes)

By default, the `content/` folder is part of the Git repository. If you want to modify content directly on the server (via SFTP or Coolify Storage tab) without pushing to Git, you MUST use Persistent Volumes.

### 1. Configure via `docker-compose.yml`
You can define a named volume or a bind mount in your compose file:

```yaml
services:
  app:
    volumes:
      - content_data:/var/www/html/content
  web:
    volumes:
      - content_data:/var/www/html/content:ro # Read-only for Nginx

volumes:
  content_data:
    driver: local
```

### 2. Configure via Coolify UI
1. Go to your Application -> **Storage**.
2. Click **Add Volume**.
3. **Mount Path**: `/var/www/html/content`
4. **Host Path**: Leave empty for an automatic named volume, or specify a path like `/var/www/my-cms/content`.

**Why Persist?**
- Allows using a web-based Markdown editor or SFTP to update content live.
- Prevents data loss if you `rm -rf` the local content folder during a manual build step (though Git usually protects this).

---

## Environment Variables
Set these in the **Environment Variables** tab in Coolify:

| Variable | Description | Example |
|----------|-------------|---------|
| `APP_ENV` | Environment type | `production` |
| `ACTIVE_SITE_OVERRIDE` | Force a site for debug | `site1.com` |
| `TZ` | System Timezone | `Europe/Paris` |
