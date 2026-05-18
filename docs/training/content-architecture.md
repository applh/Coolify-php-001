# Training Content Architecture: 10x10x10 Hierarchy

## 1. The Challenge
Managing **60,000 slides (60MB)** across **1,000 hours** requires a highly organized data format that balances AI "Grounding", content generation, and high-speed discovery. 

The training hierarchy is split into two distinct environments:
- **AI Studio:** Manages the logical hierarchy (10x10x10 levels) up to 1-hour granularity. Agents use this to provide detailed content and systematically improve slides at each level.
- **Deployed Training App:** Consumes the 1-hour granularity data to dynamically generate or display 60+ slides per hour of training.

## 2. The 10x10x10 Structure

To achieve this, the curriculum is structured as:
- **10 Phases** (100 hours each): Broad disciplinary tracks (e.g., Web Foundations, Frontend Orchestration).
- **10 Modules per Phase** (10 hours each): Specific subjects within the track.
- **10 Lessons per Module** (1 hour each): The finest granular level stored in AI Studio. Each lesson holds the metadata and AI instructions required to generate 60 slides.

## 3. The Implementation: "Hybrid Metadata"

To optimize AI performance while adhering to constraints, we adopt a **JSON-Manifest + Markdown-Atomic** approach.

### 3.1 Layer 1: The Root Taxonomy (JSON)
The `taxonomy.json` file maps the first two levels: the 10 Phases and 100 Modules.
- **Benefit**: Zero-latency concept lookups for the high-level roadmap.

### 3.2 Layer 2: The Phase Manifests (JSON)
Each Phase (1-10) has a dedicated `Phase-X-Manifest.json` (or handled directly in AI Studio folders). These manifests contain the 100 Lessons (1 hour each) and their slide generation metadata.
- **Benefit**: AI Studio agents can "skim" all the lessons in a phase within a single context window to improve or expand them.

### 3.3 Layer 3: The Deep Dive / Slide Generation (Markdown / UI)
Once an agent or user identifies a specific 1-hour Lesson, it loads a dedicated Markdown file (or a specific block in a "Big Markdown" file) for the full code example and explanation. The deployed app takes this logic to generate the 60 slides.
- **Benefit**: Preserves syntax highlighting and formatted technical instructions for the user.

## 4. Storage Limits in AI Studio
- **File Explorer**: Avoid lists > 50 files in a single folder. Use sub-directories per module or phase.
- **Context Window**: Keep single JSON manifests < 200KB for maximum prompt efficiency.
