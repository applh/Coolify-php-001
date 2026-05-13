# Multi-Stack CMS Manager - Setup & Troubleshooting

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
   Use the AI Studio Preview window to interact with the CMS Manager.

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

## 4. Troubleshooting

### "Publish is Broken" (Blank screen or 500 error)
1. **Check Build:** Ensure `dist/index.html` exists. If missing, the server will return a 500 error with instructions to run `npm run build`.
2. **Path Issues:** If script/style tags in the shared URL fail to load (404), ensure that the `dist` assets were generated correctly and that the platform is serving from the root domain.
3. **Multiple Servers:** If the preview feels "stuck" or shows old code, there might be orphaned Node.js processes.
   - **Solution:** Use the "Restart Dev Server" tool or manually kill processes (e.g., `pkill -f tsx`).

### Zip Restoration Errors
If you see "Zip entry list is empty" in logs, it means a restoration was attempted but the zip was malformed or empty. This is handled gracefully by skipping the restoration.

### "404 - Site Not Configured"
This originates from the PHP application (`repo-php`). It means the `Host` header reaching the PHP router doesn't match any folder in `content/` or `my-data/`.
- **Solution:** Check the `README.md` in `repo-php` for domain mapping instructions.

### AI Media Generation Issues
- **Tasks Stuck in 'Pending':** Ensure the AI Media Queue worker is triggered (scanning templates via the dashboard button).
- **Zips invalid:** If you see console errors about `ADM-ZIP: Invalid format`, the system automatically unlinks them and attempts to re-generate the media assets.

---

## 5. Deployment Recommendation
For a live production environment, we recommend:
1. Hosting the **Node.js CMS Manager** on a Node service (e.g., Cloud Run, Railway, Heroku).
2. Hosting the **PHP Multisite App** on a specialized PHP platform or a VPS using Docker (as provided in `repo-php/docker-compose.yml`).
