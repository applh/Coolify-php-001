# React CMS Implementation

This implementation replicates the multi-domain logic of the original PHP CMS using a modern React SPA approach.

## Key Components
- `src/sites.json`: Configuration-as-code. Defines domain mappings, themes, and content blocks.
- `src/App.tsx`: The "Engine". Detects the hostname on mount and resolves the active site.
- `src/style.css`: Tailwind CSS entry point.

## Multi-Domain Logic
```tsx
const host = window.location.hostname;
const site = sitesData.find(s => s.domain === host) || sitesData[0];
```

## UI Approach
Uses a modular section system (Hero, Features, Footer) that adapts visually based on the site's `theme` config.
