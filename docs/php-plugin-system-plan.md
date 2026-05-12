# PHP CMS Plugin System Implementation Plan

This document outlines the upgrade to the PHP CMS framework to support a per-site plugin architecture.

## 1. Directory Structure

New directory for plugins:
```text
/repo-php/plugins/
├── plugin-a/
│   └── plugin.php      # Main entry point
└── plugin-b/
    └── plugin.php
```

Each site can now have a configuration file:
```text
/repo-php/content/site1.com/config.json
```
Example `config.json`:
```json
{
    "active_plugins": ["google-analytics", "custom-sidebar"]
}
```

## 2. Plugin Manager (`/repo-php/class/PluginManager.php`)

A core class responsible for:
- Discovering available plugins.
- Loading active plugins for the current site.
- Providing a Hook system (Actions and Filters).

### Hook System
- `PluginManager::addAction($hook, $callback)`: Register a function to run at a specific point.
- `PluginManager::doAction($hook, ...$args)`: Trigger all functions registered to a hook.
- `PluginManager::addFilter($hook, $callback)`: Register a function to modify data.
- `PluginManager::applyFilters($hook, $value, ...$args)`: Run data through all registered filters.

## 3. Integration Lifecycle

1. **Resolution**: `Router::dispatch()` identifies the active site.
2. **Initialization**: `PluginManager::init($siteDir)` is called.
3. **Loading**: `PluginManager` reads `config.json` and `include_once` the `plugin.php` of each active plugin.
4. **Execution**: Site templates (`index.php`) call `PluginManager::doAction('head')`, `PluginManager::doAction('footer')`, etc.

## 4. Example Plugins

- **SEO Optimizer**: Filter to modify the `<title>` tag.
- **Analytics**: Action to inject script tags in the footer.
- **Dark Mode**: Action to inject CSS in the head.

## 5. Security Considerations

- Plugin paths must be strictly controlled to prevent directory traversal.
- Plugins are executed with the same permissions as the CMS.
- Activation is restricted to site-specific configurations.
