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

## 6. MVC Design Pattern Evaluation

### Current Alignment
The Component system integrates seamlessly into the **MVC (Model-View-Controller)** pattern:
- **Model**: Responsible for data fetching and business logic.
- **Controller**: Orchestrates the data flow and chooses the top-level view.
- **View (`Component`)**: Handles the visual representation. Components act as "Micro-Views" that consume data passed from the Controller.

### The "Store" Pattern for Cross-Component State
To avoid **Prop Drilling** (passing data through multiple nested layers), we've implemented a **PHP Store** (`repo-php/class/Store.php`).

#### Benefits for AI Agents and Humans:
1.  **Deterministic Data Flow**: Instead of components guessing which variables are available in their scope, they can rely on `Store::get('user')` or expect global keys to be automatically extracted into their scope.
2.  **Single Source of Truth**: Global metadata (site name, SEO config, user login state) is managed in one place.
3.  **Encapsulation**: Components become more "Logic-Lite" as they pull what they need from the store rather than requiring complex constructor/prop injections.
4.  **AI Optimization**: AI Agents are highly productive with central stores because they can reason about the "Global State" as a single object, rather than tracking variable mutations across 10 different include files.

## 7. Roadmap Update

-   [x] **Part 1**: Create `repo-php/class/Component.php` with basic prop and slot support.
-   [x] **Part 2**: Implement `repo-php/class/Store.php` for global state management.
-   [ ] **Part 3**: Update `Layout.php` to use the new Component system and Store.
-   [x] **Part 4**: Implement the `/api/render-component` endpoint and client-side "async" loader.
-   [x] **Part 5**: Migrate `babiblog.fr` or `sambazen.net` to the new hierarchy.
