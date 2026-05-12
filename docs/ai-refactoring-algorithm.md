# AI Refactoring Algorithm for PHP CMS

This document details the step-by-step algorithm used by AI agents to refactor and optimize the PHP core and content files of the CMS.

## 1. Objective
Enable the AI to safely improve the codebase by identifying technical debt, enhancing security, and optimizing performance without breaking the custom MVC architecture.

## 2. The Refactoring Loop

The algorithm follows a strictly sequential process to ensure system stability.

### Step 1: Target Identification
- **Trigger**: A manual request from the Admin UI or an automated `code_audit` task.
- **Input**: A file path (e.g., `repo-php/class/Router.php`) and a refactoring goal (e.g., "Extract validation logic").

### Step 2: Context Gathering
The agent reads not just the target file, but its dependencies:
1. **Source Read**: Load the full content of the target file.
2. **Signature Analysis**: Identify classes/methods used within that file.
3. **Dependency Mapping**: Read `class/App.php` or relevant base classes (`Model.php`, `Controller.php`) to understand the inheritance hierarchy.

### Step 3: Prompt Construction (The "Refactor Blueprint")
The AI is provided with a multi-part prompt:
- **System Role**: Senior PHP Architect specialized in custom MVC frameworks.
- **Existing Code**: The source code gathered in Step 2.
- **Constraints**: 
    - Maintain compatibility with PHP 8.x.
    - Do not introduce external dependencies (e.g., Composer packages) unless explicitly allowed.
    - Preserve the `repo-php/content/` structure.
- **Goal**: Specific instruction (e.g., "Refactor this method into smaller, testable units").

### Step 4: Generation and Verification
- **Model Execution**: Gemini 1.5 Pro/Flash generates the new code.
- **Syntax Check (Simulation)**: The system performs a basic `php -l` (lint) check on the generated snippet if the environment allows, or uses a specific prompt to "Check for syntax errors".

### Step 5: Safety Diffing
Instead of overwriting the whole file, the system attempts a surgical replacement:
1. **Identify Chunk**: Locate the specific lines or methods to be replaced.
2. **Generate Patch**: Create a diff-like structure.
3. **Atomic Write**: Write the changes to a temporary file first, then swap them into the production directory.

### Step 6: Feedback Loop
- **Logging**: The refactoring task is marked as `completed` in `ai_tasks.json`.
- **Review**: The Admin UI displays the `before` and `after` code for human approval before the next heartbeat can pick up a related task.

## 3. Supported Refactoring Tasks

| Task Type | Description |
|-----------|-------------|
| `extract_method` | Breaks down long functions in Controllers or Models. |
| `security_harden` | Adds `htmlspecialchars` or SQL sanitization to legacy code blocks. |
| `performance_opt` | Replaces inefficient loops or redundant `file_get_contents` calls with cached versions. |
| `type_hinting` | Adds modern PHP type hints and return types to legacy signatures. |

## 4. Safety Guardrails
- **Immutable Files**: Certain files like `entrypoint.sh` or `nginx.conf` are marked as read-only for AI agents to prevent container-level failures.
- **Rollback JSON**: A backup of the file is created in `repo-php/backups/` before any AI-driven write occurs.
