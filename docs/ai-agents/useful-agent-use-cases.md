# Useful Agent Use Cases in Google AI Studio

This directory logs high-value, production-tested use cases for **AI Studio Coding Agents**. These patterns leverage AI Studio's cloud-sandboxed Node/Python runtimes, parallel tool-calling capabilities, and secure Environment variable injection to automate complex engineering operations.

---

## 🗺️ Summary of Use Case Catalog

| Use Case | Core Strategy | Trigger Mode | Major Capabilities Used |
| :--- | :--- | :--- | :--- |
| **1. Sovereign ZIP Code Shipper** | Zips filtered project files and ships directly to an external VPS/API. | On-Demand CLI / Prompt | Workspace FS, Fetch Stream, Secure Secrets |
| **2. Playwright/Puppeteer UI Auditor** | Crawls rendered previews, compiles performance checklists, and audits accessibility. | Hook/Cron Execution | Headless Browser, Audit Reports |
| **3. Polyglot API Bridge Generator** | Analyzes backend controllers (PHP) and generates matching Vue/React interfaces and Types. | Prompt-Driven | Cross-Stack Context, Ast Parsing |
| **4. Autonomous Linter-Fixer Loop** | Runs linters, parses diagnostics, and edits files systematically until clean. | Self-Correction | `lint_applet` / `compile_applet` Multi-Edit |
| **5. AI-Powered Localization Agent** | Parses static UI text or templates and updates JSON dictionaries via Gemini. | Workflow / CLI Prompt | Translation APIs, JSON Manifest Schema |
| **6. Database Seeder & Schema sync** | Inspects DB structures, synthesizes realistic relational seed data, and executes migrations. | CLI Task Runner | SQLite/Postgres Engines, Data Synthesis |

---

## 📦 Use Case 1: Sovereign ZIP Code Shipper

### Business Context & Problem
When a developer completes an iteration inside the high-speed AI Studio preview environment, they often need to "ship" the current state directly to a staging VPS, a Coolify server, or a continuous integration (CI) webhook. Doing this manually via git pushes, logins, and manual command execution is repetitive.

### The Agent Solution
A single-purpose agent that compresses the workspace (filtering out standard bulk patterns like `.git`, `node_modules`, and personal `.env` keys) and feeds a raw binary stream of the ZIP to a target pipeline.

### System Prompt Blueprint
```text
You are the Code Dispatch Agent. Your mission is to package the active workspace and deploy it to the target external server.

### Operational Directives:
1. When triggered, scan the directory structure to identify build outputs.
2. Run the deployment script `npx tsx archive-uploader.ts`.
3. Read the environment variables:
   - `SHIPPING_API_URL` (Destination gateway)
   - `SHIPPING_API_TOKEN` (Header authentication token)
4. Echo the status code of the HTTP response along with the exact size of the ZIP bundle.
5. Clean up any temporary files upon execution.
```

---

## 🎨 Use Case 2: Playwright/Puppeteer UI Auditor

### Business Context & Problem
Before promoting code changes to production, teams need to verify that CSS visual transitions render properly on mobile vs. desktop viewports, check for visual clutter, and catch regressions (like broken layout grids or unreadable contrast).

### The Agent Solution
An agent utilizing a sandboxed Node environment with Puppeteer/Playwright to take screenshots of the live preview container inside AI Studio, run a light lighthouse/AXE audit, and output clear aesthetic optimization suggestions.

### Execution Script Pattern (`ui-auditor.js`)
```javascript
const playwright = require('playwright');
const fs = require('fs');

async function runAudit() {
  const TARGET_URL = process.env.DEV_APP_URL || 'http://localhost:3000';
  console.log(`🔍 Launching Headless Chromium to audit: ${TARGET_URL}`);
  
  const browser = await playwright.chromium.launch();
  const page = await browser.newPage();
  
  // Set viewports for responsive check
  const viewports = [
    { name: 'Desktop_1080p', width: 1920, height: 1080 },
    { name: 'Mobile_iPhone12', width: 390, height: 844 }
  ];

  for (const vp of viewports) {
    await page.setViewportSize({ width: vp.width, height: vp.height });
    await page.goto(TARGET_URL, { waitUntil: 'networkidle' });
    
    // Take a screenshot
    const screenshotPath = `audit-screenshot-${vp.name}.png`;
    await page.screenshot({ path: screenshotPath, fullPage: true });
    console.log(`📸 Saved screenshot for review: ${screenshotPath}`);
  }

  await browser.close();
}
runAudit();
```

