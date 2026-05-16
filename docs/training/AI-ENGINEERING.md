# Practical AI Engineering: Building with LLMs

Modern fullstack development now includes a "Fourth Layer": The AI Layer. This module teaches you how to integrate Large Language Models (LLMs) like Gemini into your applications.

## 1. LLM Fundamentals & Prompt Engineering
**Goal**: Learn to treat the model as a programmable component.

- **Lab 1: The System Instructions**
    - **Reference**: `AGENTS.md` and `docs/ai-agents/gemini-api-interactions.md`
    - **Task**: Study how the "Persona" of an agent is defined.
    - **Exercise**: Create a new `.md` file that defines a "Code Reviewer Agent" system prompt. Test it by pasting a function from `repo-php` and asking for a review.
- **Lab 2: Few-Shot Prompting**
    - **Task**: Understand how giving examples improves output consistency.
    - **Exercise**: Write a prompt for the Gemini API that converts a natural language request (e.g., "Add a contact form to my site") into a JSON config compatible with `repo-php/content/site1.com/forms/forms.json`.

## 2. API Integration & Tool Calling
**Goal**: Connect the "Brain" (AI) to the "Body" (your code).

- **Lab 1: Streaming Responses**
    - **Reference**: `src/views/AiMediaTasks.vue`
    - **Task**: Analyze how the UI handles real-time data updates.
    - **Exercise**: Implement a simple chat interface in `src/` that displays chunks of text as they are received from a simulated API stream.
- **Lab 2: Function Calling (Agentic Workflows)**
    - **Reference**: `docs/ai-agents/media-generation-toolchain.md`
    - **Task**: See how the AI identifies when it needs to call a tool (like `generate_image`).
    - **Exercise**: Map out a "Tool Definition" for the `CMS.php` class that would allow an AI to create a new blog post directly.
- **Lab 3: Parallel Orchestration**
    - **Reference**: `docs/training/slides/phase9/parallel-orchestration.json`
    - **Goal**: Understand how to execute multiple AI tool calls simultaneously to reduce latency.
    - **Task**: Review the theoretical benefits of parallel execution in AI Studio vs. traditional sequential loops.
    - **Exercise**: Create a mock implementation of a parallel tool call using `Promise.all` that triggers three separate 'file linting' tasks at once.

## 3. RAG (Retrieval Augmented Generation)
**Goal**: Give the AI access to private files and documentation.

- **Lab 1: Contextual Search**
    - **Task**: Use the `grep` tool to find all mentions of "Deployment" in `/docs`.
    - **Exercise**: Write a prompt that feeds the contents of `docs/devops/deployment.md` to the AI and asks it to generate a 5-step checklist for a new student.
- **Lab 2: Vector Embeddings (Concept)**
    - **Task**: Understand the difference between "Search" and "Semantic Meaning".
    - **Exercise**: Categorize all files in `repo-php/content/` based on their "Topic" (e.g., Wellness vs. Tech) and explain how an AI could use this to recommend related sites.

## 4. AI UI/UX Patterns
**Goal**: Design interfaces that handle AI uncertainty and latency.

- **Lab 1: The "Ghost" State**
    - **Task**: Observe the loading states in the dashboard.
    - **Exercise**: Create a "Skeleton Screen" component in Vue for the AI task list to improve perceived performance during long API calls.
- **Lab 2: Human-in-the-loop**
    - **Task**: Look at `src/views/SiteEditor.vue`.
    - **Exercise**: Add a "Verify & Apply" step where the AI suggests a change, but the user must click a button to execute the file edit.

## Complexity Levels
- **Phase 3 Integration**: Prompt Engineering & API Basics.
- **Phase 5 Integration**: Tool Calling & RAG Architecture.
