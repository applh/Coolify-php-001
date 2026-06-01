FROM golang:1.21-alpine AS builder
WORKDIR /app

# Install unzip
RUN apk add --no-cache unzip

COPY . .

# Hook for restoring codebases via binary compactions
RUN if [ -f archive.zip ]; then \
      echo "📦 [Go Builder] Found archive.zip! Extracting to restore precise file index..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ [Go Builder] No archive.zip found. Proceeding with standard git synchronized files."; \
    fi

RUN go build -o main main.go

FROM alpine:latest
WORKDIR /root/
COPY --from=builder /app/main .
EXPOSE 8080
CMD ["./main"]
