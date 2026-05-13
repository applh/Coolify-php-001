# PHP Plugin System Guide

The PHP CMS features a lightweight, per-site plugin architecture that allows for modular extensions without modifying the core framework.

## 1. Directory Structure

Plugins are stored in the `/repo-php/plugins/` directory:
- Each subdirectory contains a `plugin.php` entry point.
- Plugins can hook into global lifecycle events using the `PluginManager`.

## 2. Activation
Plugins are enabled on a per-site basis via a `config.json` file located in the site's root directory (e.g., `repo-php/content/site1.com/config.json`).

```json
{
    "active_plugins": ["analytics", "seo-optimizer", "forms"]
}
```

## 3. The Hook System (`PluginManager` Class)
The `PluginManager` provides a robust "Actions and Filters" system similar to WordPress:

### Actions (Event Hooks)
Actions allow you to inject code at specific points during the page execution.
- `PluginManager::addAction($hook, $callback)`: Register a listener.
- `PluginManager::doAction($hook, ...$args)`: Trigger a hook.

Common Hooks:
- `header_head`: Within the `<head>` tag.
- `footer_scripts`: Before the closing `</body>` tag.

### Filters (Data Hooks)
Filters allow you to modify data before it is displayed or processed.
- `PluginManager::addFilter($hook, $callback)`: Register a filter.
- `PluginManager::applyFilters($hook, $value, ...$args)`: Modify and return the value.

## 4. Built-in Plugins
- **Analytics**: Automatically injects tracking scripts based on site-specific IDs.
- **SEO Optimizer**: Dynamically modifies meta tags and titles.
- **Forms**: Handles form rendering and submissions per site.
