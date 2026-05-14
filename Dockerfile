# Build Stage
FROM node:22-slim AS builder
WORKDIR /app

# Set build-time environment variables
ENV NODE_ENV=production

# Install dependencies first for better caching
COPY package*.json ./
RUN npm install

# Copy configuration files
COPY vite.config.ts tsconfig.json tsconfig.node.json index.html ./

# Copy source code
COPY src ./src

# Build the frontend assets
RUN npm run build

# Production Stage
FROM node:22-slim
WORKDIR /app

# Install runtime dependencies
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy package files (needed for dependencies in Stage 2)
COPY package*.json ./

# Install only production dependencies
# tsx depends on its own esbuild, but having typescript in dev might be missed
# if tsx needs it. However, the user had it in dev.
# We'll install production deps.
RUN npm install --omit=dev

# Copy the built assets
COPY --from=builder /app/dist ./dist

# Copy the server and other runtime source files
# We run these with tsx directly
COPY server.ts database.ts metadata.json populate-tasks.ts ./
COPY repo-php ./repo-php

# Ensure tsx is available (should be in node_modules after npm install)
# But having a global fallback or local execution is safer
ENV PATH /app/node_modules/.bin:$PATH

# CMS Manager settings
ENV NODE_ENV=production
ENV PORT=3000

EXPOSE 3000

# Healthcheck to verify the server is responding
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:3000/api/sites || exit 1

# Start the application using tsx
CMD ["tsx", "server.ts"]
