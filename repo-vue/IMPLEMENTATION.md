# Vue CMS Implementation

This implementation uses Vue 3 and Vite to provide a reactive, multi-domain CMS experience.

## Key Components
- `src/sites.json`: Shared site data.
- `src/App.vue`: Handles reactivity and domain detection in the `onMounted` hook.
- `src/style.css`: Tailwind configuration.

## Advantages
- **Clean Templates**: Vue's template syntax makes it very easy to build the CMS UI with clear conditionals.
- **Performance**: Vite HMR makes development of multi-site themes extremely fast.

## Multi-Domain Logic
Detection is performed at the root layer, ensuring all child components receive the correct context immediately.
