# Vue 3 Plain JS & Async Components in PHP CMS

This document outlines the approach for integrating Vue 3 into the PHP CMS frontend. Because the PHP environment serves pages on the fly without a Node.js build step (like Vite or Webpack), we rely on **plain JS components** and **asynchronous component loading** via the global Vue build from a CDN.

## 1. The Environment setup
We do not use Single-File Components (`.vue` files) nor TypeScript on the PHP server side. Instead:
- We include Vue 3 via CDN.
- We use the global `Vue` object (`const { createApp, ref, defineAsyncComponent } = window.Vue`).
- We write our templates inside plain HTML strings or inside `<template>` tags if defined locally within the page.

### Example Base Setup in a PHP View
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Vue 3 in PHP</title>
    <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body>
    <div id="app">
        <!-- Vue component mounts here -->
        <my-counter></my-counter>
    </div>

    <script>
        const { createApp, ref } = Vue;

        const MyCounter = {
            setup() {
                const count = ref(0);
                return { count };
            },
            template: `
                <div class="p-4 border">
                    <p>Count: {{ count }}</p>
                    <button @click="count++" class="bg-blue-500 text-white px-2 py-1">Increment</button>
                </div>
            `
        };

        const app = createApp({});
        app.component('my-counter', MyCounter);
        app.mount('#app');
    </script>
</body>
</html>
```

## 2. Using Async Components
Loading everything into one giant JS file is not scalable. Vue 3's `defineAsyncComponent` allows us to load components only when they are needed.

To make it work without a bundler, we wrap our components in plain `.js` files that append their definitions to the global scope or return them via a Promise layout. A common pattern in a script-tag based architecture is using standard ES modules if the browser supports it, or dynamically fetching the JS text from an endpoint, but utilizing ES modules is easiest:

### Step 1: Component File (`/components/MyAsyncWidget.js`)
Use Export Default if `type="module"`, or assign to a global namespace like `window.Components.MyAsyncWidget` if not using modules.

Using standard ES modules:
```javascript
// /components/MyAsyncWidget.js
const { ref } = Vue;

export default {
    setup() {
        const message = ref('Hello from async component!');
        return { message };
    },
    template: `
        <div class="p-4 bg-gray-100 rounded">
            <h2 class="text-lg font-bold">Async Widget</h2>
            <p>{{ message }}</p>
        </div>
    `
};
```

### Step 2: Main Application File
When instantiating the app, load the component dynamically:

```html
<script type="module">
    import { createApp, defineAsyncComponent } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js';

    const app = createApp({
        template: `
            <div>
                <h1>Main Dashboard</h1>
                <!-- Renders the async component -->
                <AsyncWidget />
            </div>
        `
    });

    // Define the async component
    app.component(
        'AsyncWidget',
        defineAsyncComponent(() => import('./components/MyAsyncWidget.js'))
    );

    app.mount('#app');
</script>
```

**Note:** Notice we switched the CDN link to use `vue.esm-browser.js` so we can use `import { createApp }` along with native browser `import()` functionality. Native ES Modules (`<script type="module">`) are highly recommended because they allow `import()` boundaries naturally.

## 3. Best Practices
1. **Separation of Concerns:** Keep complex logic separated into pure JS files to make them testable. Template logic should be concise.
2. **Tailwind Styling:** Since we don't have scoped CSS built-in, rely entirely on Tailwind utility classes defined right on the elements within your template string.
3. **Template Formatting:** Use JS template literals (backticks) for multiline strings. Ensure your editor is set up to highlight HTML inside template literals if possible.
4. **Environment Variables:** PHP can inject configuration to the `window` object before loading Vue scripts.
   ```php
   <script>
       window.APP_CONFIG = {
           apiUrl: '<?= htmlspecialchars($apiUrl, ENT_QUOTES) ?>'
       };
   </script>
   ```

By using the ES module build of Vue and native browser imports, we get robust component-based architecture without requiring compiling, Vite, or a Node build pipeline at all.
