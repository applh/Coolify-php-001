# Multi-Site Flat-File Markdown CMS Implementation Plan

## Overview
A lightweight, PHP-based CMS that allows managing multiple websites from a single codebase using only Markdown files. No database required.

## Core Features
- **Multi-domain Support**: Map different incoming hostnames to specific content directories.
- **Flat-File Based**: Content is stored as `.md` files.
- **Markdown Parsing**: Automatic conversion of Markdown to HTML.
- **Customizable Themes**: Per-site or shared themes.
- **Dynamic Routing**: Automatic routes based on file structure.

## Proposed Directory Structure
```
/
├── content/
│   ├── site1.com/
│   │   ├── index.md
│   │   ├── about.md
│   │   └── config.json
│   └── site2.io/
│       ├── index.md
│       └── config.json
├── public/
│   └── index.php (Entry point)
├── src/
│   ├── Router.php
│   ├── SiteManager.php
│   └── MarkdownParser.php
├── themes/
│   └── default/
│       ├── header.php
│       └── footer.php
```

## Implementation Steps

### Phase 1: Site Resolution (Domain Mapping)
- Detect `$_SERVER['HTTP_HOST']`.
- **Active Site Override**: Support `ACTIVE_SITE_OVERRIDE` environment variable for local development and debugging (bypasses domain check).
- Match host (or override) against the `content/` subdirectories.
- Load site-specific configuration (title, theme settings).

### Phase 2: Routing & Page Loading
- Parse the request URI (e.g., `/about`).
- Search for the corresponding `.md` file in the site's content directory (e.g., `content/site1.com/about.md`).
- Handle 404s for missing files.

### Phase 3: Markdown to HTML
- integrate a library like **Parsedown** via Composer.
- Process Markdown content.
- Support YAML Front-matter for page-specific metadata (Title, Layout, Description).

### Phase 4: Theme Engine
- Create a simple layout system.
- Inject the parsed HTML into the selected theme.

### Phase 5: Multi-Site Specific Configurations
- Allow each site directory to have its own `assets/` (images, CSS).
- Path resolution helpers for shared vs local assets.

## Advantages for Coolify
- **Easy Backups**: Since everything is a file, `rsync` or `git` is enough for backups.
- **GitOps**: Content changes can be deployed via simple git pushes.
- **Low Resource Usage**: Minimal memory footprint as there is no DB overhead.
