# Implementation Plan: PHP CMS & Management Dashboard

This document outlines the roadmap for expanding the CMS capabilities, focusing on automated site creation and core PHP engine improvements.

---

## 1. Node.js CMS Dashboard: Site Management

### A. New Site Creation
- **Backend API:**
  - `POST /api/sites`: Creates a new subdirectory in `/repo-php/content/` or `/repo-php/my-data/`.
  - **Scaffolding Logic:** Automatically generates:
    - `index.php`: A basic responsive starter template.
    - `img/`: Empty directory for assets.
    - `b64/`: Directory for base64 encoded data (if used).
- **Frontend UI:**
  - "New Site" Modal in `SiteDashboard.vue`.
  - Form validation to ensure valid domain-like folder names (e.g., `mysite.com`).

### B. Lifecycle Actions
- **Delete Site:** `DELETE /api/sites/:site` with safety confirmation.
- **Duplicate Site:** `POST /api/sites/:site/clone` to use an existing site as a template.
- **Bulk Media Processing:** A dedicated "Process All Pending Media" button in the AI Media View.

### C. Advanced Domain Mapping
- **UI for `config.php`:** Visual interface to map external domains to specific subfolders in the `content/` directory without manual PHP editing.

---

## 2. PHP CMS Engine: Core Improvements

### A. Component-Based Architecture
- **Shared Templates:** Create a `/repo-php/templates/` directory for global components (Header, Footer, Analytics).
- **Layout Class:** A new `repo-php/class/Layout.php` helper to inject shared CSS/JS and wrap site content.

### B. Enhanced Data Handling
- **Block-Based Content:** Move away from pure flat-file PHP to a JSON-based structure in `content/domain/data.json` for easier management by the Node API.
- **SEO Helper:** A standard PHP class to manage Meta tags, OpenGraph data, and dynamic page titles across all sites.

### C. Placeholder Refinement
- **Better SVG Placeholders:** Improve `Router.php` to generate placeholders with dimensions, background colors, and icon support based on the filename.

---

## 3. Site & Frontend Enhancements

### A. Modern Starter Templates
- New sites will include a standard `style.css` using CSS Variables for easy theming (Primary Color, Font Choice).
- Integrated support for the [Tailwind Play CDN](https://tailwindcss.com/docs/installation/play-cdn) for rapid development.

### B. AI-Driven Automation
- **Automatic Metatags:** Use Gemini to analyze `index.php` content and generate optimized SEO descriptions.
- **Sitemap Generation:** Automated generation of `sitemap.xml` for each multi-site folder.

---

## 4. Architectural Roadmap

| Phase | Milestone | Priority |
| :--- | :--- | :--- |
| **Phase 1** | Implement `POST /api/sites` and "Add Site" UI | High |
| **Phase 2** | Abstract Shared Components in PHP `class/` | Medium |
| **Phase 3** | Implement Site Deletion and Domain Mapping UI | Low |
| **Phase 4** | Gemini-powered SEO and content generation | Medium |
