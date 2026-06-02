# Part 8: Cloud, DevOps & Coolify Orchestration (100 Hours)

Automate everything: from containerization to full-scale cloud orchestration.

## Modules (10 Hours Each)
- M071: Docker & Containerization
- M072: Docker Compose Multi-Stack
- M073: Coolify Setup & Config
- M074: CI/CD Pipelines
- M075: Cloud Providers (AWS/GCP)
- M076: Monitoring & Observability
- M077: Scaling Strategies
- M078: Infrastructure as Code
- M079: Coolify Node Scaling
- M080: Project: Automated Deployment

## Practical Labs

### 1. Docker Multi-Stack (40h)
**Reference**: `docker-compose.yml`
- **Task**: Study how PHP, Go, and Python services are networked.
- **Exercise**: Create a new containerized service and add it to the network.

### 2. Coolify Automation (40h)
**Reference**: `docs/devops/rebuild-guide.md`
- **Task**: Learn how webhooks trigger deployments.
- **Exercise**: Simulate a webhook deployment trigger using `curl`.

### 3. Automated Git Version Tagging & Backup Fallback (20h)
- **Goal**: Understand version release tracking using real Git annotated tags alongside dual-storage SQLite database persistence.
- **Reference**: `server.ts`, `src/views/SyncState.vue`, `/database.ts`
- **Exercise**: Create a new version tag `v1.5.0` via the frontend interface and verify its presence in both the underlying SQLite `version_tags` table and the real Git workspace indexes. Extend the system to support comparing codebase diffs between two tags.
- **Complexity**: Part 8 (Medium-High)

### 4. Git-Sync Zip Compaction Workaround & Coolify Deploys (4h)
- **Goal**: Learn to package and optimize nested codebases using zip binary files to guarantee file synchronization state transitions on high-frequency cloud sync limits.
- **Reference**: `/zip-repos.ts`, `/package.json`, `docs/ai-agents/ai-studio-agent-archive-upload-plan.md`
- **Exercise**: Execute the compiler command `npm run zip-repos` to compress all modular repo subdivisions into localized `archive.zip` streams. Extend the system, updating the `repo-react/Dockerfile` to automatically verify, unzip, and extract the bundle safely inside the Docker builder space during build.
- **Complexity**: Part 8 (High)

### 5. Custom Zero-Dependency Android Compile Build Server (12h)
- **Goal**: Understand how to implement a fully standalone, zero-dependency Node.js orchestration container to host and build Android Kotlin applications on memory-capped VPS environments via Coolify.
- **Reference**: `/repo-android/server.js`, `/repo-android/Dockerfile`, `/repo-android/package.json`
- **Exercise**: Launch the `repo-android` container on port 3000, trigger a compilation build, and examine the line-by-line stream outputs received via Server-Sent Events (SSE). Extend the `server.js` script to parse intermediate warning percentages from the Gradle output and visually render them as yellow telemetry lines.
- **Complexity**: Part 8 (High)

## Recommended Reading
- `docs/devops/deployment.md`
- `docs/devops/coolify-node-scaling.md`
