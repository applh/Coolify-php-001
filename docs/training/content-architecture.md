# Training Content Architecture: Markdown vs JSON

## 1. The Challenge
Managing **60,000 slides (60MB)** across **1,000 hours** requires a data format that balances AI "Grounding" (linking theory to code) with high-speed discovery.

## 2. Evaluation Table

| Metric | JSON (Structured Bundles) | Markdown (Unstructured Prose) |
| :--- | :--- | :--- |
| **Agent Parsing** | High (Deterministic) | Low (Heuristic/Greedy) |
| **Discovery** | Selective (Filter by ID/Tags) | Sequential (Must read top-to-bottom) |
| **File Count** | Low (60 Files @ 1000 slides/ea) | High (60,000 Files @ 1 slide/ea) |
| **Grounding** | Explicit Schema (`codeRefs: []`) | Implicit (Grep matches) |
| **Human Editability** | Manual (Syntax heavy) | Native (Natural language) |

## 3. The Final Architecture: "Hybrid Metadata"

To optimize AI performance, we adopt a **JSON-Manifest + Markdown-Atomic** approach.

### 3.1 Layer 1: The Taxonomy (JSON)
Agents read the `taxonomy.json` at the start of a session. This file maps high-level concepts to Module Bundles.
- **Benefit**: Zero-latency concept lookups.

### 3.2 Layer 2: The Bundle Index (JSON)
Each Phase (1-5) has a `Phase-X-Manifest.json`. This contains 1KB snippets of every slide.
- **Benefit**: The agent can "skim" 1000 slides in a single context window to find the most relevant one.

### 3.3 Layer 3: The Deep Dive (Markdown)
Once an agent identifies a specific slide ID, it loads a dedicated Markdown file (or a specific block in a "Big Markdown" file) for the full code example and explanation.
- **Benefit**: Preserves syntax highlighting and formatted technical instructions for the user.

## 4. Storage Limits in AI Studio
- **File Explorer**: Avoid lists > 50 files in a single folder. Use subbundles.
- **Context Window**: Keep single JSON manifests < 200KB for maximum prompt efficiency.
