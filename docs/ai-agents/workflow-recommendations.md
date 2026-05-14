# AI Studio Workflow & Model Recommendations

To maximize productivity in Google AI Studio, it is critical to match the **Task Type** with the appropriate **Model** and **Agent Persona**.

## 🚀 Development Timeline & Model Selection

| Phase | Model Recommendation | Agent Role | Priority |
| :--- | :--- | :--- | :--- |
| **1. Brainstorming & Architecture** | **Gemini 1.5 Pro** | Product Architect | Deep Reasoning |
| **2. Scaffolding & Initial Code** | **Gemini 1.5 Flash** | Rapid Developer | Speed / Cost |
| **3. Complex Logic & Debugging** | **Gemini 1.5 Pro** | Senior Debugger | Accuracy |
| **4. Documentation & Cleanup** | **Gemini 1.5 Flash** | Technical Writer | Conciseness |
| **5. Large Scale Refactoring** | **Gemini 1.5 Pro** | Systems Engineer | Context Window |

---

## 🛠️ Scenarios & Prompt Examples

### Scenario A: Architectural Planning
- **Model:** 1.5 Pro
- **Role:** Product Architect
- **Goal:** Define the interaction between the PHP engine and a new Go microservice.
- **Prompt Example:**
> "I want to add a `repo-go` that handles high-speed image compression. Design the API contract between the existing Node.js `server.ts` and the new Go service. How should the filesystem be shared to avoid race conditions?"

### Scenario B: Rapid Feature Implementation
- **Model:** 1.5 Flash
- **Role:** UI Developer
- **Goal:** Create a simple button component in the Vue dashboard.
- **Prompt Example:**
> "Add a 'Sync All' button to the `SiteDashboard.vue` header. Use Tailwind blue-600 and include a spinning icon during the sync state."

### Scenario C: Deep Debugging
- **Model:** 1.5 Pro
- **Role:** Security/Systems Engineer
- **Goal:** Fix a 'Permission Denied' error when the Python API tries to write to the PHP content folder.
- **Prompt Example:**
> "Analyze the Docker Compose volumes and the Python write permissions in `main.py`. The Python service is failing to create files in `repo-php/content/`. Trace the UID/GID mapping across the containers and provide a fix."

### Scenario D: Performance Benchmarking
- **Model:** 1.5 Pro
- **Role:** Performance Engineer
- **Goal:** Optimize the current `SiteBenchmarker.vue` logic.
- **Prompt Example:**
> "Review the current D3.js implementation in `SiteBenchmarker.vue`. We are seeing lag with 50+ sites. Rewrite the rendering loop to use canvas or optimized SVG groupings."

---

## 💡 Pro-Tips for AI Studio

1. **Context Management:** If the project gets extremely large, use the "File Selection" feature in AI Studio to only include relevant sub-repos (e.g., just `repo-php` and `server.ts`) when performing targeted edits.
2. **Turn-Based Iteration:** Don't ask for 10 things at once. Use a "Plan -> Execute -> Verify" cycle.
3. **Drafting Rules:** Use the `AGENTS.md` file to persist your "Agent Role" preferences so the AI consistently adopts the correct persona.
