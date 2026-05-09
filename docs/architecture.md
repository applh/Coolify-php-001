# Architecture Overview

This CMS is built as a lightweight, flat-file multi-site engine.

## Core Components

### 1. SiteManager (`/src/SiteManager.php`)
- **Resolution**: Determines which site folder in `/content/` to use based on `HTTP_HOST` or the `ACTIVE_SITE_OVERRIDE` environment variable.
- **Configuration**: Loads the site-specific `config.json` containing the site title, theme, and description.

### 2. Router (`/src/Router.php`)
- **URI Mapping**: Takes the request URI (e.g., `/contact`) and maps it to a Markdown file in the active site's folder (e.g., `/content/mysite.com/contact.md`).
- **Index Handling**: Automatically resolves `/` to `index.md`.

### 3. MarkdownParser (`/src/MarkdownParser.php`)
- **Front Matter**: Extracts YAML-like metadata (Title, Description) from the top of the `.md` files.
- **Conversion**: Converts Markdown syntax into HTML. It is designed to work seamlessly with `erusev/parsedown`.

### 4. Theme Engine (`/themes/`)
- Minimalist PHP layouts. The `layout.php` receives variables like `$title`, `$content`, and `$siteName` to wrap the parsed HTML.

## Data Flow
1. Request hits `public/index.php`.
2. `SiteManager` identifies the target directory in `content/`.
3. `Router` finds the correct `.md` file.
4. `MarkdownParser` transforms content and metadata.
5. `layout.php` is included to render the final HTML.
