# Feasibility Evaluation: Universal Polyglot CMS & 1000-Hour Training

## 1. Project Roadmap Evaluation

### 1.1 Handle Multiple Repo Stacks
**Status: ✅ FULLY CAPABLE**
AI Studio's runtime environment is containerized (Docker-based) and supports multi-language execution. The current project structure already encompasses:
- **Backend**: PHP, Python, Go, Rust.
- **Frontend**: React, Vue, Vanilla JS.
- **Mobile**: Flutter, Android (Kotlin).
- **Orchestration**: Docker Compose, Nginx.

AI agents can navigate these folders, recognize language-specific patterns, and perform cross-stack migrations (e.g., converting a PHP service to Go).

### 1.2 Handling 1,000 Hours / 60MB / 60k Slides
**Status: ✅ CAPABLE (With Strategic Structuring)**
- **Format Decision**: **JSON Bundles** for indexing, **Markdown** for depth. See [Content Architecture](./training/content-architecture.md) for the full comparison.
- **Volume**: 60MB is handled via "Progressive Disclosure" (loading only what's needed).

---

## 2. Proposed Content Training Hierarchy (Agent-Optimized)

To optimize for AI Agent discovery (RAG-lite) and generation, the content should be structured using a **"Progressive Disclosure"** hierarchy.

### 2.1 The "Atomized Bundle" Structure
Instead of 60,000 files, use **600 files** (100 slides per file) or **60 files** (1000 slides per file).

```text
/docs/training/
├── roadmap.json          # Programmatic master plan
├── index/                # Searchable metadata
│   ├── keywords.json     # Map keywords -> bundle IDs
│   └── taxonomy.json     # Hierarchical categories
└── slides/               # The 60MB Payload
    ├── phase-1/
    │   ├── module-01.json # Contains 100-1000 slide objects
    │   └── module-02.json
    ├── phase-2/
    └── ...
```

### 2.2 Data Schema for Slide Bundles (JSON)
Agents work best with structured data. Each "Module" file should follow this schema:

```json
{
  "moduleId": "P1-M1",
  "title": "Web Fundamentals",
  "slides": [
    {
      "id": 1,
      "title": "HTTP Protocol",
      "content": "Raw 1KB text or code snippet...",
      "tags": ["network", "basics"],
      "referenceFiles": ["repo-php/public/index.php"]
    }
  ]
}
```

### 2.3 Optimization for Generation
1. **Semantic Indexing**: Create a `master-index.md` that summarizes the content of every bundle. The agent reads this first to decide which `module-X.json` to load.
2. **Context Injection**: When an agent assists a student in "Phase 2", the system can automatically load the `phase-2/summary.json` into the context.
3. **Cross-Linking**: Every slide should reference 1-2 actual source code files in the `repo-*` folders. This allows the AI to ground the theory in the project's real code.

## 3. Recommended Implementation Steps
1. **Consolidate**: Migrate any existing fragmented documentation into "Bundles".
2. **Database Strategy**: For 60,000 entries, consider a `training.db` (SQLite) instead of JSON files. AI Agents can run SQL queries to pull exactly the slides they need.
3. **Agent Role Definition**: Define a "Training Coach" agent in `AGENTS.md` specifically tasked with navigating this hierarchy.
