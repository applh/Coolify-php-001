# PHP Template Hierarchy Building Guide

This document describes the hierarchy-based templating system implemented in the PHP CMS. This system allows building complex pages by layering templates (levels) and injecting content from the inside out.

## 1. The "Level Hierarchy" Concept

Instead of a flat `header` -> `content` -> `footer` approach, we use a recursive "Wrapping" strategy. Each level is responsible for its own structure and provides a "slot" (or hole) where the next level will be inserted.

### The Hierarchy Stack
*   **Level 0 (Shell)**: The outermost container (`<html>`, `<head>`, `<body>`). Global scripts and CSS.
*   **Level 1 (Layout)**: The structural arrangement (Sidebar + Main, Grid, or Single Column).
*   **Level 2 (View)**: The specific page content (Article, Dashboard, Gallery).
*   **Level 3 (Fragment/Component)**: Small reusable units inside the view.

## 2. Drawing the Hierarchy

```
[ Level 0: Shell ]
    [ Level 1: Layout ]
        [ Level 2: View ]
            [ Level 3: Components ]
```

## 3. Implementation: "Build then Insert"

In PHP, we achieve this by capturing the output of each level and passing it to the parent level as a variable (usually `$slot`).

### Example Workflow

1.  **Process Level 2 (View)**:
    Render the article content. Save result to `$content`.
2.  **Process Level 1 (Layout)**:
    Pass `$content` as `$slot` to the `PostLayout.php`. Save result to `$content`.
3.  **Process Level 0 (Shell)**:
    Pass `$content` as `$slot` to `MainShell.php`. Output to browser.

## 4. Usage with `Hierarchy` Class

We provide a `Hierarchy` class to manage this stack cleanly.

```php
$page = new Hierarchy();

// Define the levels from OUTSIDE to INSIDE
$page->wrap('shell', ['title' => 'My Blog']);
$page->wrap('layouts/two-column', ['sidebar' => true]);
$page->wrap('views/article', ['post_id' => 123]);

// Render (starts from the inside and wraps outwards)
echo $page->render();
```

## 5. Technical Benefits

1.  **Isolation**: Level 2 doesn't need to know about the Shell. It only knows it's a View.
2.  **Reusability**: You can swap the Shell (e.g., for a "Print Shell" or "Minimal Shell") without changing the Layouts or Views.
3.  **Clean Controllers**: The controller logic simply defines the stack, rather than calling `header()` and `footer()` manually.
4.  **AI Compatibility**: The "Level" structure is highly predictable. An AI agent can easily identify which file to edit based on whether the change is structural (Layout) or content-specific (View).

## 6. Directory Structure

```text
views/
├── shell.php          # Level 0
├── layouts/           # Level 1
│   ├── basic.php
│   └── sidebar.php
└── views/             # Level 2
    ├── home.php
    └── post.php
```
