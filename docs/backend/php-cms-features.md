# PHP CMS Features Implementation

This document covers how core CMS features are implemented within the PHP application architecture using modern, class-based approaches.

## 1. Class Autoloading

To avoid explicit `require_once` statements for every utility class, the system uses an SPL (Standard PHP Library) autoloader registered at the very top of execution in `/repo-php/public/index.php`:

```php
// Autoloader for classes
spl_autoload_register(function ($class_name) use ($rootPath) {
    // Basic autoloader looking in the /class directory
    $file = $rootPath . '/class/' . str_replace('\\', '/', $class_name) . '.php';
    if (file_exists($file)) {
        require_once $file;
    }
});
```

This ensures any new classes added to `/repo-php/class/` (such as `CMS` or `Router`) are perfectly integrated and lazily loaded on first use.

## 2. Dynamic Routing (`Router` Class)

The multisite routing logic is fully encapsulated in the `Router` class (`/repo-php/class/Router.php`). On a request, the `public/index.php` simply passes the detected valid data path and dispatches:

```php
$router = new Router($contentPath);
$router->dispatch();
```

### Routing Logic
The `.php` routing execution flows through these priorities:
1. **Developer Override:** Uses `ACTIVE_SITE_OVERRIDE` if present.
2. **Domain Mapping Config:** Checks for `config.php` in the dataset path to see if the requested HTTP host explicitly maps to a target folder layout.
3. **Implicit Name Resolution:** Checks if the directory bearing the exact HTTP host exists.
4. **Fallback:** Defaults to displaying `site1.com`.

### Directory Isolation
A critical feature is `chdir($siteDir);`. When a site template starts execution, it assumes that it is the root of execution, not `public/index.php`. This allows templates to correctly reference internal files via pure relative paths (`include 'header.php'`).

## 3. Global Initialization and Health Checks (`CMS` Class)

Core environment functions are contained in the `CMS` class (`/repo-php/class/CMS.php`). This class is loaded automatically when invoked due to the autoloader. 

For instance, hitting `/?cms_debug=true` on the frontend URL performs an environment check by evaluating the static `validateSetup()` function in the `CMS` class:

```php
if (isset($_GET['cms_debug'])) {
    header('Content-Type: application/json');
    echo json_encode(CMS::validateSetup($contentPath));
    exit;
}
```

The validation asserts:
- Checks if the PHP version meets minimum CMS architecture requirements (7.4+).
- Checks if the core backend content directory string acts as a properly readable file mapping.
- Asserts presence of mapping configs.

## 4. Secure Admin Area and Passkey Authentication

The PHP CMS includes a built-in admin dashboard secured by an environment-based passkey (`APP_ADMIN_PASSKEY`).
- Accessing `/admin` routes requests to the `AdminRouter` (`/repo-php/class/AdminRouter.php`).
- It checks the passkey using headers or cookies.
- This area provides a lightweight, pure-PHP hosted management layer without needing the full Node.js dev environment.

## 5. Vue 3 Plain JS Architecture (No Node Build Step)

The Admin Area uses Vue 3, but is compiled entirely in the browser using the global Vue build from a CDN.
- Avoids Vite, Webpack, or Node.js environment requirements on the PHP server.
- **Async Components:** Vue's `defineAsyncComponent` and ES Modules (`<script type="module">`) load component JS files dynamically to split the code efficiently without a bundler.
- Full details available in `vue3-plain-js-components.md`.

## 6. ZIP Import & Export (Backup/Migrate)

Both the Node.js API and the PHP Admin API support full site export and import via `.zip` files.
- Users can download an entire site's code, templates, and media as a single `.zip` file from the Site Dashboard or the PHP Admin view.
- A ZIP archive can be uploaded to completely overwrite the `content/site.com` directory on the fly.
- This drastically simplifies migrations and backing up pure flat-file architectures.

## 7. Bypassing FTP and Databases (Vue Control)

The frontend Express CMS (and now the PHP Admin) writes files explicitly into either `content/` or `my-data/`. The flat-file architecture processes templates directly from whatever resides in that namespace dynamically. Updates sent from the Vue client are mapped identically: creating a markdown or template file immediately results in its live resolution via the `Router` execution.
