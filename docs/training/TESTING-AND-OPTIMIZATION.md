# Testing, Performance & Optimization Labs

Learn to build software that is fast, reliable, and verified.

## 1. Testing Strategies (Practical)
**Goal**: Ensure your features don't break as the code evolves.

- **Lab 1: Manual Test Logs**
    - **Task**: Create a `docs/training/TEST-LOG.md`.
    - **Exercise**: For every feature you build in Phase 2, write 5 manual test cases (e.g., "Clicking 'Save' without input shows error").
- **Lab 2: Unit Testing Logic**
    - **Reference**: `repo-php/class/Router.php`
    - **Task**: Identify a method that is easy to test (no external dependencies).
    - **Exercise**: Write a mock test script in PHP that passes 10 different URLs to the router and asserts that the correct controller is returned.

## 2. Performance Optimization
**Goal**: Make your application feel instantaneous.

- **Lab 1: Asset Optimization**
    - **Reference**: `repo-php/content/babiblog.fr/b64/`
    - **Task**: Analyze why images are stored as Base64 strings.
    - **Exercise**: Measure the page load weight. Implement a lazy-loading strategy for these images using the native `loading="lazy"` attribute.
- **Lab 2: Database Query Performance**
    - **Reference**: `repo-php/class/DB.php`
    - **Task**: Look at how queries are executed.
    - **Exercise**: Use `EXPLAIN` on a SQL query in the `SiteExplorer` to identify if an index is missing on the `site_url` column.

## 3. Benchmarking & Analytics
**Goal**: Use data to drive engineering decisions.

- **Lab 1: The Benchmarking Tool**
    - **Reference**: `src/views/SiteBenchmarker.vue`
    - **Task**: Run a benchmark on all demo sites.
    - **Exercise**: Document the "Time to First Byte" (TTFB) for PHP vs Python vs Go.
- **Lab 2: Load Testing (Simulated)**
    - **Task**: Use the `ab` (Apache Benchmark) tool if available, or a simple fetch loop in JS.
    - **Exercise**: Hit the `repo-php` index 100 times in a row and calculate the average response time. Optimize one slow component and re-test to see the delta.

## Complexity Levels
- **Basic**: Manual Logs & Asset cleanup.
- **Advanced**: DB Indexing & Component Profiling.
