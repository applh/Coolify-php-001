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

## 4. Multi-Domain Routing
In production, your external domains should be pointed to your server. The PHP router (`repo-php/public/index.php`) matches the `Host` header to the folders in `repo-php/content/`.

Example:
- Request for `site1.com` -> serves `repo-php/content/site1.com/index.php`
- Request for `localhost:3000` -> default entry point.
