# Multi-Stack Expansion Implementation Plan

This document outlines the goal, setup, and workflow for the four new repositories: **Go, Rust, Flutter, and Android**.

## 🎯 Objectives
1. **Diversity**: Showcase polyglot architecture capable of handling different workload types (Compute, Safety, UI, Mobile).
2. **Coolify Readiness**: Each stack is independent and ready for "One-Click Deployment" using Docker Compose.
3. **CI/CD Integration**: The repositories include build environments designed for automated artifact generation.

## 📁 Repository Overview

| Repo | Tech Stack | Purpose | Deployment Strategy |
| :--- | :--- | :--- | :--- |
| `repo-go` | Go 1.21 | High-speed API / Microservices | Alpine-based Docker |
| `repo-rust` | Rust (Actix-web) | Safety-critical backend | Debian-slim Docker |
| `repo-flutter` | Flutter Web | Responsive Frontend | Nginx-served Web Build |
| `repo-android` | Android/Gradle | Native Mobile App | Dockerized Build Agent |

---

## 🛠️ Setup Instructions for Coolify

### 1. Connecting the Repositories
Since these are sub-folders in a monorepo, in Coolify you should:
- Create a new **Resource** -> **Source Code**.
- Point to your Github repo.
- Set the **Base Directory** to the specific repo folder (e.g., `./repo-go`).

### 2. Docker Compose Configuration
Each repository contains its own `docker-compose.yml`. Coolify will automatically detect these.
- Ensure the **Public Port** in Coolify settings matches the `ports` mapping in the `docker-compose.yml`.

### 3. Special Case: Android
`repo-android` is configured as a **Build Agent**. 
- In Coolify, you can set this up as a "Build" service that outputs the `.apk` file to a shared volume.
- Use the provided `Dockerfile` to ensure all Android SDK licenses and dependencies are pre-installed.

---

## 🔄 Development Workflow

1. **Local Development**:
   - Use the individual `docker-compose up` commands within each directory to test logic locally.
2. **AI Studio Iteration**:
   - Prompt the AI to modify specific domains. Example: *"Add a JSON logging middleware to the Go service in repo-go."*
3. **Automated Deployment**:
   - Pushing to the `main` branch will trigger Coolify to rebuild only the affected sub-directories.

---

## 📈 Scalability Considerations
- **Go/Rust**: Both are highly memory-efficient. You can set strict resource limits (e.g., 64MB RAM) in Coolify to maximize server density.
- **Flutter**: The multi-stage build ensures that the final image contains only the static files and the Nginx binary, keeping the production footprint minimal (<20MB).

*Created to expand the architectural capabilities of the Universal CMS Manager.*
