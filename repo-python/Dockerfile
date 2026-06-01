FROM python:3.11-slim

WORKDIR /app

# Install unzip utility
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Hook for restoring codebases via binary compactions
RUN if [ -f archive.zip ]; then \
      echo "📦 [Python] Found archive.zip! Extracting to restore precise file index..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ [Python] No archive.zip found. Proceeding with standard git synchronized files."; \
    fi

EXPOSE 3000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "3000"]
