# AI Agent Rules for Creating Sites

When generating or modifying sites within the `content/` directory, the AI agent must adhere to the following core principles:

## 1. Page Speed Score Optimal
- Minimize blocking scripts and CSS. Use `defer` or `async` for scripts where possible.
- Optimize images by relying on modern formats (e.g., WebP) and always include `loading="lazy"` on below-the-fold images.
- Keep DOM sizes reasonable and utilize semantic HTML to ensure lightweight rendering.
- Avoid bulky libraries when native browser features (like vanilla JS or CSS grids) suffice.

## 2. SEO (Search Engine Optimization)
- Output proper `<title>` and `<meta name="description">` tags on every page dynamically.
- Use a clear, semantic heading structure (`<h1>` to `<h6>`) without skipping levels.
- Always include descriptive `alt` text for images.
- Ensure the viewport is configured for mobile devices (`<meta name="viewport" content="width=device-width, initial-scale=1.0">`).

## 3. UX (User Experience)
- Implement mobile-first, responsive modern designs.
- Tailwind CSS is preferred for clean, consistent styling.
- Provide clear Call-To-Action (CTA) elements.
- Maintain sufficient color contrast (WCAG compliance) to ensure readability for everyone.
- Add subtle, purposeful animations or hover states for interactive feedback.
- Always include a navigation drawer on the left side of the layout.
- Always include a vertical floating menu bar on the right side of the layout.

## 4. Security
- Sanitize and validate all user inputs (if form handling or query params are used).
- Use `htmlspecialchars($var, ENT_QUOTES, 'UTF-8')` when outputting any variable to prevent XSS attacks.
- Ensure error reporting does not expose sensitive server paths or configurations in production (handled via `.env` or global PHP config).
- Do not expose configuration secrets in public client-side code.

## 5. Minimal Intervention & Scope Control
- **Stick to the Request:** Only modify the specific features, files, or lines requested by the user. 
- **No Unsolicited Refactors:** Do not rewrite existing logic for "cleanliness" or "best practices" unless it is directly causing the bug being fixed or is explicitly requested.
- **Environment Stability:** Do not add configuration files (like `.gitignore`), change deployment scripts (`package.json`), or modify core server behavior (like `isProd` logic) unless it is necessary to fulfill the user's specific task.
- **Keep it Simple:** Prefer the simplest solution over introducing new complexity, environmental variables, or architectural layers.
- **Verify before Action:** If a feature isn't broken, do not "fix" it. If you suspect a change is needed outside the requested scope, ask the user before proceeding.

## 6. Documentation Management
- **Prioritize `docs/`:** Always update and maintain documentation within the `docs/` directory.
- **Root Folder Cleanliness:** Avoid writing new documentation files in the root folder. The only exception is the `README.md` file. All other technical guides, architecture notes, or manuals must be placed in or migrated to the `docs/` folder.

## 7. Educational Integrity (Training Updates)
- **Automatic Lab Generation:** Whenever a new feature, plugin, or architectural change is implemented, the AI agent MUST append a new "Practical Lab" to the relevant file in `/docs/training/`.
- **Lab Content Requirements:** Each new lab must include:
    - **Goal**: What the student will learn.
    - **Reference**: The specific files/folders modified.
    - **Exercise**: A hands-on task to extend or debug the new feature.
    - **Complexity**: Assign it to Part 1 through 5 based on difficulty.
- **Student Prompting**: Always remind the user that a new training lab is available for the feature just developed.

## 8. Training Content Hierarchy (60k Slides)
- **Serialization**: Never create 1:1 files for slides. Use **JSON Manifests** (max 500 slides per file) to store slide metadata.
- **Grounding**: Ensure any new architectural training content includes a `codeReferences` array in its JSON entry pointing to the physical file in `repo-*`.

## 10. JavaScript Interaction Standards
When writing JavaScript for the frontend, the AI agent MUST:
- **No Native Dialogs**: Do NOT use `window.alert()`, `window.confirm()`, or `window.prompt()`. These are blocking and provide a poor user experience within an iFrame environment.
- **Custom UI Components**: Use custom modal, toast, or notification components for user feedback and confirmations.

## 11. Explicative AI Agent Actions
- **Transparency**: When performing debugging or modifying architecture, clearly explain the actions being taken and why.
- **Contextualize Problems**: If an error occurs (such as a 404 or `MissingFieldException` from an API), identify the root cause (e.g., incorrect model version in SDK) in your explanation instead of silently fixing it.
- **Differentiate Environments**: Always explicitly clarify constraints (such as AI Studio environment requirements versus standard deployed builds, or Android versus web behavior) when they influence technical decisions.

## 12. Continuous Architecture Documentation
- **Keep Documentation Updated**: Whenever a new architectural choice is made or discovered (e.g., how the frontend communicates with APIs, proxy server requirements, routing mechanisms, or framework boundaries), update the appropriate architecture documentation in the `docs/` folder (such as `docs/architecture-decisions.md` or related files).
- **Justify Choices**: Record the "Why" and the "Context" behind major decisions so that other developers (and future AI agents) understand the constraints that forced a specific implementation.

## 13. API & SDK Upgrade and Verification Standards
When implementing or maintaining third-party integrations, external libraries, REST APIs, or AI Models (like Gemini SDKs), the AI agent MUST adhere to these operational directives:
- **Proactive Active Model / Call Discovery**: Never endlessly loop on an API error (e.g., `404 Not Found`, invalid methods, or schema mismatches). At the first sign of registration/routing failures, construct a temporary verification check or query the API capabilities endpoint (for example, call `ModelService.ListModels` equivalent or standard GET query `v1beta/models?key=...`) to determine the list of available active models, methods, and configurations in the current environment.
- **Always Keep SDKs & API Schemes Up to Date**: Prioritize using the latest official, non-deprecated APIs, client SDK models (e.g., upgrading from ancestral `gemini-1.5-flash` to the modern confirmed `gemini-2.5-flash` alias), and modern syntax rather than lagging on outdated documentation. Avoid using stale parameters or unneeded API paths.
- **Version Reporting**: The AI agent MUST always verify the latest stable release for any dependency. The AI agent MUST show a report to users about the chosen version of a library or SDK among other available versions to confirm it is not using outdated packages.
- **Diagnostic Transparency**: Share the valid capabilities, active model catalogs, endpoints, or API schemas discovered during the verification process. Render this diagnostic information in your explanations to empower developers/humans to understand the configuration state and support rapid, informed debugging.

## 14. Deprecated Code Avoidance
- **Never Use Deprecated Code**: AI agents must strictly avoid utilizing deprecated API calls, libraries, or methods. 
- **Modern Replacements**: Always research and implement the modern, supported equivalent. For instance, in Jetpack Compose, use `Icons.AutoMirrored.Filled` for directional icons rather than their deprecated `Icons.Filled` counter-parts.

## 15. Real-Time Repository Compaction Rules (ZIP Workaround)
Whenever the AI agent successfully edits, creates, or deletes any files contained inside a `repo-*` directory (e.g., modifying react source lines, adding go paths, or expanding python variables), the agent **MUST** execute the local packaging routine before finishing the turn:
- **Action**: Run the command: `npm run zip-repos`
- **Goal**: Recompile and sync-stage `archive.zip` inside modified repo folder scopes instantly.
- **Verification**: Ensure the command runs completely without syntax blocks. Do NOT skip this step; missing zips can break remote builds on Coolify.
- **Log Outputs**: Confirm dynamic terminal compilations in summaries so humans and deploy engines are fully synchronized.

