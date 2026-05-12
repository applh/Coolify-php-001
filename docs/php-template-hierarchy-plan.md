# Implementation Plan: PHP Component Hierarchy (Vue3 Inspired)

This document outlines the strategy for implementing a component-based templating system in the PHP CMS, drawing inspiration from **Vue 3's component model** and **async loading patterns**.

## 1. Architectural Vision

Currently, the PHP CMS uses a flat `include` or `View::render` approach. We want to transition to a **hierarchical, reusable component system** that supports:
-   **Isolated Scopes**: Variables shouldn't leak between components unless passed explicitly.
-   **Slots**: Providing "Named Slots" and "Default Slots" for layout flexibility.
-   **Deferred (Async) Fragments**: Identifying heavy components that should be rendered on the client via a follow-up AJAX request rather than blocking the initial page load.

## 2. Core Implementation: The `Component` Class

We will implement a new `Component` class in `repo-php/class/Component.php`.

### API Design
```php
Component::render('Card', [
    'title' => 'My Card',
    'variant' => 'primary'
], function() {
    // This closure acts as the "default slot"
    echo "<p>This is the content inside the card.</p>";
});
```

### Slot Support
To support named slots, we can use a collector pattern:
```php
Component::render('Layout', [], [
    'header' => function() { echo "<h1>Title</h1>"; },
    'footer' => function() { echo "<p>Copyright 2024</p>"; },
    'default' => function() { echo "Main content"; }
]);
```

## 3. "Async" Components in PHP

Since PHP is synchronous by nature, "Async" components will be handled by a two-stage strategy:

1.  **Server-Side Placeholder**: If a component is marked as `async`, the server renders a unique `<div>` with a data-component attribute and relevant props serialized.
2.  **Client-Side Hydration**: A minimal JS loader (part of `public/js/script.js`) detects these placeholders and makes a GET request to `/api/component-render?name=X&props=Y` which returns the HTML fragment.

### Benefit
This mimics the "Async Component" behavior of Vue 3 where the UI remains responsive and the heavy parts load as they become ready.

## 4. AI Agent Productivity Evaluation

### Why an AI Agent is more optimized for this:

1.  **Context Retention**: When building a hierarchy of 20+ components, a human developer often loses track of prop types or slot names across nested files. An AI agent (e.g., using Gemini 1.5 Pro) retains the entire component library schema in its 1M+ token window.
2.  **Pattern Enforcement**: The AI can strictly enforce the "Atmospheric Minimalist" design language (defined in `Layout.php`) by generating code that uses consistent Tailwind utility classes without manual copy-pasting.
3.  **Atomic Deployment**: Implementing a new templating system requires updating the `View` class, creating `Component.php`, and migrating existing site templates. An AI agent can perform these multi-file edits in a single coordinated action, ensuring the app never enters a broken state.
4.  **Schema Generation**: The AI can automatically generate JSON schemas for the component props, which can then be used by the Vue 3 dashboard to provide a "Visual Editor" experience for these PHP components.

## 5. Roadmap

-   [x] **Phase 1**: Create `repo-php/class/Component.php` with basic prop and slot support.
-   [ ] **Phase 2**: Update `Layout.php` to use the new Component system.
-   [x] **Phase 3**: Implement the `/api/render-component` endpoint and client-side "async" loader.
-   [x] **Phase 4**: Migrate `babiblog.fr` or `sambazen.net` to the new hierarchy as a proof of concept.
