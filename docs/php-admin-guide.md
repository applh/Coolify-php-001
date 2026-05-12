# PHP Admin Area Guide

The PHP CMS includes a lightweight, built-in admin dashboard accessible via `/admin`. This provides a pure-PHP management layer for site templates and plugins.

## 1. Authentication
The Admin Area is secured by a passkey defined in your environment variables:
- **Variable**: `APP_ADMIN_PASSKEY`
- **Method**: The system checks this key against a secure cookie or header.

## 2. Features
- **Site Management**: List and inspect current site folders.
- **File Inspector**: View the raw file structure of each tenant.
- **Plugin Configuration**: Enable or disable plugins (Analytics, SEO, Forms).
- **Forms Manager**: View submissions and configure form fields for each site.

## 3. Architecture
The admin panel uses a **modular Vue 3 architecture** located at `/repo-php/class/AdminRouter.php` and `/repo-php/public/js/components/`.

- **Asynchronous Loading**: Views like `AdminSitesList` and `AdminAiTasks` are loaded on-the-demand using `defineAsyncComponent`.
- **Reusable Library**: A library of "Artful Minimalist" components (Buttons, Cards, Tables, Modals) ensures design consistency.
- **Portability**: This allows the admin interface to be "portable" and work in environments where only PHP is available, while still providing a modern SPA feel.

### Key Admin Views
- **Sites List**: Overview of all hosted domains with instant download/upload capabilities.
- **AI Media Queue**: Real-time monitoring of Gemini-powered background tasks.
- **Forms Manager**: Dynamic editor for JSON-based form schemas and submission browser.

## Accessing the Admin Area
To access the admin panel, append `/admin` to your site's URL. You will be prompted for your passkey if it is not already stored in your browser session.

## Developer Security Note
The PHP Admin and API strictly enforce path-traversal protections. All file operations are validated using `realpath()` and must reside within the authorized `content/` or `my-data/` directories.
