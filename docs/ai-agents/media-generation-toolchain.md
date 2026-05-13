# Media Generation Toolchain & Architecture

This document explains the end-to-end workflow for generating, syncing, and serving media (images) within the CMS, and clarifies the design choices—specifically the use of `.zip` files as a wrapper for binary media assets.

## The Problem: Why the "Crazy Workaround" (.zip Wrappers)?

In this environment, tracking, exporting, and persisting raw, dynamically generated binary image files (like `.jpg`, `.png`, and `.webp`) using standard Git commits can lead to two main issues:
1. **Binary Tracking & Corruption Risk:** Raw generated binaries can sometimes get corrupted or ignored depending on the virtualized file system rules, or run into size and Git LFS boundaries.
2. **Export Synchronization:** The platform syncs standard text/archive nodes perfectly. By wrapping the images in individual `.zip` archives, the binary payload is cleanly packaged. This safely guarantees that the AI-generated images are persisted, version-controlled, and can be transported when exporting the project.

By using **empty `.zip` files as placeholders**, we can proactively track images that need to be generated without breaking file explorers or corrupting image readers, and easily run node scripts to locate these placeholders to build our generation queue.

---

## The Complete Toolchain Flow

### 1. Discovery and Placeholder Creation (Node.js)
When the "Rescan Media" action is triggered, the Node.js server (`server.ts` -> `/api/scan-media` endpoint) walks the `repo-php/content/` directory scanning `.php`, `.css`, and `.html` files.
- It looks for image paths (e.g., `<img src="/img/hero.jpg">`).
- If neither the binary image `img/hero.jpg` nor its wrapper `img/hero.jpg.zip` exists, it creates an **empty** `img/hero.jpg.zip` file.
- It then adds a task to the `media_tasks` SQLite table assigning the prompt `"Create an image for hero.jpg"`.

### 2. AI Generation (Vue.js to Gemini)
The `AiMediaTasks.vue` dashboard queries the SQLite queue.
- Users trigger a generation task. The Vue component calls the `ai.models.generateContent` SDK to ask the Gemini model to create the image.
- **Output:** The Gemini API returns the generated image payload as a **Base64 encoded string**.

### 3. Packaging & Saving (Node.js via AdmZip)
The Base64 string is POST-ed to the server endpoint `/api/media/save-base64`.
- **Base64 Decode:** The Node server decodes the Base64 string directly into a raw binary `Buffer`.
- **Zipping (The wrapper):** Using `adm-zip`, it writes that raw binary buffer into the archive under its original filename (e.g. `hero.jpg`). 
- **Storage:** It saves this archive over the previously empty placeholder as `hero.jpg.zip`.
- *Note:* The content saved inside the `.zip` archive is **real binary image format**, not Base64!

### 4. Serving the Image (PHP Router On-the-Fly Extract)
When a user navigates to the PHP site and their browser requests `/img/hero.jpg`, the request goes to the PHP application (`repo-php/class/Router.php`).
- **Absence Check:** PHP checks if `img/hero.jpg` exists. It doesn't (because we only saved the `.zip`).
- **Zip Fallback Check:** PHP then sees `img/hero.jpg.zip` exists.
- **Extraction:** Using the `ZipArchive` class, PHP opens the zip, targets the embedded `hero.jpg` file, and uses `$binary = $zip->getFromName()`.
- **Caching & Delivery:** 
  1. PHP writes that binary data out to `img/hero.jpg` natively on the filesystem so subsequent requests bypass the unzip process.
  2. It serves the binary directly back to the browser with the correct MIME type (e.g., `image/jpeg`).

---

## Critical Clarification: Binary vs Base64 inside the Zip

**The generator saves the images in the zip archive in REAL BINARY IMAGE FORMAT.**

The Node server explicitly handles the decoding:
```javascript
// Server.ts (/api/media/save-base64)
const buffer = Buffer.from(imageBase64, 'base64'); // <-- Converts back to binary
indivZip.addFile(path.basename(fullPath), buffer);   // <-- Packages binary byte array
```

As a result, **PHP ONLY needs to `unzip` the file.** 
No Base64 decoding (`base64_decode()`) is required in the PHP application layer. PHP merely reads the file from the zip archive natively and writes the buffer back out.

Because PHP relies on this extraction directly:
- `Dockerfile` requires OS packages: `libzip-dev` and `zip/unzip`.
- `Dockerfile` requires PHP extension: `docker-php-ext-install zip`. 

This combination of tools guarantees that generated media makes it to the persistent Git graph without disrupting the developer experience.

---

## Future Evolution: Site Content as a Single Zip Archive

The current setup uses individual `.zip` wrappers for each binary media file while keeping standard files in regular directories. A planned future evolution will transition this architecture to managing an entire site's content as a **single `.zip` archive**.

### Proposed Architecture Changes:

1. **Sites as Archives:** Instead of having a directory per site under `repo-php/content/` (e.g., `repo-php/content/site1.com/`), the entire site content will be stored as `repo-php/content/site1.com.zip`.
2. **Simplified Media Storage:** With the entire site stored as an archive wrapper, individual media images no longer need their own individual `.zip` files. They will simply exist as standard files (e.g., `img/hero.jpg`) *inside* the larger site-level zip archive.
3. **Node.js Virtual Exploration:** The Node.js application will implement a virtual filesystem layer powered by `adm-zip` (or similar) to explore and interact with the `.zip` archive as though it were a regular directory. The AI media scanning and task generation logic will run directly against this virtual structure.
4. **PHP Serving:** The PHP routing logic will be updated to extract on demand from the site archive, potentially maintaining a temporary or cached extract dir to maintain high performance.

This evolution will simplify binary handling at the file level while minimizing file explorer clutter and maintaining perfect synchronization properties.