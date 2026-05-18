# Coolify Setup: Vite + React Stack

This guide explains how to deploy the **repo-react** application using Coolify. This is a modern React frontend built with Vite and TypeScript.

## 1. Project Configuration

In Coolify, create a new **Application** and point it to the `/repo-react` directory.

### Build Configuration
- **Build Pack**: Docker Compose
- **Docker Compose Location**: `/repo-react/docker-compose.yml`
- **Port**: 3000 (The internal app port defined in the Dockerfile)

## 2. Dockerfile Explained

The `repo-react` stack uses a two-stage-like approach in a single file for simplicity:
1. **Build**: Runs `npm run build` to generate static files in `dist/`.
2. **Serve**: Uses the `serve` package to host the static files on port 3000.

```dockerfile
FROM node:20-slim
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
RUN npm install -g serve
EXPOSE 3000
CMD ["serve", "-s", "dist", "-l", "3000"]
```

## 3. Environment Variables

While the React app is purely static, you might want to inject environment variables for API endpoints during the build part:

| Variable | Description |
| :--- | :--- |
| `VITE_API_BASE_URL` | The URL of your backend (e.g., the FastAPI stack). |

**Note:** In Coolify, ensure these are marked as **Build Variable** if you want them injected during `npm run build`.

## 4. Deployment Steps

1. Select your GitHub repository and branch.
2. Set the Base Directory to `/repo-react`.
3. Coolify will automatically detect the `docker-compose.yml` and start the build.
4. Once completed, your React app will be available at your configured FQDN.

## 5. Optimization

For better performance in production:
- Use a **Static Site** build pack in Coolify instead of Docker if you want Coolify to handle the Nginx/Traefik serving directly without a Node.js process.
- If using the Docker approach, the `serve` command is lightweight but for high traffic, an Nginx-based Docker image is preferred.
