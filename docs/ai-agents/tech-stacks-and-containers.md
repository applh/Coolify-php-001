# AI Studio Agents: Tech Stack Compatibility & Container Deployment Architectures

This architectural guide analyzes the technology runtimes available within **Google AI Studio Coding Agents**, addresses their capability to compile and deploy containers, and provides a comparative analysis of programming stacks. This helps developers optimize performance, optimize container footprint size, and ensure smooth AI Agent Experience (**AX**) during automated code-generation workflows.

---

## 🛠️ 1. Available Tech Stacks with AI Studio Agents

AI Studio and its associated development sandbox execute inside a secure, headless Cloud Run container. On the backend, this developer workspace comes pre-loaded with standard compilers, runtimes, package managers, and development utilities:

1.  **Node.js & npm (v20+)**: Supporting TypeScript compilation, JSX/Vite servers, and native Express/Next.js execution.
2.  **JS Engines & Utilities**: Native platforms like `tsx`, `esbuild`, and global formatting/linting CLI drivers.
3.  **PHP Engine (v8.2+)**: Combined with standard PDO extensions, SQLite/PostgreSQL drivers, and local Nginx or built-in test server routers.
4.  **Python & pip (v3.11+)**: Configured for lightweight backend microservices (e.g., FastAPI, Flask) and data processing routines.
5.  **Go Runtime (v1.22+)**: Allowing rapid compilation of static, high-frequency low-resource API servers.
6.  **Rust Cargo Platform (v1.78+)**: Equipped with the Rust compiler (`rustc`) for building strongly-typed, memory-safe system applications.
7.  **Android SDK & Gradle Tools**: For compiling native Kotlin/Compose packages (located in `repo-android`).
8.  **Flutter SDK**: Supporting visual mobile compilation pipelines for multi-platform applications (`repo-flutter`).

---

## 🐋 2. Can AI Studio Agents Deploy Containers?

A clear distinction must be made between **internal execution capabilities** and **external deployment orchestration**.

```
                        [ AI Studio Workspace Container ]
                                       │
            ┌──────────────────────────┴──────────────────────────┐
            ▼                                                     ▼
[ Internal Sandbox Restrictions ]                       [ External Deployment Pipelines ]
  - runs inside gVisor virtualization                     - writes and validates Dockerfiles
  - Docker-in-Docker blocked                              - pushes validated code to Git/CI
  - Cannot execute local "docker build"                   - triggers Coolify or Cloud Run Webhooks
```

