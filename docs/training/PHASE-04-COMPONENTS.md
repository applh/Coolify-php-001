# Phase 4: Component Systems & Design Integration (100 Hours)

Build reusable, accessible, and high-performance component libraries.

## Modules (10 Hours Each)
- M031: Atomic Design Principles
- M032: Shadcn UI & Community Registries
- M033: Custom Component Libraries
- M034: Advanced React Patterns
- M035: Advanced Vue Patterns
- M036: UI States & Edge Cases
- M037: Design-to-Code Workflows
- M038: Dynamic Theming
- M039: Chart & Data Visualization (D3)
- M040: Project: Component Library

## Practical Labs

### 1. Atomic Component Design (40h)
**Reference**: `repo-php/public/js/components/` (CmsButton, CmsCard, etc.)
- **Task**: Study the plain JS implementation of UI components.
- **Exercise**: Create a Vue version of `CmsModal.js` in `src/components`.

### 2. Data Visualization (30h)
**Reference**: `src/views/SiteBenchmarker.vue`
- **Task**: See how Recharts or D3 is used to plot performance.
- **Exercise**: Add a custom chart to the Site Dashboard.

### 3. Evolutionary Component Refactoring (30h) 🍓 NEW
**Goal**: Learn how to transition from monolithic views to an atomic design system with polished UX.
**Reference**: `/src/components/`, `src/App.vue`, `src/views/SiteDashboard.vue`
**Exercise**: 
- **Step 1**: Analyze the abstraction of `BaseCard.vue` and `BaseButton.vue`.
- **Step 2**: Implement a `BaseInput.vue` component that supports validation states and clearable actions.
- **Step 3**: Replace the raw inputs in `SiteExplorer.vue` with your new `BaseInput.vue`.
**Complexity**: Phase 2 (Design & Reactivity)

## Recommended Reading
- `docs/frontend/admin-component-reference.md`
