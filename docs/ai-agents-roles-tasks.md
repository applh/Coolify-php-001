# AI Agents: Roles and Tasks

This document describes how AI agents are utilized within the CMS ecosystem to automate tasks, improve content, and assist in software engineering processes.

## 1. Agent Personas (Roles)

The system uses specialized personas to ensure high-quality output based on context.

### 1.1 The Content Strategist
- **Goal**: Improve readability, engagement, and tone consistency.
- **Tasks**:
    - Rewriting headlines for better conversion.
    - Refining body text for professional or playful tones.
    - Summarizing long-form content.

### 1.2 The SEO Specialist
- **Goal**: Optimize pages for search engine visibility.
- **Tasks**:
    - Generating descriptive `<title>` and `<meta name="description">` tags.
    - Suggesting semantic heading structures.
    - Identifying missing alt text for images.

### 1.3 The Software Engineer Assistant
- **Goal**: Assist in site structure and code-level improvements.
- **Tasks**:
    - Analyzing HTML for accessibility (WCAG) issues.
    - Suggesting performance optimizations (e.g., identifies large assets).
    - Generating boilerplate configuration or site structures.

### 1.4 The Agent Coder
- **Goal**: Perform complex, multi-file refactoring and feature implementation.
- **Tasks**:
    - Scanning existing class hierarchies to propose improvements.
    - Extracting reusable logic from controllers into service classes.
    - Creating new files and updating dependencies automatically.
    - Implementing cross-cutting concerns (e.g., logging, auth) across several files.

## 2. Task Ecosystem

Tasks are units of work executed by the AI agents via the `AIAgent` class.

### 2.1 Content Tasks
- `improve_text`: Takes existing text and a context (e.g., "hero section") and returns improved copy.
- `translate_content`: Translates sections to target languages while maintaining formatting.

### 2.2 Structural Tasks
- `generate_meta`: Analyzes the primary content of a page to produce SEO metadata.
- `site_audit`: Performs a high-level review of a site's `index.php` and suggests UX/UI improvements.

## 3. Implementation Workflow

1. **Definition**: A task is defined in the CMS (e.g., "Improve the 'About Us' section").
2. **Queueing**: The task is stored in `ai_tasks.json` with a `pending` status.
3. **Trigger**: The `/api/heartbeat` endpoint is called (manually or via scheduler).
4. **Execution**:
    - The `AIAgent` identifies the role required.
    - A specific prompt is constructed using the `GEMINI_API_KEY`.
    - Gemini processes the request and returns a result.
5. **Persistence**: The result is saved back to the task record.
6. **Application**: The user reviews the AI's suggestion and applies it to the site files via the CMS dashboard.

## 4. Software Engineering Integration

AI agents are integrated into the dev lifecycle:
- **Auto-Fixes**: In future iterations, agents could automatically commit small SEO fixes or text improvements directly to the site's directory.
- **Quality Gates**: Running a `site_audit` task before "deploying" or "syncing" a site ensures a baseline of quality.
