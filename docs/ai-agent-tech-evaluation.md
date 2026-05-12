# Technical Evaluation: Data Formats for AI Agents

When building applications designed to be managed or modified by AI agents (like Gemini or Antigravity), the choice of data format significantly impacts "editability," token consumption, and reasoning accuracy.

## 1. Flat Files (Preferred for Agent Interaction)

### Markdown (`.md`)
*   **Pros:** Best balance of human readability and structural meaning. AI agents understand Markdown headers and blocks intuitively. Excellent for documentation and content-rich pages.
*   **Cons:** Not suitable for deeply nested or relational data.
*   **Agent Synergy:** High. Context injection usually handles Markdown perfectly.

### YAML (`.yaml`, `.yml`)
*   **Pros:** Lower token overhead than JSON. Strict indentation provides structure without excessive punctuation (`{}`, `[]`, `"`). Very easy for agents to parse and "fill in."
*   **Cons:** Indentation errors can be fatal for parsers if the agent makes a mistake.
*   **Agent Synergy:** Very High. Preferred for configuration.

### JSON (`.json`)
*   **Pros:** Universal compatibility with almost every programming language. Schema validation (JSON Schema) allows for strict control.
*   **Cons:** High token consumption due to repeated quotes and braces. Harder for humans (and sometimes AI) to view "diffs" clearly.
*   **Agent Synergy:** Medium-High. Best for APIs and internal state, but use YAML for configuration if possible.

### Plain Text (`.txt`)
*   **Pros:** Zero overhead. Lowest possible token cost.
*   **Cons:** No built-in structure. Requires custom parsing logic.
*   **Agent Synergy:** Low for logic, High for raw content/logs.

---

## 2. Specialized Agent Patterns

### Base64-in-Text (`.txt`)
This project utilizes a specific pattern where binary assets (images) are stored as Base64 strings inside `.txt` files.
*   **Pros:** Allows the AI agent to "see" and "transport" binary data as text without needing specialized binary-handling tools in the early stages of a pipeline.
*   **Cons:** Massive token cost if the full file is read into context. Not efficient for large assets.
*   **Strategic Use:** Good for small icons or UI textures that the agent needs to generate or move.

### Zip Archives (`.zip`)
*   **Pros:** Compresses many files into one; preserves directory structures and permissions.
*   **Cons:** Opaque to the AI agent until unzipped. Most LLMs cannot "edit" inside a zip in real-time without a tool-call to unzip, modify, and re-zip.
*   **Agent Synergy:** Low for active work, High for "packaging" a finished result for the user.

---

## 3. Databases

### SQLite (`.db`, `.sqlite`)
*   **Pros:** High performance for relational queries. All-in-one file deployment. Support for complex state and search.
*   **Cons:** Binary format is invisible to the agent without a SQL tool/adapter. Agents cannot `edit_file` a `.db` file; they must use a database client.
*   **Agent Synergy:** Medium. Requires a "SQL Explorer" or similar toolkit to be effective. Best when the data volume exceeds the context window.

---

## Summary Recommendation

| Use Case | Recommended Format | Why? |
| :--- | :--- | :--- |
| **Documentation** | Markdown | Structural clarity. |
| **Config/Settings** | YAML | Cleanest syntax, low tokens. |
| **Structured Data** | JSON | Standard-compliant, schema-ready. |
| **Relational Data** | SQLite | Queryable performance over large sets. |
| **Agent Visuals** | Base64-TXT | Allows binary "proxy" in text contexts. |

### Pro-Tip: The "Context Gap"
If data is larger than ~50KB, avoid JSON/YAML for the agent's *active* memory. Move to **SQLite** or **Flat Files with a Search Index** to prevent the agent from being overwhelmed by its own context window.
