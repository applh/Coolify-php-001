# Part 9: Intelligence, Agents & Gemini API (100 Hours)

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
- **Complexity**: Part 3 (Engineering Fundamentals)

### 4. High-Fidelity Slide Engineering (10h)
**Reference**: `AGENTS.md`, `docs/training/slides/part1/module-intro.json`
- **Goal**: Master the constraints for generating production-ready training content with AI.
- **Task**: Review the 'Slide Generation Standards' in `AGENTS.md`.
- **Exercise**: Take an existing slide from a manifest (e.g., `docs/training/slides/part1/module-intro.json`) and expand its content beyond 500 characters, adding a relevant SVG illustration, official links, and student prompts.
- **Goal**: Ensure training content provides deep value, visual grounding, and paths for further exploration.
- **Complexity**: Part 1 (Documentation & AI Orchestration)

## Recommended Reading
- `docs/ai-agents/ai-coder-agent-guide.md`
- `docs/ai-agents/ai-agents-roles-tasks.md`

### 5. Agent Teams and Automations (10h)
**Reference**: `docs/ai-agents/agent-teams-implementation-plan.md`
- **Goal**: Learn to implement team-based AI agent task execution with cron automation.
- **Task**: Analyze the `AgentTeamManager` and `Scheduler` classes in the PHP backend.
- **Exercise**: Create a new automated agent in `agent_teams.json` and set up a recurring cron task for it.
- **Complexity**: Part 3

### 6. Agent Performance & Evaluation (10h)
**Reference**: `docs/evaluation-report.md`, `src/views/SiteBenchmarker.vue`
- **Goal**: Implement quality gates and evaluation metrics for agentic workflows.
- **Task**: Analyze how the `SiteBenchmarker` component evaluates site performance and UX.
- **Exercise**: Add a custom 'Success Metric' for an AI task that verifies if the generated HTML is WCAG compliant.
- **Complexity**: Part 4 (Component Systems)

### 7. Parallel Orchestration & Concurrency (10h)
**Reference**: `docs/training/slides/part9/parallel-orchestration.json`, `docs/ai-agents/gemini-api-interactions.md`
- **Goal**: Master the implementation of parallel tool calling in AI Studio.
- **Task**: Review the theoretical model of parallel orchestration in the slides.
- **Exercise**: Implement a script that uses the Gemini SDK to call two different tools (e.g., `list_dir` and `read_file`) in a single turn using parallel function calling.
- **Complexity**: Part 5 (Backend Mastery)

### 8. API Discovery & Model Resilience (10h)
**Reference**: `AGENTS.md`, `docs/ai-agents/gemini-api-interactions.md`, `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`
- **Goal**: Master model resilience and dynamic service discovery to avoid model loop crashes upon API updates.
- **Task**: Review model discovery endpoint protocols (`v1beta/models`) to dynamically inspect environment capabilities.
- **Exercise**: Implement a lightweight retry or lookup utility that queries valid active models when a `404 NOT_FOUND` is caught, logging valid candidates to the user.
- **Complexity**: Part 3

### 9. Multi-Applet Complexity Mapping & Safe Prompting (10h)
**Reference**: `repo-android/docs/ai-agent-applet-complexity-guide.md`
- **Goal**: Understand the varying complexities of Android Jetpack Compose and background tasks under AI-agent modifications, learning how to prompt specifically to resolve hardware/lifecycle dependencies securely.
- **Task**: Analyze the failure modes and safety blueprints mapped for Tier 3 and Tier 5 applets.
- **Exercise**: Choose an applet from the complexity scaling (e.g., Camera or Backup Manager) and write a mock prompt that incorporates the layout, threading, and lifecycle directives, proving how context constraints prevent typical regression states.
- **Complexity**: Part 1 (Documentation & AI Orchestration)

### 10. Sovereign ZIP Shipper & Agent Tools (10h)
**Reference**: `/docs/ai-agents/ai-studio-agent-archive-upload-plan.md`, `/archive-uploader.ts`
- **Goal**: Learn how to design custom single-purpose agents that bundle workspace files and integrate with external APIs securely using environment variables.
- **Task**: Review the system prompt and code design for the ZIP packager utility.
- **Exercise**: Adapt the `/archive-uploader.ts` script to support standard `multipart/form-data` upload using Node's native `fetch` FormData representation, and verify that it handles dynamic network retry delays when encountering temporary HTTP 503 states.
- **Complexity**: Part 3 (Engineering Fundamentals)

### 11. AI Studio Agent Blueprints & Use Case Design (10h)
**Reference**: `docs/ai-agents/useful-agent-use-cases.md`, `docs/ai-agents/google-ai-studio-experience.md`
- **Goal**: Master the design and implementation of diverse high-value agent roles within Google AI Studio.
- **Task**: Analyze the six core agent blueprints (ZIP Shipping, UI Auditing, Polyglot Bridging, Self-Correction, Localization, and Schema Syncing).
- **Exercise**: Create a detailed custom system prompt for the "Playwright/Puppeteer UI Auditor" agent that instructs the model to compile a list of visual bugs (e.g., overlapping text blocks, lack of screen margins, low color contrast) and draft actionable fix suggestions.
- **Complexity**: Part 2 (Visual Design & UI/UX Principles)


