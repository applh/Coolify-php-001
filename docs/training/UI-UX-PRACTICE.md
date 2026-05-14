# Practical UI/UX Design for Engineers

Bridge the gap between functional code and delightful user experiences.

## 1. User Experience (UX) Engineering
**Goal**: Design flows that minimize friction and maximize user success.

- **Lab 1: Information Architecture**
    - **Reference**: `repo-php/public/js/components/AdminSitesList.js`
    - **Task**: Map out the user journey for an admin looking to delete a site.
    - **Exercise**: Redesign the "Sites List" component to include "Quick Actions" that reduce the number of clicks required for common tasks.
- **Lab 2: Accessibility (a11y) Audit**
    - **Reference**: `src/App.vue`
    - **Task**: Use a screen reader or audit tool on the main dashboard.
    - **Exercise**: Add missing `aria-label` attributes and ensure keyboard navigation works for all interactive buttons.

## 2. User Interface (UI) Craftsmanship
**Goal**: Create visually distinctive interfaces using modern design systems.

- **Lab 1: Visual Hierarchy & Spacing**
    - **Reference**: `repo-php/views/components/ArticleCard.php`
    - **Task**: Analyze the padding and margins.
    - **Exercise**: Apply the "8pt Grid System" to this component, ensuring all spacing increments are multiples of 8. Use Tailwind utility classes like `p-4`, `m-8`, etc.
- **Lab 2: Typography & Color Theory**
    - **Reference**: `src/style.css`
    - **Task**: Look at the font pairings.
    - **Exercise**: Implement a "High Contrast" theme for the application, selecting a pairing of fonts that improves readability for users with visual impairments.
- **Lab 3: Micro-Animations**
    - **Reference**: `public/js/script.js`
    - **Task**: Notice the transitions when components load.
    - **Exercise**: Add a subtle "hover scale" and "smooth entry" animation to all cards in the dashboard using Tailwind's `transition`, `transform`, and `duration` classes.

## Quality Standards
Always verify your designs using the [frontend-design](../../skills/system_skills/design_guidelines/SKILL.md) skill guidelines.
