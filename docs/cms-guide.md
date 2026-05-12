# Multi-Site CMS Management Guide

This CMS allows you to manage multiple independent PHP websites from a single Node.js dashboard.

## 1. Creating a New Site
1. Navigate to the **Site Dashboard** in the Node.js application.
2. Click **"+ Add New Site"**.
3. Enter a folder name (e.g., `mynewproject.com`).
4. The system will create:
   - A folder in `repo-php/content/`.
   - A boilerplate `index.php` utilizing our global `Layout` engine.
   - An `img/` directory for assets.

## 2. Using the PHP Framework
The CMS includes a lightweight PHP framework to ensure cross-site aesthetic consistency.

### The Layout Class
Instead of raw HTML, use the `Layout` class in your `index.php`:

```php
<?php
// Your page logic here
Layout::header("Project Name");
?>

<main>
    <h2 class="serif italic text-4xl">Artful Minimalism</h2>
    <p>Your content here...</p>
</main>

<?php
Layout::footer();
?>
```

### Key Classes & Assets
- **`.serif`**: Applies the *Instrument Serif* font (italic by default).
- **Tailwind CSS**: Pre-configured and available via CDN in the header.
- **`CMS::image($path)`**: Helper to safely reference images.

## 3. SEO & AI Media
- **AI Media Queue**: The dashboard scans your `index.php` files for image references (e.g., `src="/img/hero.jpg"`). If the file is missing, it appears in the **AI Media** view.
- **Processing**: Click "Run AI Generation" to have Gemini create optimized, context-aware placeholders or textures for your missing assets.

## 4. Admin Management Area
Beyond the Node.js dashboard, each PHP instance provides a secure `/admin` area for site-specific management.

- **Interactive Forms**: Create custom forms (Contact, Inquiry, etc.) without writing code. See [Admin Area Guide](./php-admin-guide.md).
- **Asset Management**: Upload and download site bundles as ZIP archives for easy migration.
- **Queue Monitoring**: Watch AI media tasks as they process in the background.

## 5. AI Copilot & Management
The CMS includes an AI-assisted management layer powered by Gemini. You can interact with the system using natural language to optimize SEO, generate content, or build new pages.

- **Developer Guide**: For details on how the AI integration works, see [Gemini API Interactions & Memory](./gemini-api-interactions.md).
- **Function Calling**: The AI can perform file operations and site management tasks.

## 6. Advanced: Data Persistence & Resets
When deploying to production (e.g., via Coolify), the application uses a persistent volume for your site data (`my-data`).
- **Persistence**: Your site changes are saved across deployments.
- **Forced Reset**: If you need to revert all sites to the fresh state defined in your repository, set the environment variable `APP_DATA_RESET="true"` and redeploy. This will overwrite `my-data` with the latest contents of the `content/` folder.
