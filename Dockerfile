# Build Stage
FROM node:22 AS builder
WORKDIR /app

# Declare ARGs for build-time if needed (Vite can use them if prefixed with VITE_)
ARG GEMINI_API_KEY
ARG APP_ADMIN_PASSKEY
ARG PORT=3000

# Install dependencies first for better caching
COPY package*.json ./
RUN npm ci

# Copy configuration files and source code
COPY . .

# Build the frontend assets with memory limit
RUN node --max-old-space-size=1024 ./node_modules/vite/bin/vite.js build

# Production Stage
FROM node:22-slim
WORKDIR /app

# Declare ARGs again for the final stage (Coolify requirement)
ARG GEMINI_API_KEY
ARG APP_ADMIN_PASSKEY
ARG PORT=3000

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

# Create data directory and ensure it's writable
RUN mkdir -p /app/data && chmod 777 /app/data

# Copy the server and other runtime source files
# We run these with tsx directly
COPY server.ts database.ts scheduler.ts metadata.json populate-tasks.ts ./
COPY docs ./docs
COPY repo-android ./repo-android
COPY repo-flutter ./repo-flutter
COPY repo-go ./repo-go
COPY repo-php ./repo-php
COPY repo-python ./repo-python
COPY repo-react ./repo-react
COPY repo-rust ./repo-rust
COPY repo-vue ./repo-vue

# Ensure tsx is available (should be in node_modules after npm install)
# But having a global fallback or local execution is safer
ENV PATH /app/node_modules/.bin:$PATH

# FRAISE * AI Software Engineer settings
ENV NODE_ENV=production
ENV PORT=3000

EXPOSE 3000

# Healthcheck to verify the server is responding
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:3000/api/sites || exit 1

# Start the application using tsx
CMD ["node", "--import", "tsx", "server.ts"]
