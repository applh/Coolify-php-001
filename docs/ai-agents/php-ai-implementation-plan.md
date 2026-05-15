# AI Features Implementation Plan for PHP CMS

This plan outlines the integration of AI-powered features into the PHP-based CMS, enabling automated website improvements and media generation via the Gemini API.

## 1. Architecture Overview

The system will follow an asynchronous task-queue model:
- **Task Queue**: A JSON-based storage for pending and historical AI tasks.
- **AI Agent**: A PHP class that interfaces with the Google Gemini API.
- **Heartbeat Endpoint**: A specialized API route that triggers the execution of pending tasks.

## 2. Core Components

### 2.1 `AITaskManager` (class/AITaskManager.php)
- Responsibility: Manage the task lifecycle (creation, retrieval, updates).
- Storage: `/my-data/ai_tasks.json`.
- Methods:
    - `addTask($site, $type, $prompt)`
    - `getNextTask()`
    - `updateTask($id, $data)`
    - `getTasks($site = null)`

### 2.2 `AIAgent` (class/AIAgent.php)
- Responsibility: Execute prompts using the Gemini 1.5 Flash or Pro models.
- Authentication: Uses `GEMINI_API_KEY` environment variable.
- Features:
    - `generateText($prompt)`
    - `improveWebpage($html, $instruction)`
    - `refactorCode($files, $goal)`: (See `ai-coder-agent-guide.md` for details)

### 2.3 `Heartbeat Controller` (Integrated into logic)
- Route: `/api/heartbeat`
- Logic:
    1. Check for `GEMINI_API_KEY`.
    2. Check for pending tasks in `ai_tasks.json`.
    3. If a task exists, mark it as 'running'.
    4. Execute the task via `AIAgent`.
    5. Save result and mark as 'completed' (or 'failed').
    6. Return a summary of activity.

## 3. Workflow Example

1. **Trigger**: A user requests an "AI Improvement" for a specific page.
2. **Queueing**: The system adds a task to `ai_tasks.json` with status `pending`.
3. **Heartbeat**: A periodic background request hits `/api/heartbeat`.
4. **Execution**: The scheduler picks the task, sends the page content to Gemini, and receives an improved version.
5. **Application**: The result is saved, and the user can see/approve the changes in the CMS dashboard.

## 4. Security Considerations
- **API Key Safety**: The `GEMINI_API_KEY` is never exposed to the client.
- **Heartbeat Protection**: The heartbeat route should ideally be protected by the `APP_ADMIN_PASSKEY` or limited to specific IPs/local triggers.
- **Rate Limiting**: The heartbeat processes one task at a time to stay within API quotas and prevent server overload.

## 5. Next Steps
- Implement `AITaskManager` and `AIAgent` classes.
- Update `App.php` to handle the `/api/heartbeat` route.
- Implement the task processing logic.
- (Optional) Build a simple AI dashboard in the Vue/Admin UI.
