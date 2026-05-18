# Full-Stack Engineering: The Infinite Glossary

This document contains a comprehensive breakdown of the technical terminology used across the FRAISE platform. These terms cover frontend, backend, AI integration, and devops architectures.

---

## 1. Core Architecture & Backend (PHP Stack)

### PHP (Hypertext Preprocessor)
A server-side scripting language designed for web development. In this app, PHP is the primary engine for the legacy CMS core located in `repo-php/`. It handles template rendering, database interactions via PDO, and the plugin ecosystem. PHP's execution model is typically request-response, where a script initializes, processes data, and terminates for every HTTP request, though features like OPcache and modern runtimes have significantly optimized this lifecycle.

### MVC (Model-View-Controller)
An architectural pattern that separates an application into three main logical components: the Model (data logic), the View (UI logic), and the Controller (business logic). Our CMS implements a custom MVC framework. Models in `repo-php/class/Model.php` interact with the DB, Views are modular PHP files in `repo-php/views/`, and Controllers manage the flow between them. This separation ensures that the database schema doesn't leak directly into the frontend, allowing for easier maintenance and testing.

### CMS (Content Management System)
A software application that allows users to create, manage, and modify content on a website without needing specialized technical knowledge. This project is a 'Multi-Tenant' CMS, meaning it can host multiple sites (site1.com, site2.com) from a single shared codebase. The `CMS` class in `repo-php/class/CMS.php` acts as the orchestrator, determining which site folder in `content/` to load based on the request hostname and managing the plugin lifecycle for that specific instance.

### PSR (PHP Standard Recommendation)
A set of PHP specifications published by the PHP Framework Interop Group. This app follows PSR patterns for coding styles and autoloading. Adhering to standards like PSR-4 (Autoloading) ensures that classes located in `repo-php/class/` can be automatically found by the PHP engine without thousands of manual `require` statements. This promotes interoperability with external libraries installed via Composer and makes the codebase predictable for other PHP developers.

### Composer
The standard dependency manager for PHP. It allows you to declare the libraries your project depends on and it will manage (install/update) them for you. In our architecture, Composer handles the installation of critical tools like routing libraries or database drivers. The `vendor/` directory (mapped in Docker volumes) contains these libraries, ensuring that our core logic remains slim while leveraging the vast PHP ecosystem for common tasks like image processing or API clients.

### PDO (PHP Data Objects)
A database access layer providing a uniform method of access to multiple databases. The `DB` class in `repo-php/class/DB.php` uses PDO to interact with SQLite or MySQL safely. PDO is critical for security because it supports 'Prepared Statements', which automatically sanitize user input to prevent SQL Injection attacks. Instead of concatenating variables into strings, we pass them as parameters, allowing the DB engine to handle the data safely.

### Router
A component that maps incoming URLs to specific code handlers. Our app uses both a backend Router in `repo-php/class/Router.php` and a frontend router in the Vue app. The Backend Router analyzes the Request URI (e.g., `/api/tasks`) and invokes the correct PHP controller. This allows us to have clean, 'Pretty URLs' without `.php` extensions, which is better for SEO and user experience. The frontend router handles navigation within the dashboard without triggering a full page reload.

### Plugin Architecture
A design pattern that allows extending the functionality of a core system without modifying its source code. The `PluginManager` in `repo-php/class/PluginManager.php` scans the `plugins/` directory (e.g., `analytics`, `seo-optimizer`, `forms`) and hooks them into the CMS lifecycle. This 'Open-Closed Principle' implementation means we can add a new feature like a newsletter signup simply by dropping a folder into the plugins directory, which the CMS then automatically initializes.

---

## 2. Modern Frontend (Vue & Vite Stack)

### Vue 3 (Composition API)
A progressive JavaScript framework for building user interfaces. Our main dashboard uses Vue 3 with the Composition API (`setup` script). Unlike the Options API, the Composition API allows us to group logic by feature rather than by lifecycle hook, making complex components like `SiteEditor.vue` much easier to read. Vue's reactivity system ensures that when the data changes, the UI updates instantly without manual DOM manipulation.

