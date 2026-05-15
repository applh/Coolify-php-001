# Node CMS Architecture and Features

This document outlines the features of the Node.js (Express & Vite) CMS Management interface which powers the backend of the PHP CMS.

## 1. Hybrid Applet Structure
The application runs two distinct technical stacks in parallel:
- **PHP Flat-File Application:** Runs the actual frontend templates via standard PHP processing.
- **Node.js Express App:** Manages the development workflow, acts as an API for the Vue 3 dashboard, and handles file manipulation.

The Node.js server (`server.ts`) acts as the entry point during development in AI Studio. It runs Vite middleware to serve the Vue 3 dashboard frontend, while simultaneously providing REST APIs.

## 2. Dynamic File Editing (`/api/sites/`)
The main purpose of the Node CMS is to bypass FTP or database requirements. 
The API endpoints provide full CRUD operations against the `/repo-php/content/` (and `/repo-php/my-data/`) directories.
- Users can view sites, list directories, and edit files directly in their browser.
- Updates are instantly available to the PHP application since they read from the exact same filesystem.

## 3. SQLite Local Database
The Node server uses `better-sqlite3` to maintain a local `cms.db` database inside the project root.
- It is currently used for **AI Media Generation Task Queues**. It tracks which files need images generated, logs prompts, and tracks generation status (`pending`, `completed`, `failed`).
- It is completely isolated from the PHP application, serving solely as a management database for the Node environment.

## 4. ZIP Import & Export
To easily manage, backup, or deploy active sites, the Node.js API utilizes `adm-zip` to handle exporting and importing sites:
- **Download (`/api/sites/:site/download`):** Zips the targeted site directory (e.g., `content/mysite.com`) on the fly, streaming a `.zip` archive to the client browser.
- **Upload (`/api/sites/:site/upload`):** Accepts a standard `Multipart Form Data` upload (via Multer). Extracts the contents of the ZIP archive directly into the active content directory, instantly overwriting current files.

## 5. Media Wrapping and AI Task Sync
To solve the issue of binary files in virtual Git graphs, the Node server packages images as zip wrappers.
- The `server.ts` endpoint `/api/scan-media` combs through template files, builds AI image generation tasks, and inserts them into SQLite.
- The `server.ts` endpoint `/api/media/save-base64` accepts Base64 strings from the client (via Gemini generation), decodes them into raw Binary buffers, and zips them via `adm-zip` to be safely written to disk. The PHP application seamlessly extracts them on the fly.
