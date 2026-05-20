# AI Team Session Management

## Session Storage

**Are the sessions stored in flat files or SQLite db?**
Sessions and their associated artifacts are strictly stored as **flat files** in the local file system (typically within a designated directory like `Documents/AITeam/`). 

We chose flat files over an SQLite database to ensure seamless interoperability with the Multi-App Hub's File Explorer applet. By saving outputs as standalone files, users can natively browse, open, and manage their generated text and images using the standard file explorer interface without requiring any database extraction or custom UI layers.

## Session Continuity

**How to load and continue a session?**
Because the system relies on flat-file storage rather than a central database, session continuity works by reading from the file system:
- **Active State:** During an active session, the conversation context is maintained in-memory within the `ViewModel` of the `AITeamScreen`.
- **Loading Previous Sessions:** To continue a past session, the applet parses a chronological conversation manifest (e.g., `session_id.json` or by reading the directory contents) that links user prompts to their generated output files. When a user selects a past session from the history, the applet reads these files into memory to reconstruct the chat UI and resume the conversation with the AI.

## Output Formats

**What output formats are possible?**
The AI Team applet generates and saves outputs in standard, widely readable file formats:

1. **Structured Text (`.txt`, `.md`)**
   - Used for standard chat responses, technical explanations, and code snippets.
   - Markdown is the preferred default to support rich formatting (bold, italics, code blocks) in compatible text viewers.
   - *Prompt Example:* "Write a comprehensive onboarding guide for new Android developers. Format the document with Markdown headings, bullet points, and code blocks, and output it as `onboarding_guide.md`."
2. **Images (`.jpg`, `.png`)**
   - Used for generated visual content.
   - Images are downloaded or decoded from the API response and written directly to the file system as standard image files.
   - *Prompt Example:* "Generate a concept art illustration of a futuristic, neon-lit mobile device interface. Please save the output as `concept_art.png`."
3. **Data & Session Manifests (`.json`)**
   - Used to store the conversational metadata, mapping the sequence of prompts to their corresponding `.md` or `.png` output files for session reloading.
   - *System Note:* While session manifests are generated automatically by the applet's state manager, you can also prompt the AI to export raw structured data.
   - *Prompt Example:* "Extract the top 5 UI color hex codes we discussed today and return them as a clean JSON array file named `theme_colors.json`."
