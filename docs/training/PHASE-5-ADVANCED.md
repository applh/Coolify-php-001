# Phase 5: Advanced Engineering & AI (150 Hours)

Synthesize everything you've learned. Build modular systems and leverage AI to scale your output.

## Learning Objectives
- Plugin-based architectures.
- Dockerizing multi-service apps.
- Automating workflows with AI Agents.
- CMS performance tuning.

## Practical Labs

### 1. The Plugin System (50h)
**Reference**: `repo-php/class/PluginManager.php`
- **Task**: See how the SEO and Analytics plugins are loaded.
- **Exercise**: Build a "Maintenance Mode" plugin that intercepts all requests.

### 2. Containerization & Scaling (40h)
**Reference**: `docker-compose.yml` and `Dockerfile`
- **Task**: Compare the Dockerfiles of different languages.
- **Exercise**: Add a Redis container to the main `docker-compose.yml` for caching.

### 3. AI Agent Integration (60h)
**Reference**: `docs/ai-agents/`
- **Task**: Read the `ai-refactoring-algorithm.md`.
- **Exercise**: Use the AI to refactor `repo-php/class/Router.php` to support middle-wares.

### 4. Deployment & Coolify Mastery
**Reference**: `docs/training/slides/phase5/deployment-coolify.json`
- **Task**: Learn how to automate deployments using Coolify Webhooks and Node.js ZIP updates.
- **Exercise**: Simulate a "Self-Update" by using `adm-zip` logic (referencing `server.ts`) to programmatically update a site's `index.php` from a buffer.

### 5. Agile Roles & AI Transformation
**Reference**: `docs/training/slides/phase5/agile-ai-roles.json`
- **Task**: Understand how AI shifts core project roles (PO, SM, Developer).
- **Exercise**: Complete the labs in [Practical Business, Management & DevOps](./PRACTICAL-BUSINESS-DEVOPS.md#3-agile-roles-in-the-ai-era).

### 6. Git Sync & Version Control Automation (40h)
**Reference**: `docs/devops/git-sync-feature.md`
- **Goal**: Master automated version control workflows.
- **Reference**: `/docs/devops/git-sync-feature.md`
- **Exercise**: Set up a local Git server using Gitea or similar, then configure the Node.js backend to push updates automatically on every "Save" event in the Site Editor.
- **Complexity**: Phase 5

## Recommended Reading
- `docs/ai-agents/ai-coder-agent-guide.md`
- `docs/backend/php-plugin-system-guide.md`
