# AI Coder Agent Guide

The "AI Coder Agent" (or AI Architect) is the specialized persona designed to handle complex architectural changes, multi-file refactoring, and new feature implementation across our hybrid Node/PHP environment.

## 1. Capabilities

- **Context-Aware Refactoring**: Identifies patterns and suggests structural changes that maintain consistency between the Node.js dashboard and the PHP runtime.
- **Multi-File Orchestration**: Coordinated updates across multiple files to ensure that interface or architectural changes are applied atomically.
- **Architectural Guidance**: Recommends best practices for directory structure, class inheritance, and plugin implementation.

## 2. Usage Workflow

The AI Architect follows a structured process to ensure stability:

1.  **Analysis**: The agent scans the current file tree and detects relevant dependencies.
2.  **Proposal**: It generates a detailed Markdown description of the proposed changes, explaining the rationale and identifying all affected files.
3.  **Refinement**: The user can provide feedback or request adjustments before implementation.
4.  **Implementation**: The agent applies the changes using atomic file operations.

## 3. Integration with Gemini 1.5 Pro

### 4.1 Large Context Handling
To effectively scan the code, the system will use **Gemini 1.5 Pro**. Its high token limit allows for:
- Ingesting the entire `repo-php/class/` directory (approx. 20-50KB).
- Retaining memory of the architectural guidelines throughout the session.

### 4.2 File System Interaction
A new class `RepositoryScanner` will be implemented:
- `scan($path)`: Returns a map of `filePath => content`.
- `filter($extensions)`: Limits context to `.php`, `.js`, `.css` files.

### 4.3 Code Searching & Discovery
For large repositories where even 1.5 Pro might struggle with the sheer volume of assets, the Agent Coder uses a tiered discovery strategy:
1. **Grep-based Search**: The system pre-scans for keywords (e.g., "SQL", "Auth", "Session") to determine which files are relevant to a specific task.
2. **Namespace Mapping**: Maps out the PHP `class` and `use` statements to build a dependency graph.
3. **Similarity Search (Optional)**: In larger implementations, use vector embeddings to find code blocks similar to the target refactoring goal.

## 5. User Interface (Admin Dashboard)

The CMS Admin panel will feature an "AI Architect" section:
- **Scan Window**: Shows which files the AI is currently "looking at".
- **Proposal Viewer**: A side-by-side view (Diff) of all files affected by the proposed refactoring.
- **Apply/Reject Buttons**: Final human-in-the-loop gate before any code changes are written to disk.

## 6. Safety and Guardrails

1. **Git-like Backups**: Before applying a multi-file change, the system creates a timestamped zip backup of `repo-php/`.
2. **No-Fly Zone**: The agent is explicitly forbidden from editing `.env`, `docker-compose.yml`, or the `public/index.php` entry point without explicit admin override.
3. **Execution Limit**: Multi-file refactorings are limited to 10 files per task to keep complexity manageable.
