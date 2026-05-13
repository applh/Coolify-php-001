# Coolify Setup: Vite + Vue Stack

This guide provides instructions for deploying the **repo-vue** application using Coolify. This stack contains a Vue 3 application built with Vite.

## 1. Project Configuration

In the Coolify dashboard, create a new **Application** and set the base directory to `/repo-vue`.

### Build Configuration
- **Build Pack**: Docker Compose
- **Docker Compose Location**: `/repo-vue/docker-compose.yml`
- **Port**: 3000

## 2. Docker Setup

The `repo-vue` Dockerfile is optimized for serving single-page applications:
1. It installs dependencies and builds the project using `vue-tsc` (for type checking) and `vite build`.
2. It serves the resulting `dist/` folder using the `serve` library on port 3000.

## 3. Deployment Checklist

- **Base Directory**: Ensure this is set to `/repo-vue` so the builder finds the correct `package.json`.
- **FQDN**: Configure your domain or subdomain (e.g., `https://vue.yourdomain.com`).
- **Health Checks**: Coolify's default health checks work well with this setup as it listens on port 3000.

## 4. Connecting to Other Stacks

If your Vue app needs to communicate with the **repo-python** (FastAPI) or **repo-php** stack:
1. Determine the internal Coolify network URL or the public FQDN of the other service.
2. Add an environment variable like `VITE_API_URL` in the Coolify "Environment Variables" section.
3. Use this variable in your Vue components (`import.meta.env.VITE_API_URL`).

## 5. Troubleshooting HMR in Development

Note that Hot Module Replacement (HMR) is designed for local development. When deployed to a production-like environment on Coolify, the app is built as static files, so HMR is not active. Changes require a redeploy or a "Push to Deploy" setup.
