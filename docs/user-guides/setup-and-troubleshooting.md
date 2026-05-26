# FRAISE * AI Software Engineer - Setup & Troubleshooting

This project is an advanced hybrid application featuring a **Node.js (Express + Vue 3) Management Dashboard** and multiple containerized stacks: **PHP Multisite**, **React**, **Vue**, and **Python FastAPI**.

## 1. Architecture Overview

### Backend (Node.js/Express)
- **Role:** Centralized CMS Management, File API, AI Media Task Queue.
- **Entry Point:** `server.ts`
- **Database:** SQLite (`cms.db`) for tracking AI media tasks.

### Frontend (Vue 3)
- **Role:** Visual dashboard for managing all stacks, editing files, and generating AI images.
- **Build System:** Vite.

### Application Stacks
- **repo-php/**: The original website engine that serves client-facing pages (Apache/PHP).
- **repo-react/**: Modern React frontend stack (Vite + TypeScript).
- **repo-vue/**: Modern Vue frontend stack (Vite + TypeScript).
- **repo-python/**: Backend API stack (FastAPI + Uvicorn).

---

## 2. Setup Instructions

### Local Development (AI Studio Preview)
1. **Install Dependencies:**
   The platform handles `npm install` automatically.
2. **Start Development Server:**
   Run `npm run dev`. This starts the Node.js server with Vite middleware enabled.
3. **Open Preview:**
   Use the AI Studio Preview window to interact with FRAISE.

### Production Build & "Publish" (Deployment)
1. **Build the Frontend:**
   The platform usually runs `npm run build` which populates the `dist/` directory.
   ```bash
   npm run build
   ```
2. **Start the Production Server:**
   The `npm start` script runs the server with `NODE_ENV=production`.
   ```bash
   npm start
   ```
   In this mode, `server.ts` serves static files from `dist/` and disables the Vite HMR/Refetch middleware.

---

## 3. Serving the PHP Application

**Important Notice:** The Node.js Manager is a *management tool*. It does not natively run the PHP interpreter for the sites.

- **To view PhP sites in AI Studio:** Since the AI Studio container is optimized for Node.js, it may not have a PHP runtime. To see your PHP sites, you typically manage them here and deploy the `repo-php` folder to a PHP-ready host.
- **To manage PhP sites:** Use the **Explorer** and **Editor** in the Node CMS Dashboard to modify the templates located in `repo-php/content/`.

---

## 4. Complete Troubleshooting Explanation

In a high-density, multi-repo hybrid application environment (Node.js, PHP, React, Vue, Python FastAPI, and Jetpack Compose Android), system failures typically manifest along predictable architectural seams. Understanding these failure classes prevents superficial, repeating fixes and directs developers—and AI agents—to the true root causes.

### 4.1 Multi-Stack Integration & Container Boundary Conflicts
In local development (like the AI Studio Preview) and production-grade Docker deployments, services often communicate across container boundaries. The most common integration failures include:
- **Port Collisions & Proxy Constraints:** The platform's reverse proxy exclusively routes external traffic through **Port 3000**. If any development server (Vite, Express, PHP router) attempts to bind to or expose another port (e.g., 5173, 3001) to the outside world, the connection will time out. 
- **Filesystem Permission & Ownership Mismatches:** When Docker containers mount shared volumes (e.g., `repo-php/content/` mounted to both the Node.js Host Manager and the PHP Apache container), files created by one process may be given an exclusive UID/GID (such as root or 1000). The second process then throws a `Permission Denied` error when trying to modify or delete them. This is common during AI-driven template writes or Zip restorations.
- **Host Resolution Divergence:** In containerized networks, `localhost` refers to the container itself, not the Docker host network. If a Python FastAPI service tries to reach the PHP application using `localhost:8000`, the connection will fail. Instead, internal Docker service names (defined in `docker-compose.yml`) must be used for address mapping.

### 4.2 Application-Specific State & Routing Failures
- **"404 - Site Not Configured" (PHP Engine):** The PHP Apache multisite instance uses dynamic virtual hosting based on the incoming `Host` header. It checks the `/repo-php/content/` folder for a directory matching the hostname. If a request is proxied incorrectly, or if the directory lacks an `index.php`, the router throws a 404 block.
- **React & Vue Vite HMR Socket Drops:** During development, the browser attempts to establish a websocket connection with Vite's Hot Module Replacement (HMR) server. In the AI Studio iframe sandboxed runtime, **HMR is disabled by the platform (`DISABLE_HMR=true`)** to prevent continuous repainting during incremental code writes. This produces "failed to connect to websocket" errors in the console. These are completely benign and should be ignored, as the system refreshes the frame on command completion.
- **SQLite Database Locks:** Simultaneous write connections to the decentralized key-value stores (`cms.db` or task databases) from multi-threaded workers can lock the file, causing write operations to block or fail with `SQLITE_BUSY`.

### 4.3 Third-Party API & SDK Upgrade Deficiencies
Integrating modern LLM frameworks (like the `@google/genai` TypeScript SDK) or geographic maps platforms without rigid verification can crash processes silently on boot:
- **API Model Deprecation/Mapping Failures:** Older code bases that bind to ancestor model IDs (e.g., `gemini-1.5-flash`) will immediately fail with `404 Model Not Found` when the cloud provider deprecates that model in favor of modern equivalents (e.g., `gemini-2.5-flash`).
- **Initialization Crash Loop:** Running an SDK client initialization globally at module import time (e.g., `const stripe = new Stripe(process.env.STRIPE_KEY!)`) causes the entire runtime server to crash immediately on start if the key is missing in `.env`.
- **Silent Failing (Lack of Diagnostic Transparency):** Simple catch-all blocks `try { ... } catch (e) {}` swallow stack traces, leading developers and AI agents into endless debug loops. Instead, services must log explicit parameters (error names, HTTP status codes, missing parameters) to empower the system to heal itself.

---

## 5. Complete Debug Steps and Strategies

This systematic debug roadmap enables developers and AI agents to isolate, diagnose, and resolve cross-stack bugs with surgical precision.

```
                  ┌───────────────────────────────┐
                  │    Step 1: Isolate & Replicate│
                  │   (Stand-alone scripts/REPL)  │
                  └───────────────┬───────────────┘
                                  ▼
                  ┌───────────────────────────────┐
                  │  Step 2: Probe Capabilities   │
                  │ (Verify APIs & Model Catalogs)│
                  └───────────────┬───────────────┘
                                  ▼
                  ┌───────────────────────────────┐
                  │   Step 3: Audit Ecosystem     │
                  │   (Logs, Ports, & Processes)  │
                  └───────────────┬───────────────┘
                                  ▼
                  ┌───────────────────────────────┐
                  │  Step 4: Tight Validation Loop│
                  │ (Lint ➔ Compile ➔ Dev Server) │
                  └───────────────────────────────┘
```

### 5.1 Step 1: Isolation & Reproducibility (Isolate-Is-King)
Never debug inside a complex visual process. Extract the failing code component and run it in absolute isolation.
1. **Use Node REPL or Standalone Script Execution:** If an integration function (such as a database query or a CMS Markdown file parser) fails, create a temporary script `test-leak.ts` and execute it with:
   ```bash
   npx tsx test-leak.ts
   ```
2. **Utilize `curl` for HTTP Handshaking:** Do not rely on front-end browser fetch. When debugging the Python FastAPI backend, curl it directly from inside the main terminal to see dry raw JSON outputs:
   ```bash
   curl -i http://localhost:3000/api/python-health-endpoint
   ```
3. **Verify File Exits & Relative Workspace Boundaries:** When writing automated file read/write routines, remember that `/` represents the absolute root container, whereas `.` refers to the workspace. Test paths interactively before embedding them in script utilities.

### 5.2 Step 2: Proactive Discovery & API Capabilities Auditing
If an external API (like Gemini SDK or Google Maps API) begins throwing connection or validation errors, run a live capability query to understand what the server supports *right now*.
1. **List Active Models Legally:** Do not guess which endpoint is active. Execute an anonymous model check directly from the server or terminal to discover valid models, methods, and configurations available in your current region:
   ```bash
   curl https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY
   ```
2. **Analyze Dynamic Catalog Output:** Inspect the resulting schema. If your script specifies a model alias that is missing from the list, update your SDK configuration to match the modern confirmed alias. 
3. **Lazy Initialization Guard:** Wrap client-facing SDK declarations in a lazy-loading accessor function rather than module-level instantiations. This standard prevents severe runtime crashes if keys are unconfigured:
   ```typescript
   // Guarded Initialization Standard
   let geminiClient: GoogleGenAI | null = null;
   export function getGeminiClient(): GoogleGenAI {
     if (!geminiClient) {
       const key = process.env.GEMINI_API_KEY;
       if (!key) {
         throw new Error("CRITICAL: GEMINI_API_KEY is not defined in the environment config.");
       }
       geminiClient = new GoogleGenAI({ apiKey: key });
     }
     return geminiClient;
   }
   ```

### 5.3 Step 3: Diagnostic Audits (Logs, Ports, and Stagnant Processes)
1. **Trace Logs Chronologically:** Review backend terminal outputs or write log entries using structured patterns (e.g., `AppLogger` or JSON payloads).
2. **Audit Open Ports & Address Bindings:** If the dev server fails to launch because a port is busy, check what process is holding Port 3000:
   ```bash
   grep -r "3000" .
   ```
3. **Terminate Orphaned Servers (Clean Slate):** Stagnant Node.js processes can hijack HMR file watchers. Clean them out completely:
   ```bash
   pkill -f tsx || pkill -f node || pkill -f vite
   ```

### 5.4 Step 4: Multi-Tier Verification Loop
Always verify modifications progressively to avoid cascading workspace corruption. Follow this verification pipeline strictly:
1. **Run `lint_applet`:** This is the cheapest test. Catches immediate syntax errors, undeclared variables, missing package imports, and schema irregularities in less than 5 seconds.
2. **Run `compile_applet`:** Executes full static typing compilation against TypeScript and framework files. Confirms that structural builds are sound.
3. **Run `restart_dev_server`:** Resets standard dev server states once compilation is confirmed green. Cleans runtime environment parameters.

---

## 6. Complete Examples of Prompts to Help AI Agents Debug

To get accurate, bug-fixing responses from AI Coding Agents (such as Gemini in AI Studio), avoid vague descriptions. Provide files, full error traces, and exact contextual guardrails. Use these optimized prompt templates to guide AI agents:

### 6.1 Prompt Template: Debugging API Mismatches & Missing Credentials
Use this prompt when an SDK integration (Gemini, Stripe, Google Maps) is failing to connect or throwing authentication/routing errors:

```markdown
Role: Senior SDK/Integration Engineer
Task: Debug an API integration failure in our multi-repo workspace.

Issue: My application is failing to connect to [API Name, e.g., Gemini API]. 
Current Error Message / Track Trace: 
"[Insert complete stack trace or terminal output error here]"

Primary Files to Inspect:
- `server.ts`
- `/src/views/SiteDashboard.vue`

Context & Guardrails:
1. Do NOT write mock data as a fallback. I need a real, functional API integration.
2. Ensure you utilize lazy initialization for the SDK client. Do not initialize at global module load.
3. If this is an API routing or registration issue, write a lightweight discovery command/function (e.g. querying model discovery API endpoint `v1beta/models?key=...`) to verify active capabilities and write out the results transparently.
4. Upgrade any outdated model SDK definitions (e.g., from deprecated ancestral model names to current stable aliases like 'gemini-1.5-flash' or 'gemini-2.5-flash').

Please analyze this error, detect the broken line, explain the root cause, and modify the code with the correct standard replacements.
```

### 6.2 Prompt Template: Debugging CSS, Overlays, and Responsive Layout Bugs
Use this prompt when a UI element is offset, covering interactive components, lacking contrast, or scaling poorly on mobile viewports:

```markdown
Role: Lead UI/UX Engineer
Task: Fix visual layout and styling bugs in our Vue/Tailwind dashboard.

Issue: [Describe specific layout bug, e.g., "The map picker element in AgendaScreen.kt is covering the footer when the viewport scales down to mobile widths, and buttons are not clickable due to overlay issues."]

Primary Files to View:
- `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- `/src/style.css`

Core Constraints:
1. Use ONLY Tailwind utility classes for styling. Do NOT use custom inline CSS or separate stylesheet definitions.
2. Maintain strict Web Content Accessibility Guidelines (WCAG) compliance for visual contrast ratios.
3. Design for a fully responsive layout range (Desktop-first or robust Mobile-first grids).
4. Ensure critical touch-targets on mobile viewports are at least 44px in size.
5. All custom UI alert notifications must use a custom inline toast or custom CSS warning elements—do NOT use blocking native systems like `window.alert()`.

Analyze the responsive breakpoint classes and the stacking order (z-index) of the container elements in the specified file, and correct the classes to repair the visual overflow.
```

### 6.3 Prompt Template: Debugging TypeScript Compile, ESM, & Vite Bundle Crashing
Use this prompt when Vite compile outputs fatal packaging crashes or TypeScript complains about type discrepancies:

```markdown
Role: Senior Systems Bundler and Architect
Task: Fix TypeScript type errors and Vite compilation blocks.

Issue: The command `compile_applet` is failing during the production build cycle. 
Build Console Output: 
"[Insert entire compile/build failure text or CLI output here]"

Context & Build Standards:
1. All typescript `import` statements must remain at the top level of the module file.
2. We must use named imports/exports; do not destructure objects in your import statements.
3. Do NOT use `import type` to import Standard Java-style Enums.
4. Use standard `enum` declarations, never use `const enum`.
5. Check if dependencies are already listed inside `package.json` before attempting to add new ones. If a package is missing, state it clearly.

Identify which package imports or static type bindings are causing the tree-shaking parser to fail, modify the declarations to be compliant with TypeScript ESM bundling rules, and verify the changes using linting.
```

### 6.4 Prompt Template: Debugging Cross-Container File Write & Volume Mounting
Use this prompt when services fail to read/write directories or files across container boundaries:

```markdown
Role: Senior Devops / Systems Engineer
Task: Resolve cross-container file execution and mounting block.

Issue: The Node.js Host Manager throws an `EACCES: permission denied` error when compiling static web templates or modifying files located within the shared Docker volumes folder.
Permissions on Host:
"[Insert folder permissions, e.g., drwxr-xr-x 2 root root]"

Primary Files:
- `docker-compose.yml`
- `Dockerfile`

Context & Security Guardrails:
1. Do not relax volume security permissions completely (e.g., avoid standard `chmod -R 777` permissions).
2. Align user runtime execution IDs (UID/GID) inside the Dockerfile configs so that they execute with coordinated permissions.
3. Document any required environmental configurations in `.env.example` cleanly without exposing real secrets.

Trace the network and user ID alignments across our containers, and correct the mount arguments or container build contexts to enable secure, concurrent write states.
```

---

## 7. Android App Troubleshooting: Background Tasks & Crons

If `repo-android` background features (such as Cron Services, Automation Tasks, or Alarms) are not triggering automatically:

1. **"App Closed" vs "App Force Stopped":** 
   - If the user presses the Home button or backs out of the app normally, `AlarmManager` and `WorkManager` **will continue to work** and will wake up the app when needed.
   - If the user **swipes the app away from the Recent Apps list**, many Android OEMs (Samsung, Xiaomi, OnePlus) treat this as a **Force Stop**.
2. **Foreground App Wipes (Force Stop consequence):** 
   - Force-closing an app via the Recents tray will unilaterally cancel all scheduled alarms, broadcast receivers, and `WorkManager` queues on most Android skins. The app will **not** wake up again until the user explicitly taps the app icon to relaunch it, or the device reboots (triggering the `BootCompletedReceiver`).
3. **OEM Battery Restrictions (Aggressive Killing):** Manufacturers have highly aggressive App Standby Buckets that immediately suspend background activity if an app is closed.
   - **Fix:** Navigate to device **Settings -> Apps -> Your App -> Battery** and change the state to **Unrestricted** or **Don't Optimize**. Some devices also have a "Lock in Recents" feature to prevent accidental swiping.
4. **Exact Alarms Revoked:** Starting in Android 14 (API 34), `SCHEDULE_EXACT_ALARM` permissions can be silently rejected or disabled by the OS to save battery. The updated `CronScheduler` defaults to a standard loose alarm if this is blocked, but this can cause triggers to become inaccurate.

---

## 8. Deployment Recommendation

For a live production environment, we recommend:
1. Hosting the **Node.js FRAISE Backend** on a Node service (e.g., Cloud Run, Railway, Heroku).
2. Hosting the **PHP Multisite App** on a specialized PHP platform or a VPS using Docker (as provided in `repo-php/docker-compose.yml`).
3. Deploying frontend SPA projects directly to an edge CDN (Vercel, Netlify, Cloudflare Pages) if built separately from the custom backend.