### The Internal Sandbox Restriction (Why Local Builds Fail)
*   **Context**: The AI Studio developer sandbox is itself a containerized virtual machine (running inside Google's secure micro-VM layer, like **gVisor** or serverless Cloud Run).
*   **Limitation**: Running `docker build` or launching the local `docker-compose.yml` engines directly inside the agent's CLI is blocked because nested virtualization (Docker-in-Docker / DinD) requires privileged root capabilities that are restricted for security reasons.

### The External Container Deployment Strategy (How Agents Deploy)
Although agents cannot execute a docker command *locally* inside their prompt thread, they are fully capable of **orchestrating remote container deployments** to production environments. This is done through three primary configurations:

1.  **Coolify & VPS Hook Integration**:
    - Build environments like **Coolify** (configured in `/docs/devops/coolify-setup-node.md` and `/docs/devops/coolify-setup-php.md`) feature REST interfaces and Webhook receivers.
    - AI Studio agents can bundle the code, push to the remote repository, and execute an authenticated curl message to trigger a remote Docker rebuild and deployment sequence.
2.  **Serverless Cloud Run Pipelines**:
    - Utilizing Google Cloud Service Account JSON credentials stored securely in the AI Studio environment settings, the agent can call `gcloud run deploy --source` or push a compiled container to Google Artifact Registry (GAR) remotely.
3.  **CI/CD Pipeline Generation**:
    - The agent can write highly specific Github Actions workflows (`.github/workflows/deploy.yml`) that handle linting, running unit tests, compiling the Docker image, and pushing it to AWS ECS, GCP GKE, or digital ocean droplets.

---

## 📊 3. Comparative Matrix: Tech Stacks, Compiles, and Agent Preference

Different stacks have drastically different characteristics regarding memory footprint, compilation speeds, performance output, and **AI Agent Experience (AX)**.

| Technology Stack | Typical Docker Image Size (Optimized / Full) | RAM Footprint (Idle / Load) | Latency / Cold Start | Max Throughput (Reqs/Sec) | AX Preference Level & Rationale |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Go** | **12 MB** (Scratch) / 120 MB | 5 MB / 45 MB | ~15ms (Stellar) | >15,000 | **High AX**: Extremely readable, simple syntax, fast compilation, explicit error handling. Highly accessible for AI. |
| **Rust** | **18 MB** (Distroless) / 350 MB | 3 MB / 30 MB | ~10ms (Stellar) | >25,000 | **Medium-Low AX**: Long compilation times, complex lifetime/borrow rules mean AI can struggle with edge-case compiler locks. |
| **Node.js (TypeScript)** | **90 MB** (Distroless) / 450 MB | 45 MB / 180 MB | ~180ms | >6,000 | **High AX**: Beautiful typing system allows AI to self-correct using linters. Seamless JSON native processing. |
| **Python (FastAPI)** | **110 MB** (Alpine) / 850 MB | 30 MB / 150 MB | ~220ms | >4,500 | **High AX**: Clean structure matching native LLM logic. Rapid prototyping capability makes troubleshooting easy. |
| **PHP (Nginx + FPM)** | **140 MB** (Alpine) / 550 MB | 15 MB / 60 MB | ~150ms | >5,000 | **Medium AX**: Straightforward directory structure, but context fragmentation can happen due to mixed HTML/PHP templates. |
| **React / Vue (SPAs)** | **20 MB** (Static Nginx) / 600 MB | 5 MB / 20 MB | ~50ms (Static) | N/A (Client-Side) | **High AX**: Superior component isolation allows surgical agent edits without affecting surrounding parent elements. |
| **Android / Flutter** | **N/A** (Mobile Binary) / ~8 GB (Build environment) | 80 MB / 400 MB (Client device) | ~800ms (App Boot) | N/A | **Low AX**: Immense compilation overhead, complex layouts, device simulation dependencies limit inline preview loops. |

---

## 🔍 4. In-Depth Operational Strategy per Stack

### A. Go (High-Frequency Low-Resource Apps)
*   **Best For**: Lightweight microservices, dynamic rate-limiting proxies, and data synchronizers.
*   **Docker Strategy**: Use a multi-stage Dockerfile compiling Go inside `golang:alpine` and copying the single static binary into an empty `scratch` image. This reduces security exposure to zero.
*   **AX Benefit**: Go forces clean, straightforward logic blocks. Agents rarely struggle with complex AST structures while working in Go.

### B. Rust (Maximizing Resource Efficiency & Integrity)
*   **Best For**: Resource-dense algorithms, real-time sync systems, canvas rendering engines.
*   **Docker Strategy**: Use a cached multi-stage setup matching `cargo-chef` to segment build recipes from source changes, preserving layer cache. Copy the output binary to a minimally secure standard `distroless/cc` runtime.
*   **AX Benefit**: If an agent successfully writes Rust code that passes the compiler, it has a nearly 100% chance of executing perfectly in production. The compiler itself acts as a shield against regression visual bugs.

### C. Node.js with TypeScript (Dynamic Enterprise backends)
*   **Best For**: Rapid development of standard REST or GraphQL APIs, real-time collaboration engines, and full-stack middleware proxies.
*   **Docker Strategy**: Use Alpine or slim Node bases. Leverage standard dev-dependencies isolation so compiled results discard heavy build modules from the production package.
*   **AX Benefit**: The compiler and the ESLint engine provide a rich feedback channel that enables agents to perform autonomous self-healing cycles.

### D. Python with FastAPI (AI Processing Integrations)
*   **Best For**: Native language processing, scraping micro-services, and AI integrations.
*   **Docker Strategy**: Avoid full `python:latest` bases which can easily exceed 1GB. Use `python:3.11-slim` or `python:3.11-alpine` to maintain small images under 150MB.
*   **AX Benefit**: Pydantic's automatic validation allows the model to map exact data schema types with extreme confidence.

---

## 🎯 5. Architectural Recommendations for Multi-Repo Environments

1.  **Isolate Frontends & Backends**: Maintain the decoupling displayed in the current repository (Vue web, PHP backend, Rust, Go microservices) to ensure developers can run updates on a single module without reloading the entire system stack.
2.  **Combine Docker and Mock Frameworks**: Provide JSON datasets (e.g., `sites.json`) as local fallbacks so that developers and agents can test component logic locally even when database instances are sleeping or transient.
3.  **Focus on Multi-Stage Docker caching**: Always declare intermediate caching flags for dependencies (e.g., package.json or cargo.toml) ahead of source copy statements inside Dockerfiles to ensure rapid build times during production deployments.