---

## 🌉 Use Case 3: Polyglot API Bridge Generator

### Business Context & Problem
In full-stack architectures (like ours, combining Express or PHP backends with Vue/React clients), changing a database column or endpoint input shape requires updating validation rules, model objects, TypeScript declaration files, and API service classes.

### The Agent Solution
An expert API Alignment Agent that scans backend route definitions and models, and automatically rewrites frontend API fetch service classes and matching TypeScript definitions in a single turn.

### System Prompt Blueprint
```text
You are the API Bridge Specialist. Your role is to bridge the gap between our backend model definitions and modern typed frontend code.

### Operational Directives:
1. When any backend route file (e.g., in `/repo-php/class/` or `/server.ts`) is modified, immediately scan the changes.
2. Update the corresponding frontend typed structures (e.g., in `/src/types/` or `/src/views/`).
3. Maintain zero-duplication: compile validation rules from structural definitions directly into reactive JS client validation loops.
4. Report any missing endpoints or schema discrepancies as structured diagnostic errors in your summary.
```

---

## 🩺 Use Case 4: Autonomous Linter-Fixer Loop

### Business Context & Problem
During quick development, small syntax mistakes, unused variables, and misplaced brace brackets can cause typescript compilation failures or trigger lint alerts. Manually fixing dozens of minor linter complaints block build systems.

### The Agent Solution
The agent executes code edits, triggers the project validation tools, analyzes compiler error lines, and applies surgical replacements to the specific files, repeating up to a safe execution threshold.

### Workspace Context Pattern
The agent uses its workspace tools directly:
1. `edit_file` / `multi_edit_file` -> Makes target code changes.
2. `lint_applet` -> Evaluates code quality and retrieves exact lines that violate rules.
3. Loops over compilation feedback, automatically repairing imports and syntax without asking for user hand-holding.

---

## 🌐 Use Case 5: AI-Powered Localization Agent

### Business Context & Problem
Adapting websites for multi-national audiences requires hunting for raw strings throughout component layouts, wrapping them with translation keys, and populating translation dictionaries (`en.json`, `fr.json`, `es.json`).

### The Agent Solution
An agent designed to crawl source code directories, identify hardcoded text values (excluding code syntax, variables, and tags), replace them with key mapping calls (e.g., `$t('welcome')`), and generate beautifully synchronized local translations files via real-time Gemini processing.

### Custom Script Pattern (`translate.ts`)
```typescript
import * as fs from 'fs';
import { GoogleGenAI } from '@google/genai';

const ai = new GoogleGenAI(); // Instantiated using internal secure variable key

async function translateContent(sourceText: string, targetLanguage: string) {
  const prompt = `Translate this UI text strictly into target language: "${targetLanguage}". Rely on idiomatic phrases, preserve original typography styling placeholders (like HTML brackets or interpolations). Return raw text only:\n\n${sourceText}`;
  
  const response = await ai.models.generateContent({
    model: 'gemini-2.5-flash',
    contents: prompt
  });
  
  return response.text.trim();
}
```

---

## 🗄️ Use Case 6: Database Seeder & Schema Sync

### Business Context & Problem
Testing pagination controls, filtering algorithms, and visual charts requires massive, realistic data collections that represent relational schemas (such as user entities matching post logs and payment structures). Writing manual seed SQL queries takes hours.

### The Agent Solution
An agent that crawls your database schema files (`database.ts`, SQLite, or Postgres definitions) and uses structured LLM outputs to generate millions of rows of perfect mock data records that maintain direct referential integrity.

---

## 📚 Best Practices for Activating Agents in AI Studio

To get high-fidelity results when interacting with these agent patterns inside your workspace:

1.  **Strict Scope Limits**: Provide the agent with precise input/output file boundaries so it does not waste tokens scanning unneeded logs or cache folders.
2.  **Explicit Verification**: Instruct the agent to run the compiler (`npm run build` or `compile_applet`) as its absolute final verification step to enforce type safety.
3.  **Use `.env.example` in Workspace**: Declare all external API keys there to instruct the AI to check for their presence, but make sure the real keys are injected securely and invisibly through the **AI Studio Settings** menu.
