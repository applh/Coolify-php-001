# Phase 3: Backend Systems & Polyglot Development (275 Hours)

Explore different ways to build the "brain" of an application. You will work with PHP, Python, Go, and Rust.

## Learning Objectives
- MVC (Model-View-Controller) Architecture.
- Database design and ORMs.
- Concurrency and Performance in Go/Rust.
- API Design with Python (FastAPI/Flask).

## Practical Labs

### 1. The PHP MVC Engine (90h)
**Reference**: `repo-php/class/`
- **Task**: Trace a request from `public/index.php` through `Router.php` to a `Controller.php`.
- **Exercise**: Add a new method to `CMS.php` to fetch "Recent Posts" and display them.

### 2. Python for Data & APIs (60h)
**Reference**: `repo-python/main.py`
- **Task**: See how Python handles `sites.json`.
- **Exercise**: Create a new endpoint that returns the average "Benchmarking" score of all sites.

### 3. High-Performance Go/Rust (75h)
**Reference**: `repo-go/main.go` and `repo-rust/src/main.rs`
- **Task**: Compare the same logic (serving a site) in two different compiled languages.
- **Exercise**: Implement a simple "Hello World" API in `repo-rust` using the `axum` or `actix-web` crate.

### 4. Database & Persistence (50h)
**Reference**: `database.ts` and `repo-php/class/DB.php`
- **Task**: Understand how the app connects to storage.
- **Exercise**: Write a SQL migration to add a `tags` column to the sites table.

## Recommended Reading
- `docs/backend/php-mvc-architecture.md`
- `docs/backend/node-cms-features.md`
