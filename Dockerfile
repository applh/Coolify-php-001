# Use Node.js 22 as specified in package.json
FROM node:22-slim AS builder

WORKDIR /app

# Install build dependencies
COPY package*.json ./
RUN npm install

# Copy source and build the frontend assets
COPY . .
RUN npm run build

# Production stage
FROM node:22-slim

WORKDIR /app

# Install production dependencies only
COPY package*.json ./
RUN npm install --omit=dev

# Copy the built assets from the builder stage
COPY --from=builder /app/dist ./dist
# Copy the server and other necessary assets
COPY --from=builder /app/server.ts ./
COPY --from=builder /app/database.ts ./
COPY --from=builder /app/repo-php ./repo-php
COPY --from=builder /app/metadata.json ./
COPY --from=builder /app/populate-tasks.ts ./

# Install tsx globally or rely on local if in package.json
RUN npm install -g tsx

# Expose the app port
EXPOSE 3000

# Set production environment
ENV NODE_ENV=production
ENV PORT=3000

# Start the application
CMD ["tsx", "server.ts"]
