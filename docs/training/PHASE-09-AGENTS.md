# Phase 9: Intelligence, Agents & Gemini API (100 Hours)

Harness the power of Large Language Models to build autonomous coding and content agents.

## Modules (10 Hours Each)
- M081: Prompt Engineering Theory
- M082: Gemini API Integration
- M083: RAG (Retrieval Augmented Gen)
- M084: AI Media Generation
- M085: AI Coder Agent Logic
- M086: Auto-Refactoring Algorithms
- M087: Agent Task Queues
- M088: Function Calling & Tools
- M089: Fine-Tuning & Model Eval
- M090: Project: AI CMS Plugin

## Practical Labs

### 1. Gemini API Integration (40h)
**Reference**: `docs/ai-agents/gemini-api-interactions.md`
- **Task**: Trace an AI request from the UI to the SDK.
- **Exercise**: Create a custom prompt for generating site descriptions.

### 2. Coder Agent Refactoring (40h)
**Reference**: `docs/ai-agents/ai-refactoring-algorithm.md`
- **Task**: Study the agentic refactoring process.
- **Exercise**: Implement a simple agent task that renames variables in a PHP file.

### 3. Dynamic Curriculum Discovery (20h)
**Reference**: `src/views/TrainingCenter.vue`
- **Goal**: Learn how to implement file-system-driven dynamic UI components.
- **Task**: Analyze how `import.meta.glob` is used to index JSON modules without manual imports.
- **Exercise**: Add a new JSON file to `docs/training/slides/` and verify it appears automatically in the 'Atlas' view.
- **Complexity**: Phase 3 (Engineering Fundamentals)

### 4. High-Fidelity Slide Engineering (10h)
**Reference**: `AGENTS.md`, `docs/training/slides/phase1/module-intro.json`
- **Goal**: Master the constraints for generating production-ready training content with AI.
- **Task**: Review the 'Slide Generation Standards' in `AGENTS.md`.
- **Exercise**: Take an existing slide from a manifest (e.g., `docs/training/slides/phase1/module-intro.json`) and expand its content beyond 500 characters, adding a relevant SVG illustration, official links, and student prompts.
- **Goal**: Ensure training content provides deep value, visual grounding, and paths for further exploration.
- **Complexity**: Phase 1 (Documentation & AI Orchestration)

## Recommended Reading
- `docs/ai-agents/ai-coder-agent-guide.md`
- `docs/ai-agents/ai-agents-roles-tasks.md`

### 5. Agent Teams and Automations (10h)
**Reference**: `docs/ai-agents/agent-teams-implementation-plan.md`
- **Goal**: Learn to implement team-based AI agent task execution with cron automation.
- **Task**: Analyze the `AgentTeamManager` and `Scheduler` classes in the PHP backend.
- **Exercise**: Create a new automated agent in `agent_teams.json` and set up a recurring cron task for it.
- **Complexity**: Phase 3
