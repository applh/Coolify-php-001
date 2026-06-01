FROM node:20-slim

WORKDIR /app

# Install unzip utility
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
RUN npm install

COPY . .

# Hook for restoring codebases via binary compactions
RUN if [ -f archive.zip ]; then \
      echo "📦 [Vue] Found archive.zip! Extracting to restore precise file index..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ [Vue] No archive.zip found. Building with standard git synchronized files."; \
    fi

RUN npm run build

# Install a simple server to serve the static files
RUN npm install -g serve

EXPOSE 3000

CMD ["serve", "-s", "dist", "-l", "3000"]

