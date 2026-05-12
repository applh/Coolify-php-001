# AI Agent Coder: Implementation Plan

This document details the design and implementation of the "Agent Coder" - a specialized AI persona capable of complex refactoring, multi-file architectural changes, and new feature implementation within the PHP CMS.

## 1. Overview

The Agent Coder goes beyond simple text improvements. It behaves like a senior developer who understands the entire codebase, can identify patterns, and suggests structural changes that span multiple files.

## 2. Core Capabilities

- **Codebase Scanning**: Ingests multiple files to understand cross-file dependencies and inheritance.
- **Architectural Refactoring**: Proposes moving logic from Controllers to specialized Service classes or Models.
- **New File Creation**: Generates boilerplate and implementation for new classes (e.g., new Plugins, Middleware).
- **Multi-File Patching**: Provides a coordinated set of changes where an edit in one file (e.g., interface change) is reflected in all implementing classes.

## 3. Implementation Workflow

### Phase 1: Context Aggregation
The agent cannot work in a vacuum. The system must provide:
1. **Directory Tree**: A flattened list of all files in `repo-php/`.
2. **Core Dependencies**: Always includes `class/App.php`, `class/DB.php`, and `class/Router.php`.
3. **Target Module**: All files related to the specific feature being refactored (e.g., all files in `class/` or a specific site in `content/`).

### Phase 2: The "Analyze & Propose" Loop
Instead of direct execution, the Agent Coder follows a 2-step process:

1. **Analysis Task**: 
   - Input: The aggregated code context and a high-level goal (e.g., "Implement a plugin system").
   - Output: A **Refactoring Proposal** in Markdown format, explaining *what* will change and *why*.
2. **Implementation Task**:
   - Input: The approved proposal.
   - Output: A structured JSON object containing the operations:
     ```json
     {
       "operations": [
         {
           "type": "create",
           "path": "repo-php/class/PluginInterface.php",
           "content": "<?php ..."
         },
         {
           "type": "edit",
           "path": "repo-php/class/App.php",
           "instructions": "Locate the loadPlugins method and replace it with...",
           "original_code_block": "...",
           "new_code_block": "..."
         }
       ]
     }
     ```

### Phase 3: Validation and Execution
- **Dry Run**: Use a temporary directory to write the proposed files.
- **Linting**: Run `php -l` on all new or modified files.
- **Atomic Commit**: If all files pass linting, the changes are moved to the production directory.

## 4. Technical Requirements

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
