# Universal Polyglot CMS Manager PRO

A high-performance, multi-tenant management system powered by a modern Node.js & Vue 3 dashboard. Manage dozens of independent stacks (**PHP, Python, React, Vue, Go, Rust, Flutter, Android**) from a single interface with AI-driven media generation and benchmarked deployment patterns.

## 🚀 Quick Start (Management Dashboard)

1. **Install Dependencies**: `npm install`
2. **Launch Dashboard**: `npm run dev`
3. **Open Dashboard**: Go to `http://localhost:3000` to start creating and managing your sites.

## 📖 Detailed Instructions

We have provided comprehensive guides in the `docs/` directory:

- **[Multi-Stack Expansion Plan](./docs/plan-expansion.md)**: Details on the Go, Rust, Flutter, and Android setups.
- **[Installation & Dev Setup](./docs/devops/setup-guide.md)**: Deep dive into the Node/Express/SQLite stack.
- **[Stack Comparison](./docs/stack-comparison.md)**: Evaluation of the original technology stacks.
- **[Development Plan](./docs/plan.md)**: High-level roadmap and feature progress.
- **[Benchmarking Guide](./docs/benchmarking-guide.md)**: Instructions for the site performance monitoring tool.
- **[AI Studio Recommendations](./docs/ai-agents/workflow-recommendations.md)**: Workflow and model selection guide for AI-driven development.
- **[CMS Management Guide](./docs/backend/cms-guide.md)**: How to create sites, use the PHP `Layout` engine, and manage AI media.
- **[Admin Handbook](./docs/user-guides/user-admin-handbook.md)**: A user-friendly guide to managing sites and forms.
- **[Coolify Setup: PHP Stack](./docs/devops/coolify-setup-php.md)**: Detailed PHP multisite deployment.
- **[Deployment Guide](./docs/devops/deployment.md)**: General steps to deploy using Docker and Coolify.

---

## 🛠 Features

- **8+ Tech Stacks Supported**: Instant tracking for PHP, Python, React, Vue, Go, Rust, Flutter, and Android.
- **AI Media Copilot**: Automated generation of missing assets using **Google Gemini**.
- **Coolify Ready**: Every stack is pre-configured with Docker Compose optimized for self-hosting with Coolify.
- **Performance Focused**: Includes Go and Rust for high-performance microservices.
- **Hybrid Mobile Support**: Scaffolding for Flutter Web and Android build pipelines.

---

## 📁 Project Structure

```text
.
├── repo-php/               # Core PHP Engine & Website Content
├── repo-react/             # Vite + React + TypeScript Stack
├── repo-vue/               # Vite + Vue + TypeScript Stack
├── repo-python/            # FastAPI + Uvicorn Python Stack
├── repo-go/                # Go Microservice
├── repo-rust/              # Rust Actix Web Service
├── repo-flutter/           # Flutter Web Application
├── repo-android/           # Android Build Environment
├── src/                    # Vue 3 Frontend (CMS Dashboard)
├── server.ts               # Express Backend (API & Site Management)
├── docs/                   # Exhaustive documentation
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
