# Node.js + Vue (Root App) Agent Teams Implementation Plan

This plan outlines how to replicate the PHP-based AI Agent Teams, Cron Management, and Sync features natively inside the root Node.js/Vue application architecture (`server.ts`).

## 1. Teams and Roles Management (Node.js)

### Data Modeling
Create a JSON manifest `data/agent_teams.json` to define the AI teams.

```json
{
  "teams": [
    {
      "id": "training-generator-team",
      "name": "Node Training Content Team",
      "agents": [
        {
          "id": "node-slide-architect",
          "role": "Curriculum Designer",
          "skills": ["slide-content-structuring", "pedagogical-design"],
          "commonSkills": ["gemini-interaction"]
        },
        {
          "id": "node-svg-illustrator",
          "role": "Visual Designer",
          "skills": ["svg-generation", "illustration-styling"],
          "commonSkills": ["gemini-interaction"]
        }
      ]
    }
  ]
}
```

### Architecture
- Create a `src/server/agentQueue.ts` (or within `server.ts`) to parse `agent_teams.json`.
- Expose the team's information to the Vue frontend via a `/api/agents/teams` GET endpoint.

## 2. Cron Tasks Management in `server.ts`

- Add SQLite columns to `media_tasks` (or a separate `ai_tasks` table) for `is_recurring`, `cron_expression`, and `last_run_at`.
- Install the `node-cron` package (`npm install node-cron`) to handle scheduled tasks cleanly.
- Keep an in-memory map or array in `server.ts` of active cron jobs. When the app starts, query all recurring tasks from the database and call `cron.schedule(expression, callback)` for each.
- Add endpoints like `/api/tasks/schedule` to let the UI create or cancel tasks, dynamically updating the active `node-cron` jobs.

## 3. Specialized Training Slide Generation Team via Gemini

- Import `@google/genai` (if not already used) inside `server.ts`.
- Set up execution handlers based on the tasked agent's role.
- **Slide Architect Tasks:** Send prompts requesting structural JSON for slides (`generate_slide_outline`).
- **SVG Illustrator Tasks:** Execute prompts that output only raw SVG strings (`generate_slide_svg`).
- Save the structured slide content into the `docs/training/slides/*` directory dynamically.

## 4. Sync Feature (AI Studio Preview <-> Prod Sync)

- The `server.ts` already implements base `sync` APIs (`/api/sync/export`, `/api/sync/import`, `/api/sync/push`).
- Create an endpoint `POST /api/sync/admin-sync` that orchestrates downloading the latest training content via the `/api/sync/push` and `/api/sync/import` logic and authenticates securely to coordinate between generic environments.
- In the Vue frontend (`TrainingCenter.vue`), make sure `syncWithProd()` calls the local `/api/sync/admin-sync` using standard absolute paths without relying on hardcoded localhost ports.

---
## Practical Lab: Phase 9 (Node.js Agents)
**Goal:** Migrate and operate AI Agent team routines strictly within a Node server.
**Reference:** `docs/ai-agents/node-vue-agent-teams-plan.md`
**Exercise:** Extend `server.ts` schema to support `cron_expression` and build the `node-cron` dispatcher.
**Complexity:** Phase 4
