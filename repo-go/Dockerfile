FROM golang:1.21-alpine AS builder
WORKDIR /app

# Install unzip
RUN apk add --no-cache unzip

COPY . .

# Hook for restoring codebases via binary compactions
RUN if [ -f archive.zip ] && unzip -tq archive.zip >/dev/null 2>&1; then \
      echo "📦 [Go Builder] Found valid archive.zip! Extracting to restore precise file index..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ [Go Builder] No valid archive.zip found. Proceeding with standard git synchronized files."; \
      rm -f archive.zip; \
    fi

RUN go build -o main main.go

FROM alpine:latest
WORKDIR /root/
COPY --from=builder /app/main .
EXPOSE 8080
CMD ["./main"]
