# Evaluation: Cross-Platform Synchronization Strategies

This document evaluates solutions to keep the AI Studio Preview (Source of Truth) in sync with a persistent VPS (Coolify/Docker) and multiple client platforms (Android, Flutter, etc.).

## 1. Current State
- **AI Studio**: transient environment, primary development hub.
- **VPS**: persistent production environment.
- **Mechanism**: Manual ZIP export/import via `SyncState.vue`.
- **Limitation**: Highly manual; risk of data drift; code/data split is not automated.

---

## 2. Solution Options

### Option A: Manual ZIP Sync (Current)
- **Workflow**: Download ZIP from AI Studio -> Upload to VPS.
- **Pros**: Zero infrastructure cost; simple.
- **Cons**: Slow; prone to user error; not "live".

### Option B: Git-Backed Data Persistence
- **Workflow**: AI Studio commits `cms.db` and media to Git -> VPS pulls Git.
- **Pros**: Standardized; history tracking.
- **Cons**: SQLite DBs in Git cause merge conflicts; Git becomes bloated with media (.zip files are used to mitigate this but still not ideal).

### Option C: Remote Push (Recommended)
- **Workflow**: AI Studio CMS has a "Push to Production" button. It calls the VPS API directly with an authenticated ZIP payload.
- **Pros**: One-click sync; handles both DB and code files; no manual download/upload.
- **Cons**: Requires the VPS to be publicly reachable; requires API authentication.

### Option D: External Managed Database (Firestore/Cloud SQL)
- **Workflow**: Both environments point to a central cloud database.
- **Pros**: Real-time sync; no manual action.
- **Cons**: "Costly" as per user request; complex networking (opening SQLite or MySQL across the web).

---

## 3. Recommended Implementation: "Remote Push Agent"

To keep every platform in sync without high costs:

1. **Code (Git)**: Push from AI Studio to GitHub -> Coolify Auto-deploy.
2. **Data (API Push)**: 
    - `src/views/SyncState.vue` will be updated to allow entering a `Remote Host` and `API Key`.
    - A new `POST /api/sync/push` endpoint on `server.ts` will bundle the local state and send it to the remote VPS.
    - The `POST /api/sync/import` endpoint will be hardened with a `SECRET_KEY` check.
3. **Client Platforms (Multi-Repo)**:
    - Android/Flutter apps should point their `BASE_URL` to the **VPS instance** (persistent) rather than the AI Studio preview (transient).

## 4. Next Steps
1. Add `SYNC_SECRET_KEY` to `.env.example`.
2. Update `server.ts` to support authenticated imports.
3. Update `SyncState.vue` to support the "Push to Production" workflow.
4. Document the "Cloud-First Sync" architecture for the AI Agents.
