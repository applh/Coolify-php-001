FROM rust:1.75-slim as builder
WORKDIR /usr/src/app

# Install unzip Utility
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

COPY . .

# Hook for restoring codebases via binary compactions
RUN if [ -f archive.zip ]; then \
      echo "📦 [Rust Builder] Found archive.zip! Extracting to restore precise file index..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ [Rust Builder] No archive.zip found. Proceeding with standard git synchronized files."; \
    fi

RUN cargo build --release

FROM debian:bookworm-slim
COPY --from=builder /usr/src/app/target/release/repo-rust /usr/local/bin/repo-rust
EXPOSE 8081
CMD ["repo-rust"]
