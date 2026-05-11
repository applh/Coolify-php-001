# PHP CMS Manager (Full-Stack)

A powerful, multi-tenant PHP content management system powered by a modern Node.js & Vue 3 dashboard. Manage dozens of independent PHP websites from a single interface with AI-driven media generation.

## 🚀 Quick Start (Management Dashboard)

1. **Install Dependencies**: `npm install`
2. **Launch Dashboard**: `npm run dev`
3. **Open Dashboard**: Go to `http://localhost:3000` to start creating and managing your PHP sites.

## 📖 Detailed Instructions

We have provided comprehensive guides in the `docs/` directory:

- **[Installation & Dev Setup](./docs/setup-guide.md)**: Deep dive into the Node/Express/SQLite stack.
- **[CMS Management Guide](./docs/cms-guide.md)**: How to create sites, use the PHP `Layout` engine, and manage AI media.
- **[Deployment Guide](./docs/deployment.md)**: Steps to deploy using Docker and Coolify.
- **[Architecture](./docs/php-cms-architecture.md)**: Understanding how the Node API interacts with the PHP filesystem.

---

## 🛠 Features

- **Multi-Site Dashboard**: Create and delete PHP sites instantly.
- **AI Media Copilot**: Automated generation of missing assets using **Google Gemini**.
- **Modern PHP Framework**: Integrated `Layout` and `CMS` classes for elegant, consistent site design.
- **Real-time Editor**: Edit PHP templates directly from the browser.
- **Docker Ready**: Pre-configured for seamless deployment to production environments.

---

## 📁 Project Structure

```text
.
├── repo-php/               # Core PHP Engine & Website Content
│   ├── class/              # Global PHP Helpers (Layout, CMS)
│   ├── content/            # Directory for independent sites
│   └── public/             # Entry point for the PHP application
├── src/                    # Vue 3 Frontend (CMS Dashboard)
├── server.ts               # Express Backend (API & Site Management)
├── cms.db                  # SQLite database for task tracking
├── Dockerfile              # Production container config
└── docs/                   # Exhaustive documentation
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

## Media Assets & Git Tracking
This project includes a workaround to ensure that binary media files (images, etc.) added via the CMS are tracked by Git (which normally handles text better or might be configured to ignore large binaries).

1. **Zip Workaround**: When a media file is uploaded via the Node.js CMS, a companion `.zip` file is created for that specific file (e.g., `image.jpg` gets `image.jpg.zip`).
2. **Automatic Restoration**: 
   - On Node.js startup, it scans the `content/` directory and restores any missing media files from their respective `.zip` companions.
   - The PHP Router (`Router.php`) is also intelligent: if a requested media file is missing, it checks for a `.zip` version, extracts it on-the-fly to the expected location, and serves the binary content.
3. **Why?**: This allows the "Source of Truth" for your images to be committed to Git as compressed archives, ensuring they survive environment resets while remaining accessible to both the CMS and the live PHP sites.