### Vite
A modern frontend build tool that provides an extremely fast development environment and optimized production builds. Vite replaces older tools like Webpack by leveraging native ES modules in the browser. It features Hot Module Replacement (HMR) and uses esbuild for blazing-fast bundling. Vite is what orchestrates the compilation of our `.vue` and `.ts` files into the small, optimized chunks sent to the user's browser.

### Tailwind CSS
A utility-first CSS framework that allows for rapid UI development by applying low-level utility classes directly in the HTML or Vue templates. Instead of writing custom CSS for a card, we use classes like `bg-white shadow-xl rounded-lg p-6`. This approach ensures visual consistency, eliminates the need for large, unmaintainable CSS files, and provides a 'design system' out of the box.

### Reactive State (ref, reactive)
In Vue 3, reactivity is achieved using `ref` for primitive values and `reactive` for objects. When a reactive variable changes, Vue automatically detects the change and re-renders only the parts of the DOM that depend on that variable. This is the core of why modern web apps feel so fast and responsive compared to traditional server-rendered templates.

### Virtual DOM
A programming concept where an 'ideal', or 'virtual', representation of a UI is kept in memory and synced with the 'real' DOM by a library such as Vue. When state changes, Vue creates a new Virtual DOM tree, compares it with the old one (a process called 'diffing'), and calculates the minimum number of changes needed to update the actual browser DOM.

### Single Page Application (SPA)
A web application that interacts with the user by dynamically rewriting the current web page with new data from the web server, instead of the default method of a browser loading entire new pages. Our main dashboard is an SPA, providing a fluid transition between views like 'Site Explorer' and 'Benchmarker' without annoying white flashes or full-page reloads.

---

## 3. Artificial Intelligence & Agentic Engineering

### Gemini API
A family of generative AI models developed by Google DeepMind. This app integrates Gemini 1.5 Flash and Pro to power automated tasks like content generation, code refactoring, and SEO optimization. We access it via the `@google/genai` SDK, enabling the platform to 'understand' and 'create' content just like a human developer would.

### AI Agent
An autonomous AI system that can perceive its environment, reason about tasks, and take actions to achieve specific goals. Our 'AI Coder Agent' is a specialized instance of the Gemini model equipped with 'Tools' (like our file-editing API) that allow it to perform real operations, such as creating a new site template or fixing bugs based on a simple text description.

### Prompt Engineering
The process of structuring text that can be interpreted and understood by a generative AI model. It involves defining personas, constraints, and specific output formats. Effective prompt engineering is what ensures the AI generates valid Tailwind classes instead of generic CSS or creates a PHP class that actually conforms to our system's architecture.

### Function Calling
A technique that allows LLMs to interact with external APIs or functions. Instead of the AI just talking, it outputs a structured request (like `createSite({name: 'My Store'})`) which our system then executes. This bridge allows the Gemini model to connect its 'reasoning' to 'real-world actions' within our CMS.

---

## 4. Infrastructure, DevOps & Security

### Nginx
A high-performance HTTP server and reverse proxy that acts as the entry point for our application. It handles incoming traffic, serves static assets (CSS, JS, Images) directly for speed, and passes dynamic requests to the PHP-FPM process or the Node.js server. Its configuration determines how the world sees our multi-tenant sites.

### Docker & Docker Compose
Docker is a platform for running applications in 'Containers'—isolated environments that include everything needed to run software. Docker Compose manages multi-container applications (like our PHP + Python + Node setup). This ensures 'Environmental Parity', meaning the app runs exactly the same on a developer's laptop as it does on a production server.

### Coolify
An open-source, self-hostable alternative to Heroku. We use Coolify to manage our production deployments. It automates the process of pulling code from Git, building Docker images, and updating the live containers with zero downtime. It's the engine that handles the 'Operational' side of the FRAISE platform.

