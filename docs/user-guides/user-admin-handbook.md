# Admin Handbook (User Guide)

Welcome to the PHP CMS Admin Area. This guide will help you manage your websites, forms, and AI-generated content efficiently.

## Getting Started
Access your admin panel at `[your-domain]/admin`. You will need your project's **Admin Passkey** to log in.

## 1. Managing Sites
The dashboard lists all your active websites.
- **Download**: Export your entire site (including images and configuration) as a ZIP file.
- **Upload**: Overwrite an existing site or restore from a backup by uploading a ZIP.
- **Forms**: Open the specialized Forms Manager for that specific site.

## 2. Interactive Forms Manager
You can create custom contact forms, lead captures, or surveys without touching any PHP code.

### Creating a Form
1. Click **Forms** on any site in the dashboard.
2. Click **"+ Create New Form"**.
3. Define your fields (Text, Email, Select, etc.).
4. Save the form. It will generate a unique **Form ID**.

### Adding to your Site
Once created, you can include the form in your `index.php` using the global ` CMS` helper:
```php
<?php echo CMS::form('your-form-slug'); ?>
```

### Viewing Submissions
Submissions are stored securely. Click **"Submissions"** on any form in the Admin Area to view recent entries or export them as JSON.

## 3. AI Media Queue
The AI Media Queue monitors your sites for missing images.
- **Heartbeat**: Click "Run Heartbeat" to trigger the processing of any pending tasks.
- **Automatic Optimization**: The AI doesn't just "fill in" an image; it generates content based on the context of your page (e.g., if it's a bakery site, it will generate bakery-themed assets).

## 4. Design Guidelines
We follow a **Dark-Mode, Artful Minimalist** aesthetic.
- High contrast (Black background, White/Orange accents).
- Serif fonts for headers (`Instrument Serif`).
- Monospace fonts for data and technical labels (`IBM Plex Mono`).
