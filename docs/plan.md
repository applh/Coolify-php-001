# Implementation Plan: PHP CMS & Management Dashboard

This document outlines the roadmap for expanding the CMS capabilities, focusing on automated site creation and core PHP engine improvements.

---

## 1. Node.js CMS Dashboard: Site Management

### A. New Site Creation
- **Backend API:**
  - `POST /api/sites`: Creates a new subdirectory in `/repo-php/content/` or `/repo-php/my-data/`.
  - **Scaffolding Logic:** Automatically generates:
    - `index.php`: A basic responsive starter template using the global `Layout` class.
    - `img/`: Empty directory for assets.
- **Frontend UI:**
  - "New Site" Modal in `SiteDashboard.vue`.
  - Form validation to ensure valid domain-like folder names (e.g., `mysite.com`).

### B. Lifecycle Actions
- **Delete Site:** `DELETE /api/sites/:site` with safety confirmation.
- **Duplicate Site:** `POST /api/sites/:site/clone` to use an existing site as a template.
- **Bulk Media Processing:** A dedicated "Process All Pending Media" button in the AI Media View.

---

## 2. PHP CMS Engine: Core Improvements

### A. Component-Based Architecture
- **Shared Templates:** Create a `/repo-php/templates/` directory for global components (Header, Footer, Analytics).
- **Layout Class:** A new `repo-php/class/Layout.php` helper to inject shared CSS/JS and wrap site content.

### B. Enhanced Data Handling
- **Block-Based Content:** Move away from pure flat-file PHP to a JSON-based structure in `content/domain/data.json` for easier management by the Node API.
- **SEO Helper:** A standard PHP class to manage Meta tags, OpenGraph data, and dynamic page titles across all sites.

---

## 3. Scalable Site Management (1-100 Pages)

To handle larger websites efficiently, the CMS must evolve from single-file structures to a dynamic, template-driven model.

### A. Dynamic PHP Routing & Entry Point
- **Standardized `index.php`:** Every site will use a minimal standard router that:
  - Detects the requested path (e.g., `/contact`, `/services/web-design`).
  - Matches the path against a `pages.json` registry.
  - Loads the content and applies the designated **Template**.
- **Template Discovery:** The engine will look for templates in `repo-php/templates/` first (global) then fall back to site-specific templates.

### B. Content & Components
- **Block-Based Storage:** Page content stored as a JSON array of "Components":
  ```json
  [
    { "type": "Hero", "data": { "title": "Welcome", "image": "img/hero.jpg" } },
    { "type": "FeatureGrid", "data": { "items": [...] } }
  ]
  ```
- **Shared Component Library:** Pre-built PHP components located in `/repo-php/class/Components/` (e.g., `Hero.php`, `PricingTable.php`).
- **Data Isolation:** Each site maintains its own `data/` folder for site-specific JSON content, separating structure from logic.

### C. Node CMS: Multi-Page Operations
- **Page List View:** A management screen per site to see all 1-100 pages, their status (Draft/Live), and URL paths.
- **Visual Block Editor:** A visual interface in Node.js to reorder, add, and configure the JSON-based components for any given page.
- **Bulk Actions:** Ability to update SEO settings or change templates across dozens of pages at once.

---

## 4. AI Chat Copilot: Management via Natural Language

To mimic the "AI Studio" experience within the CMS Dashboard, we will implement a dedicated Chat Panel.

### A. Core Architecture
- **Stateful Conversation:** Use the `ChatSession` from the Gemini SDK on the backend to maintain context of the current management task.
- **Context Injection:** On every request, the backend will inject the current `site` structure, `repo-php` architecture, and existing file lists into the system prompt.
- **Function Calling:** Implement Gemini Function Calling to allow the AI to perform real actions:
    - `list_sites()`: Scans the content directory.
    - `edit_site_content(site, file, content)`: Directly modifies `index.php` or other site files.
    - `generate_site(name)`: Triggers the site creation scaffolding.

### B. Use Cases
- **"Optimize site1.com for SEO"**: The agent analyzes the code and suggests/applies changes to Meta tags.
- **"Create a new dark-themed site for a photography portfolio"**: The agent calls `createSite` and then updates the `index.php` with appropriate styling.
- **"Change the primary color of all sites to #F27D26"**: The agent iterates through all sites using a global script/function.

### C. Frontend Integration
- **Persistent Sidebar:** A sliding "CMS Copilot" panel available on all views.
- **Streaming UI:** Real-time markdown rendering and action confirmations (e.g., "I've created the site, click here to view it").

---

## 5. Architectural Roadmap

| Phase | Milestone | Priority |
| :--- | :--- | :--- |
| **Phase 1** | Implement `POST /api/sites` and "Add Site" UI | High [DONE] |
| **Phase 2** | Abstract Shared Components in PHP `class/` | Medium [DONE] |
| **Phase 3** | Implement Site Deletion and Domain Mapping UI | High [DONE] |
| **Phase 4** | Multi-Stack Support (React, Vue, Python FastAPI) | High [DONE] |
| **Phase 5** | Exhaustive Deployment & Rebuild Documentation | High [DONE] |
| **Phase 6** | Gemini-powered AI Chat Copilot Integration | High |
| **Phase 7** | Multi-Page Routing & JSON Content Schema | High |
| **Phase 8** | Component-based visual block editor | Medium |
| **Phase 9** | Bulk publishing & SEO optimization tools | Low |
| **Phase 10** | **Git Sync Feature (Remote Persistence)** | High |
