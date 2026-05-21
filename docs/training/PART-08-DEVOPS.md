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

## Recommended Reading
- `docs/devops/deployment.md`
- `docs/devops/coolify-node-scaling.md`
