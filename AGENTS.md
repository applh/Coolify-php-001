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
    - **Complexity**: Assign it to Phase 1 through 5 based on difficulty.
- **Student Prompting**: Always remind the user that a new training lab is available for the feature just developed.
