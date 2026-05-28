# Part 5: Backend Mastery: PHP & CMS Architecture (100 Hours)

Deep dive into the heart of the CMS: PHP MVC, Plugins, and Database Design.

## Modules (10 Hours Each)
- M041: PHP MVC Deep Dive
- M042: CMS Core Design
- M043: Plugin System Engineering
- M044: Database Design & Normalization
- M045: Authentication & JWT
- M046: API Design (RESTful)
- M047: File Storage & Media Management
- M048: Backend Security Best Practices
- M049: Nginx for PHP-FPM
- M050: Project: Headless CMS

## Practical Labs

### 1. MVC Routing (40h)
**Reference**: `repo-php/class/Router.php`
- **Task**: Trace a request from entrypoint to controller.
- **Exercise**: Create a new route and controller method in the PHP backend.

### 2. Plugin Engineering (30h)
**Reference**: `repo-php/class/PluginManager.php`
- **Task**: Study the plugin lifecycle hooks.
- **Exercise**: Build a simple plugin that logs every request to a file.

### 3. Analytics & Security Logging (30h)
**Reference**: `/docs/backend/small-company-features-plan.md`
- **Goal**: Understand how to architect high-performance, lightweight SQLite data persistence, robot classification filters, and security event traps in a multi-tenant flat-file PHP CMS.
- **Exercise**: Implement the database schema initializer inside `/repo-php/class/DB.php` and log visitor transactions inside `/repo-php/plugins/analytics/plugin.php`. Write a mock test script to verify that automated crawler probes and broken URL paths register as security/analytic log metrics.
- **Complexity**: Part 5

### 4. Dynamic Multi-Driver Persistence (PostgreSQL & SQLite) (40h)
**Reference**: `/docs/backend/db-persistence-postgresql-plan.md`
- **Goal**: Master database driver isolation, environment-level runtime database selector adapters, schema variations translation, and lightweight Repository encapsulation patterns.
- **Exercise**: Rewrite `/repo-php/class/DB.php` to parse `DB_DRIVER` and dynamically build PDO connections for PostgreSQL or SQLite. Implement `DatabaseMigrator::initTables()` to automatically instantiate correct sequences, constraints, and timestamps for both engines. Create a local testing suite using a Docker Compose sandbox to verify integration with both drivers.
- **Complexity**: Part 5

## Recommended Reading
- `docs/backend/php-mvc-architecture.md`
- `docs/backend/php-cms-architecture.md`
- `docs/backend/php-plugin-system-guide.md`
- `docs/backend/small-company-features-plan.md`
- `docs/backend/db-persistence-postgresql-plan.md`
