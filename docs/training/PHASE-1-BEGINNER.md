# Phase 1: Web Fundamentals (175 Hours)

This phase focuses on the "classic" web stack. You will learn to build responsive landing pages and simple dynamic sites using PHP.

## Learning Objectives
- Master Semantic HTML and CSS Layouts (Flexbox/Grid).
- Understand Client-Server communication (The Request/Response loop).
- Learn PHP syntax and file inclusion patterns.

## Practical Labs

### 0. Technical Language Grounding (10h)
**Reference**: `docs/training/slides/glossary/`
- **Task**: Review the **[Full-Stack Technical Glossary](./TECHNICAL-GLOSSARY.md)**.
- **Goal**: Internalize the 60+ technical terms that define the FRAISE architecture. This 3,000-word glossary is your map for the entire 1,000-hour journey.

### 1. The Anatomy of a Landing Page (40h)
**Reference**: `repo-php/content/onepage.demo/index.php`
- **Task**: Clone this file and create `repo-php/content/my-first-site/index.php`. 
- **Exercise**: Modify the Tailwind classes to change the color scheme and typography.
- **Goal**: Understand how `App.php` routes these individual site folders.

### 2. Styling with CSS and Tailwind (50h)
**Reference**: `repo-php/public/css/style.css`
- **Task**: Analyze the existing CSS. 
- **Exercise**: Create a custom utility class and apply it to a component in `repo-php/views/components/Card.php`.

### 3. Basic PHP Scripting (40h)
**Reference**: `repo-php/content/sambazen.net/countries-data.php`
- **Task**: Study how PHP arrays are used to store data.
- **Exercise**: Create a new data file for "Products" and loop through it in a new view.

### 4. Form Handling (45h)
**Reference**: `repo-php/plugins/forms/`
- **Task**: Look at how forms are structured in `site1.com/forms/forms.json`.
- **Exercise**: Add a new field to the contact form and verify it appears in the UI.

## Recommended Reading
- [MDN Web Docs: HTML/CSS](https://developer.mozilla.org/)
- `docs/backend/php-template-hierarchy-guide.md`
