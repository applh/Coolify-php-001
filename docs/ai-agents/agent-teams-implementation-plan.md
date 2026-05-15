# AI Agent Teams Implementation Plan

This plan outlines the architecture and steps to extend the AI Agent system to support team management, cron task scheduling, specialized training slide generation, and sync capabilities.

## 1. Teams and Roles Management

### Data Modeling (JSON Manifest)
We will introduce `agent_teams.json` to define teams and their agents.

```json
{
  "teams": [
    {
      "id": "training-generator-team",
      "name": "Training Content Team",
      "agents": [
        {
          "id": "slide-architect",
          "role": "Curriculum Designer",
          "skills": ["slide-content-structuring", "pedagogical-design"],
          "commonSkills": ["gemini-interaction"]
        },
        {
          "id": "svg-illustrator",
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
- Update `AIAgent` to support loading team configurations.
- Introduce `AgentTeamManager` class to handle agent selection based on role/skills.

## 2. Cron Task Management

- Extend `AITaskManager` to allow defining `schedule` (cron syntax) for tasks.
- Create a `Scheduler` class in PHP that is checked via `/api/heartbeat`.
- Tasks will include a `is_recurring` flag and `cron_expression`.

## 3. Specialized Training Slide Generation Team

- Implement the `training-generator-team` defined above.
- Create specific agent tasks for each agent in the team:
    - `generate_slide_outline` (Slide Architect)
    - `generate_slide_svg` (SVG Illustrator)

## 4. Sync Feature (AI Studio Preview <-> Prod Sync)

- Implement a sync endpoint/command that:
    1.  Fetches updated training content from the production training center.
    2.  Pushes pending changes from AI Studio preview to production.
- Use a secure token-based authentication for the sync process between endpoints.

---
## Practical Lab: Phase 9 (Teams & Automations)
Goal: Learn to implement team-based AI agent task execution.
Reference: `/docs/ai-agents/agent-teams-implementation-plan.md`
Exercise: Implement the `AgentTeamManager` and a basic recurring task.
Complexity: Phase 3
