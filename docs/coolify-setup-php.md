# Coolify Setup: PHP Multi-Site Stack

This guide provides detailed instructions for deploying the **repo-php** stack using Coolify. This stack is a specialized PHP environment designed for managing multiple sites within a single container.

## 1. Project Configuration

In Coolify, create a new **Application** and point it to the subdirectory `/repo-php` if you are using a monorepo structure, or keep it at the root if you isolated it.

### Docker Configuration
- **Build Pack**: Docker Compose (recommended) or Dockerfile.
- **Docker Compose Location**: `/repo-php/docker-compose.yml`
- **Dockerfile Location**: `/repo-php/Dockerfile.web` (This is the specialized Apache/PHP build).

## 2. Environment Variables

Configure these variables in the Coolify dashboard under the **Environment Variables** tab:

| Variable | Required | Description |
| :--- | :--- | :--- |
| `APP_ADMIN_PASSKEY` | Yes | Secure password to access the PHP Admin dashboard. |
| `ACTIVE_SITE_OVERRIDE` | No | Set this to a folder name in `content/` (e.g., `onepage.demo`) to force serve one site. |
| `APP_DATA_RESET` | No | Set to `true` to overwrite `my-data` with the base `content/` on every deploy. |

## 3. Storage & Persistence (Volumes)

The PHP stack requires persistent storage for user-uploaded content and site configurations.

### Required Volumes
In Coolify's **Storage** section, add the following volume:

| Name | Destination Path |
| :--- | :--- |
| `php_content` | `/app/repo-php/my-data` (if using the root docker-compose) or `/var/www/html/my-data` (if using Dockerfile.web directly) |

**Note:** The `entrypoint.sh` script automatically populates this volume with the demo sites on the first boot if it's empty.

## 4. Multi-Site Domain Mapping

To host multiple domains (e.g., `site1.com`, `site2.com`) using this single container:

1. **VDS/Coolify Setup**: Point all domains to your Coolify server IP.
2. **FQDNs**: In Coolify, you can add multiple FQDNs separated by commas:
   `https://site1.com, https://site2.com, https://admin.your-ip.com`
3. **Internal Routing**: The `class/Router.php` and `class/AdminRouter.php` handle the logic. By default, it looks at the `HTTP_HOST` to determine which folder in `content/` to serve.

## 5. Troubleshooting

- **Permissions**: If images don't load or forms fail to save, ensure the `my-data` directory is writable by the `www-data` user (handled automatically by our Dockerfile).
- **Logs**: Access logs via the Coolify dashboard under "Logs" -> "Application Logs".
