# Practical Software Architecture: OOP & Design Patterns

Master the art of writing maintainable, scalable, and reusable code through object-oriented principles and established patterns.

## 1. Object-Oriented Programming (OOP) Deep Dive
**Goal**: Move beyond procedural "scripting" to true object-oriented systems.

- **Lab 1: The Power of Classes & Objects**
    - **Reference**: `repo-php/class/Component.php`
    - **Task**: Analyze how `Component.php` serves as a base class for UI elements.
    - **Exercise**: Create a `ButtonComponent` that extends `Component` and overrides the `render()` method to include specific Tailwind classes for a primary action button.
- **Lab 2: Abstract Classes & Interfaces**
    - **Reference**: `repo-php/class/Model.php`
    - **Task**: Identify the abstract methods in `Model.php`.
    - **Exercise**: Create a new `ProductModel` that inherits from `Model` and implements the required logic to save data to a JSON file.

## 2. Design Patterns in the Wild
**Goal**: Implement battle-tested solutions to common software problems.

- **Lab 1: The Singleton Pattern (Database Access)**
    - **Reference**: `repo-php/class/DB.php`
    - **Task**: Verify if `DB.php` uses a Singleton pattern to prevent opening multiple database connections.
    - **Exercise**: Refactor the class to ensure that only ONE instance of the database connection can exist throughout the application lifecycle.
- **Lab 2: The Strategy Pattern (Plugin System)**
    - **Reference**: `repo-php/class/PluginManager.php`
    - **Task**: See how different plugins (`seo-optimizer`, `analytics`) follow a common interface.
    - **Exercise**: Implement a new "Optimization Strategy" that can be swapped at runtime to either minify HTML or compress images using the Strategy Pattern.
- **Lab 3: The Factory Pattern (View Rendering)**
    - **Reference**: `repo-php/class/View.php`
    - **Task**: Study how views are instantiated.
    - **Exercise**: Create a `ViewFactory` that returns different view objects based on the requested file type (e.g., `.php` vs `.html`).

## 3. SOLID Principles
**Goal**: Refactor code to meet modern engineering standards.

- **Lab: The Single Responsibility Principle (SRP)**
    - **Reference**: `repo-php/class/CMS.php`
    - **Task**: Identify if `CMS.php` is doing too much (e.g., routing, data fetching, and rendering).
    - **Exercise**: Extract the "Rendering" logic into a separate `Renderer` class to satisfy SRP.

## 4. DRY & Refactoring Techniques
**Goal**: Eliminate redundancy and simplify code logic.

- **Lab 1: Identifying Duplication**
    - **Reference**: `repo-php/class/App.php` and `repo-php/class/Router.php`
    - **Task**: Find overlapping path-handling logic between these two classes.
    - **Exercise**: Create a shared `PathHelper` trait or class to unify URL normalization.
- **Lab 2: Method Extraction**
    - **Reference**: `repo-php/class/Layout.php`
    - **Task**: Look for large blocks of logic in the `render` method.
    - **Exercise**: Refactor the method by extracting "Head", "Body", and "Footer" logic into private helper methods.
- **Lab 3: Eliminating Magic Numbers**
    - **Reference**: `repo-php/class/AITaskManager.php`
    - **Task**: Identify hardcoded limits or intervals.
    - **Exercise**: Refactor the class to use class constants for configuration values.

## Integration
Apply one architectural refactor to your backend labs every week.
