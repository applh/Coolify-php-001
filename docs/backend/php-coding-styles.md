# PHP Coding Styles & Guidelines

To maintain a clean, scalable, and maintainable PHP codebase, we follow these strict coding conventions and architectural patterns:

## 1. Class-Based Architecture
- **No Procedural Code**: All business logic, routing, and file handling must be encapsulated within classes.
- **Static Methods**: Where applicable, utilize static methods to group functional logic that doesn't require maintaining object state (e.g., utility functions, initializers, validators).

## 2. Design Patterns
- **Singleton Pattern**: Core system classes that should only have a single instance (like Configuration managers or central Routers) should implement the Singleton pattern or rely on static states.
- **Front Controller Pattern**: `public/index.php` acts as the Front Controller. It should contain minimal logic—only autoloading, environment setup, and a single static call to dispatch the Router.
- **Factory Pattern**: When instantiating complex objects (like specific page types or database connectors), use Factories to keep object creation separate from usage.

## 3. Formatting & Standards
- Follow **PSR-12** extended coding standards.
- Use explicit visibility modifiers (`public`, `protected`, `private`) for all properties and methods.
- Type-hint arguments and return types whenever possible (PHP 7.4+).

## 4. Refactoring Strategy
- **Moving Logic to Classes**: Any remaining inline PHP code in `index.php` or template files should be refactored into descriptive static methods within appropriate classes within the `/class` directory.
- **Dependency Injection**: If static methods pass thresholds of complexity, consider injecting dependencies rather than relying on global state, keeping code easily testable.
