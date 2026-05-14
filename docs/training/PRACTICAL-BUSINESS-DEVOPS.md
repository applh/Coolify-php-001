# Practical Business, Management & DevOps Labs

This module moves beyond code to the systems and strategies that make software successful in the real world.

## 1. Startup & Product Strategy (Practical)
**Goal**: Transition from "Developer" to "Product Owner" by launching a new demo.

- **Lab 1: The MVP (Minimum Viable Product)**
    - **Task**: Identify 3 features in `repo-php/content/webapps.demo` that can be removed while still keeping the site functional.
    - **Exercise**: Create a folder `repo-php/content/mvp.test` and implement ONLY the core value proposition.
- **Lab 2: User-Centric Iteration**
    - **Task**: Use the [AI Student Prompts](./AI-STUDENT-PROMPTS.md) to generate a "User Feedback Report" for `repo-react`.
    - **Exercise**: Implement one small UI change based on that simulated feedback.

## 2. Agile Project Management (Practical)
**Goal**: Organize your work using industry-standard workflows.

- **Lab 1: The Markdown Backlog**
    - **Task**: Create a file `docs/training/MY-SRE-BACKLOG.md`. 
    - **Exercise**: Translate the tasks from Phase 1 into 10 "User Stories" with clear Acceptance Criteria.
- **Lab 2: The "Sprint" Review**
    - **Task**: After completing any Phase lab, write a "Post-Mortem" in your backlog.
    - **Exercise**: Document what was difficult, what you would automate next time, and how long it actually took vs. estimated.

## 3. DevOps & Performance (Practical)
**Goal**: Master the environment where your code lives.

- **Lab 1: Container Hardening**
    - **Task**: Open `repo-php/Dockerfile`. 
    - **Exercise**: Try to change the base image to a smaller version (e.g., from `php:8.2-fpm` to `php:8.2-fpm-alpine`) and run `docker-compose up` to see if it still builds.
- **Lab 2: Benchmarking**
    - **Task**: Use the `src/views/SiteBenchmarker.vue` tool in the dashboard.
    - **Exercise**: Compare the load speeds of `repo-php`, `repo-go`, and `repo-rust`. Document the results in `docs/benchmarking-results.md`.

## 4. Practical Security Audit
**Goal**: Find and fix common vulnerabilities.

- **Lab 1: XSS (Cross-Site Scripting) Hunting**
    - **Task**: Find where user input is displayed in `repo-php/views/components/Card.php`.
    - **Exercise**: Test what happens if you pass a string like `<script>alert('pwned')</script>` into a title field. If it executes, fix it using `htmlspecialchars()`.
- **Lab 2: Input Validation**
    - **Task**: Study the `forms` plugin logic.
    - **Exercise**: Add a server-side validation rule that prevents any form submission with more than 500 characters in the message field.

## Integration with your journey
Include one task from this module for every 4 coding labs you complete to keep your skills balanced.
