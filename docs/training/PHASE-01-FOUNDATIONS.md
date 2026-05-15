# Phase 1: Web Foundations (100 Hours)

This phase focuses on the "classic" web stack. You will learn to build responsive landing pages and simple dynamic sites using PHP.

## Modules (10 Hours Each)
- M001: Computer & Internet Fundamentals
- M002: HTML5 Semantic Web
- M003: CSS3 Foundations & Bio-Design
- M004: Vanilla JavaScript ES6+
- M005: PHP Basics & Scripting
- M006: SQL & Relational Knowledge
- M007: HTTP, DNS & Web Servers
- M008: Git & Version Control
- M009: Technical Glossary
- M010: Software Engineering Fundamentals

## Practical Labs

### 1. The Multi-Tenant File Header (15h)
**Reference**: `repo-php/content/site1.com/index.php`
- **Task**: Identify how individual websites are separated by folder.
- **Exercise**: Create `repo-php/content/mysite.com/index.php` and use the common header.
- **Goal**: Understand the directory-based multi-tenancy.

### 2. Static to Dynamic (20h)
**Reference**: `repo-php/public/index.php`
- **Task**: Wrap a static HTML page in a PHP `include`.
- **Exercise**: Create a basic `header.php` and `footer.php` and include them in multiple pages.

### 3. Glossary Mastery (10h)
**Reference**: `docs/training/slides/glossary/`
- **Task**: Study the technical terms.
- **Exercise**: Pass the M009 module quiz.

### 4. DRY & Refactoring Mastery (10h)
**Reference**: `docs/training/slides/phase1/dry-refactoring-best-practices.json`
- **Task**: Master the "Rule of Three" and "Extraction" techniques.
- **Exercise**: Refactor a repetitive block of code in `repo-php/content/site1.com/index.php`.
- **Goal**: Achieve clean, non-repetitive code.

### 5. Polyglot File Explorer (10h)
**Reference**: `docs/training/slides/phase1/file-formats.json`
- **Goal**: Understand the diversity of file formats in modern full-stack development.
- **Task**: Explore different repository folders (`repo-*`) and identify unique file extensions used in each stack.
- **Exercise**: Create a new configuration file in `repo-python/` using the `.json` format to define a mock site property.
- **Complexity**: Phase 1

### 6. Ecosystem Explorer (10h)
**Reference**: `docs/training/slides/phase1/software-engineering-fundamentals.json`
- **Goal**: Differentiate between libraries and frameworks while understanding open-source structures.
- **Task**: Identify 3 external libraries used in the root `package.json` vs 3 internal classes in `repo-php/class/`.
- **Exercise**: Locate a file in `repo-react` that demonstrates "Inversion of Control" (Framework behavior) vs a file in `repo-php` that calls a utility (Library behavior).
- **Complexity**: Phase 1

## Recommended Reading
- `docs/backend/php-mvc-architecture.md`
- `README.md`
