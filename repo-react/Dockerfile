FROM node:20-slim

WORKDIR /app

# Install unzip utility
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
RUN npm install

COPY . .

# Hook for restoring codebases via binary compactions
RUN if [ -f archive.zip ] && unzip -tq archive.zip >/dev/null 2>&1; then \
      echo "📦 [React] Found valid archive.zip! Extracting to restore precise file index..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ [React] No valid archive.zip found. Building with standard git synchronized files."; \
      rm -f archive.zip; \
    fi

RUN npm run build

# Install a simple server to serve the static files
RUN npm install -g serve

EXPOSE 3000

CMD ["serve", "-s", "dist", "-l", "3000"]

