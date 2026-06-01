# Technical Implementation Plan: Git-Sync ZIP Compaction for Coolify Deploys

This document outlines the architecture, configuration, and execution guidelines for compiling multi-stack repositories in Google AI Studio into cohesive ZIP archives before synchronizing to GitHub. This workaround ensures 100% file delivery, correcting the known limitation where standard folder sync occasionally omits files, leading to broken Docker builds on Coolify.

---

## 🔍 1. Background & Core Problem Statement

Google AI Studio's built-in **Sync to GitHub** functionality is indispensable for continuous deployment but suffers from intermittent file omission during high-frequency sync steps or high-turn sessions. In multi-container, multi-stack configurations (like those deployed as Coolify applications via Docker Compose), a single missing file (such as a router module, a database migration script, or an entrypoint wrapper) will crash the builder or cause runtime failures.

### The Sovereign Workaround: Binary Compaction
Instead of relying on multi-file path sync pipelines, we package the source code of each distinct service division (`repo-*`) into a single, cohesive binary file: `archive.zip`. 
* **The Concept**: Since compressed ZIP archives are single files, they are reliably captured and committed by the AI Studio Git synchronizer without file omissions.
* **The Build Flow**: The downstream Docker builder pulls the sync state, detects the existence of `archive.zip`, extracts the content on-the-fly, cleans up the archive, and proceeds with standard compiling and compilation targets.

---

## 🛠️ 2. The Compaction Engine (`zip-repos.ts`)

A dedicated TypeScript script (`/zip-repos.ts`) has been provisioned at the coordinate root of the workspace. This engine operates via the custom npm script `"npm run zip-repos"`.

### Excludes & Filter Strategy
To prevent compiling multi-gigabyte files and leaking system configurations, the compactor strictly ignores:
1. **Dependencies**: `node_modules/`, `.venv/`, `venv/`, `.gradle/`
2. **Build Results**: `dist/`, `build/`, `out/`, `__pycache__/`
3. **Git Histories**: `.git/`, `.github/`
4. **Local Secrets**: `.env`, `.env.local`
5. **Recurse Guardians**: `archive.zip` itself is omitted to prevent recursive compression bloat.

---

## 📁 3. Tech-Stack Migration Recipes (Dockerfile Upgrades)

To integrate this workaround across the active workspace, update each stack's `Dockerfile` to automatically verify, unzip, and overlay files from `archive.zip` during the container image configuration phase.

### A. React & Vue Frontends (`repo-react/Dockerfile` & `repo-vue/Dockerfile`)
These stacks run under `node-slim` containers. Add the `unzip` package and extract the archive before executing `npm run build`:

```dockerfile
FROM node:20-slim
WORKDIR /app

# 1. Install standard unzip utility
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
RUN npm install

# 2. Copy the active workspace directories
COPY . .

# 3. Dynamic Unzipping Layer: Check, extract, and clear the archive
RUN if [ -f archive.zip ]; then \
      echo "📦 Found archive.zip! Unzipping workspace overlay..."; \
      unzip -o archive.zip && rm archive.zip; \
    else \
      echo "ℹ️ No archive.zip found. Using standard git synchronized files."; \
    fi

# 4. Standard compilation and serve execution
RUN npm run build
RUN npm install -g serve
EXPOSE 3000
CMD ["serve", "-s", "dist", "-l", "3000"]
```

### B. PHP Multisite Stack (`repo-php/Dockerfile`)
PHP containers already include system dependencies like `zip` and `unzip`. We can unpack `archive.zip` inside `repo-php/entrypoint.sh` or directly inside the `Dockerfile`:

```dockerfile
# Add directly to repo-php/Dockerfile after copying project files
COPY . .

RUN if [ -f archive.zip ]; then \
      unzip -o archive.zip && rm archive.zip; \
    fi
```

*Alternatively, unpack dynamically during container runtime inside `entrypoint.sh` to populate any persistence-mounted volumes immediately on boot.*

### C. Python API Stack (`repo-python/Dockerfile`)
The FastAPI python image can utilize the standard Python library to unzip files if manual tool extraction is restricted, or install `unzip`:

```dockerfile
FROM python:3.11-slim
WORKDIR /app

RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Dynamic Unzip Hook
RUN if [ -f archive.zip ]; then \
      unzip -o archive.zip && rm archive.zip; \
    fi

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### D. Go API Stack (`repo-go/Dockerfile`)
Multi-stage Go builds require extracting files in the builder stage before running `go build`:

```dockerfile
FROM golang:1.22-alpine AS builder
WORKDIR /app

# Alpine uses apk for package management
RUN apk add --no-cache unzip

COPY go.mod ./
# Add go.sum if it exists

COPY . .

# Extract the source before compilation
RUN if [ -f archive.zip ]; then \
      unzip -o archive.zip && rm archive.zip; \
    fi

RUN CGO_ENABLED=0 GOOS=linux go build -o main .

FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/main .
COPY --from=builder /app/*.html ./
CMD ["./main"]
```

---

## 💬 4. Manual Compaction: Instructions & Prompts

When orchestrating modifications manually, use this structural prompt pattern to instruct writing assistants to package the directories flawlessly.

### Reusable Agent Prompt Template
Copy and paste this exact instructions block into any chat session when you are preparing to sync your changes to GitHub:

```text
Please execute the repository compaction routine to package all services for sync.

### Key Instructions:
1. Run the terminal compilation command: `npm run zip-repos`
2. Wait for the node script to finish traversing 'repo-php', 'repo-react', 'repo-vue', 'repo-python', 'repo-go', 'repo-rust', and 'repo-android'.
3. Confirm that 'archive.zip' outputs exist in each modified partition.
4. Output the resulting file size details and compile metrics in a clean summary so I know the files are ready to sync.
```

---

## 🤖 5. Automated AI Agent Triggers Configuration

To guarantee that the AI coding agent automatically refreshes the `archive.zip` boundaries without requiring manual prompting, we configure persistent project rules within the workspace root instructions.

### Step 1: Append Automation Rules to `AGENTS.md`
Add the following directive under the **"Minimal Intervention & Scope Control"** section in the root file `/AGENTS.md`:

```markdown
## 15. Real-Time Repository Compaction Rules (ZIP Workaround)
Whenever the AI agent successfully edits, creates, or deletes any files contained inside a `repo-*` directory (e.g., modifying react source lines, adding go paths, or expanding python variables), the agent **MUST** execute the local packaging routine before finishing the turn:
- **Action**: Run the command: `npm run zip-repos`
- **Goal**: Recompile and sync-stage `archive.zip` inside modified repo folder scopes instantly.
- **Verification**: Ensure the command runs completely without syntax blocks. Do NOT skip this step; missing zips can break remote builds on Coolify.
```

### Step 2: Append Directive to `GEMINI.md`
Also place the corresponding target command in any specific coding instructions (`/GEMINI.md`) as a fallback rule:

```markdown
- **POST-EDITS HOOK**: If edits target repo folders, compile binaries automatically via "npm run zip-repos".
```

Now, the system engine reads these rules upon startup, enforcing compliance automatically across all coding cycles.

---

## 🎓 6. Verification and Training
A practical verification walkthrough has been integrated into the developer curricula.

*   **Training Lab**: Refer to `/docs/training/PART-08-DEVOPS.md` (Lab 4: "Git-Sync Zip Compaction Workaround & Coolify Deploys").
*   **Architectural Record**: Refer to `/docs/architecture-decisions.md` (ADR Section 20: "Git-Sync Compaction Workaround & Automated Multi-Stack ZIP Bundling").