### CI/CD (Continuous Integration/Deployment)
A set of practices that automate the process of testing and deploying code. CI ensures that every code change is valid and doesn't break existing features. CD ensures that those changes are automatically pushed to the production environment, allowing us to ship features 10x faster than traditional methods.

### Environment Variables (.env)
Sensitive configuration values stored outside the source code. Storing API keys or database passwords in `.env` files is a critical security measure that prevents secrets from being accidentally leaked into version control. Our system loads these variables at runtime to connect to services like Google Gemini or Firestore.

---

## 5. Protocols & Communication

### HTTP (Hypertext Transfer Protocol)
The foundation of web data communication. It follows a 'Request-Response' pattern where the browser asks for a resource and the server provides it. Understanding HTTP status codes (200, 404, 500) and methods (GET, POST, PUT, DELETE) is fundamental to debugging web applications and building efficient APIs.

### API Endpoints
Specific URLs that an API uses to communicate with other software. Each endpoint represents a specific resource or action. For example, `GET /api/sites` might list all sites, while `POST /api/sites` creates a new one. Well-designed endpoints (RESTful) make the backend intuitive and easy to integrate with multiple frontends.

### JSON (JavaScript Object Notation)
The standard data format for the web. It's used for everything in our app: API responses, site configurations, and even this training metadata. Because it's a simple text format composed of key-value pairs and arrays, it is easily understood by programmers and machines alike across all our polyglot languages.

### CORS (Cross-Origin Resource Sharing)
A browser-implemented security measure that defines which external domains are allowed to access an API. Because our Vue dashboard and PHP API might run on different ports or domains during development, we must explicitly permit 'Cross-Origin' requests to allow them to communicate.

---

## 6. Development Workflow

### Git (Commit, Push, Pull)
The most popular version control system. It allows us to track every change made to the codebase over time. 'Commits' represent snapshots of progress, 'Pushes' share that progress with the team, and 'Pulls' keep everyone in sync. It is the backbone of collaborative software engineering.

### Branching
A Git feature that allows developers to diverge from the main codebase to work on new features or bug fixes in isolation. This prevents half-finished code from breaking the production site and allows for peer review via Pull Requests before the new code is merged back into the 'main' branch.

### NPM Scripts
Automation commands defined in `package.json`. Instead of remembering complex CLI flags for compilers, we use simple aliases like `npm run dev` or `npm run build`. This ensures that every developer on the project uses the exact same build process, reducing 'configuration drift' and build errors.

### Build Pipeline
The automated sequence of steps that transforms developer-written code (TypeScript, Vue, SCSS) into the optimized, minified assets that the browser actually executes. This pipeline is the final safeguard that ensures performance and code quality before the application is deployed to the real world.

---

## 7. Practical Labs

### Lab 7.1: Semantic Glossary Expansion
**Goal**: Learn how to extend the platform's training metadata by adding new technical terms with high-fidelity attributes (SVG, Links, Prompts).

**Reference**: 
- `/docs/training/slides/glossary/technical-terms-2.json`
- `/docs/training/TECHNICAL-GLOSSARY.md`

**Exercise**: 
1. Open `/docs/training/slides/glossary/technical-terms-2.json`.
2. Locate the end of the `slides` array and add a new entry for a term not yet covered (e.g., "WebSockets" or "GraphQL").
3. Ensure the new entry follows the high-fidelity standard:
   - `content`: Descriptive text > 500 characters.
   - `svgContent`: A relevant, high-quality SVG illustration.
   - `links`: At least 3 links to official documentation or top-tier tutorials.
   - `studentPrompts`: 3 critical-thinking questions for students.
4. Update the corresponding entry in this Markdown file (`TECHNICAL-GLOSSARY.md`) to keep the "Human-Readable" and "Machine-JSON" documentation in sync.

**Complexity**: Part 2 (Intermediate)

---

*Note: This glossary serves as a living document. As the FRAISE platform evolves and we add more specialized repositories (Go, Rust, Flutter), this glossary will be expanded to encompass the full breadth of modern engineering terminology. A new training lab is now available for the term expansion feature just developed.*
