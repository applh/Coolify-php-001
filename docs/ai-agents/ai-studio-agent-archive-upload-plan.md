# AI Studio Agent Integration Blueprint: Sovereign Code Archiving & Shipping

This implementation plan chronicles the design, configuration, and secure execution of a specialized **AI Studio Agent** designed to archive workspace code into a pristine zip collection and securely ship (POST) it to an external gateway API using custom env variables.

---

## 🚀 1. Overview of Latest AI Studio Agent Features

Google AI Studio's agent and sandbox platform offers powerful capabilities that shift the paradigm from manual actions to high-fidelity, autonomous operations:

1.  **System-Level Custom Blueprints (`AGENTS.md` / `GEMINI.md`)**: You can define project-wide instructions that are automatically loaded into the agent's context. This lets you enforce layout rules, formatting standards, and custom tool usage directly at the system-instruction level.
2.  **Secure Environment Management Vault**: AI Studio provides a secure, client-hidden Environment Variables interface (via Settings). Sensitive secrets (such as external transmission URLs and JWT/Bearer API keys) are injected server-side. This keeps keys completely hidden from both public web previews and version-controlled codebase files (like `.env`).
3.  **High-Performance Tool Calling (Parallel Orchestration)**: Advanced models (such as Gemini 2.5 Flash and Gemini 2.5 Pro) can coordinate multi-step workflows. An agent can discover files, filter them, draft code, compile, and run transmissions concurrently.
4.  **Sandbox Runtime Environment**: The container environment includes a CLI runtime with Node.js and npm preloaded, empowering agents to interact directly with the workspace filesystem using standard scripting engines (such as `tsx` matching TypeScript code on the fly).

---

## 🛠️ 2. Architectural Design of the Shipping Toolchain

To execute recursive zipping and network streaming without bloated dependencies or security vulnerabilities, we avoid heavy multi-package architectures and rely on a lightweight, secure hybrid strategy:

```
[ AI Studio Workspace ]
        │
        ├──> [ Excludes filter (.git, node_modules, dist, .env) ]
        │
        ├──> [ adm-zip Native Compactor ]
        │
        └──> [ project-archive.zip (Ephemeral Local Memory) ]
                    │
                    ▼
          [ Node 22 Fetch Client ]  <─── [ Reads Secure Env Variables ]
                    │
                    ▼
         [ POST Binary Stream / Form ] ───>  [ External Gateway Pipeline ]
```

### Key Security Safeguards
*   **Leak Prevention**: The walk utility strictly excludes `.env` files, which prevents system credentials and local keys from leaks during transmission.
*   **Memory Footprint**: Ephemeral generation limits disk usage. Files are cleaned up instantly using file-system `unlink` hooks regardless of success or failure.
*   **Boundary Control**: Bulk runtimes (`node_modules`) and build artifacts (`dist`, `build`) are ignored to optimize bandwidth and memory consumption.

---

## 🤖 3. Creating the "Sovereign Archiver" Custom Agent

Follow these instructions in the Google AI Studio user interface to provision a dedicated agent capable of executing this operation:

### Step 1: Initialize the Custom Agent Space
1. Open your **Google AI Studio** workspace.
2. Select **Create Custom Agent** or navigate to the specialized **System Instructions** card.
3. Configure the active model to use `gemini-2.5-flash` or newer for optimized speed, tool call reliability, and context size.

### Step 2: Set the System Prompt (System Instructions)
Copy and paste the following operational directives into the customized system prompt box:

```text
You are the Sovereign Archiver & Shipper Agent for the FRAISE platform.
Your primary objective is to package the current workspace files into a clean ZIP archive and transmit (upload) it to the external API provided in the environment variables.

### Operational Directives:
1. When the user requests an archive and upload, perform a directory scan to confirm the project structure is intact.
2. Run the command: `npx tsx archive-uploader.ts` in the workspace root directory.
3. Observe and capture the CLI console outputs. Highlight the compiled size (MB), HTTP status code returned, and transmission confirmations.
4. If the execution catches environment configuration errors (such as missing variables), instruct the user specifically on how to configure them in the Settings tab.
5. Ensure that no secret local keys (specifically in .env) are ever collected, read, or outputted.
6. Provide a concise, clear high-level confirmation to the user upon success.
```

### Step 3: Configure Tools and Permissions
*   Enable **Code Execution** (the terminal/shell interaction capability) so the agent can invoke `npx tsx` and interact with the filesystem.
*   Set **Secure Environment Variable Access** if prompted, allowing the agent to evaluate variables.

---

## 🔑 4. Secure Environment Variables Configuration

To run the custom shipper securely, configure the secret variables in AI Studio's **Environment Settings**:

| Variable Name | Required | Default Value | Description |
| :--- | :---: | :--- | :--- |
| `SHIPPING_API_URL` | **Yes** | *None* | The external destination endpoint where the ZIP archive will be uploaded. |
| `SHIPPING_API_TOKEN` | *Optional*| *None* | Bearer Token or API authorization key. Prefixed automatically with `Bearer ` if omitted. |
| `ZIP_OUTPUT_NAME` | *Optional*| `project-archive.zip` | The file name target for the compacting process on local disk before upload. |

---

## 📦 5. How to Run the Toolchain manually

You can execute the shipper manually at any time using the terminal or by prompting any assistant agent:

```bash
# Execute the compiler script directly utilizing the tsx engine
npx tsx archive-uploader.ts
```

### Handling Custom Multipart Upload Formats
If your receiving gateway server strictly requires the standard `multipart/form-data` format instead of a raw binary stream, the script can be modified slightly to populate a `FormData` envelope:

```typescript
const formData = new FormData();
// Retrieve the buffer from temporary disc and append it to the body
const fileBlob = new Blob([zipBuffer], { type: 'application/zip' });
formData.append('file', fileBlob, ZIP_OUTPUT_NAME);

const response = await fetch(SHIPPING_API_URL, {
  method: 'POST',
  body: formData,
  headers: {
    'Authorization': `Bearer ${SHIPPING_API_TOKEN}`
    // Note: Do NOT manually declare Content-Type for FormData; Node/fetch will auto-inject the correct boundary!
  }
});
```

---

## 📝 6. Architectural Decision Records (ADR)

*   **Decision**: Utilize `"adm-zip"` instead of the heavy native `archiver` or spawning bash `zip` binaries.
*   **Rationale**: `adm-zip` is written in 100% pure JavaScript, making it environment-agnostic. Spawning terminal commands (`zip -r ...`) behaves differently on different platform OS/distros and can lead to silent failures on environments with locked-down shell permissions.
*   **Exclusion Policy**: All dependencies (`node_modules`), build results (`dist`/`build`), Git histories/meta (`.git`), and local credentials (`.env`) are explicitly ignored to ensure code transfers remain lightweight, fast, and completely safe.

---

## 🌟 Practical Lab Info
A dedicated training laboratory has been appended to the curriculum in **Part 9 (Intelligence, Agents & Gemini API)**. Check the details of the lab: [Part 9: Intelligence, Agents & Gemini API](/docs/training/PART-09-AGENTS.md) under Lab 10: "Sovereign ZIP Shipper & Agent Tools".
