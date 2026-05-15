# Implementation Plan: Stack Performance Benchmarking

This document outlines the strategy for benchmarking and comparing the performance of the four primary stacks in this repository: **PHP Multisite**, **Vite + React**, **Vite + Vue**, and **Python FastAPI**.

## 1. Benchmarking Objectives

The goal is to determine the operational characteristics of each stack to provide data-driven recommendations for specific use cases.

- **PHP**: Evaluate overhead of the hierarchical component system and multi-site routing.
- **React/Vue**: Compare hydration times, bundle sizes, and static serving efficiency.
- **Python (FastAPI)**: Benchmarking async request handling and JSON serialization speeds.

## 2. Key Performance Indicators (KPIs)

| Metric | Tool | Description |
| :--- | :--- | :--- |
| **TTFB** (Time to First Byte) | `curl` / Lighthouse | Measures server responsiveness. |
| **RPS** (Requests Per Second) | `k6` / `wrk` | Measures maximum throughput under load. |
| **P95 Latency** | `k6` | The response time threshold for 95% of users. |
| **Memory Footprint** | `docker stats` | Idle vs. Load RAM usage per container. |
| **LCP** (Largest Contentful Paint) | Lighthouse | User-perceived loading speed (Frontend only). |

## 3. AI Agent Interaction & Optimization Metrics

Beyond raw hardware performance, we will measure the "Developer Experience (DX) for AI" for each stack. This evaluates how effectively an AI agent can maintain and evolve the application.

| Metric | Analysis Method | Goal |
| :--- | :--- | :--- |
| **Token Efficiency** | Code density analysis | Fewer tokens required to express complex logic. |
| **Refactoring Safety** | Type-system coverage (TS/Python hints) | Reliability of AI-led structural changes. |
| **Boilerplate Ratio** | Framework analysis | Higher ratio of "business logic" vs "scaffolding". |
| **Contextual Load** | File dependency depth | Ability of AI to understand a feature within a single file. |

## 4. AI-Driven Testing & Debugging Workflow

A critical component of the benchmark is how effectively an AI agent can proactively maintain the stack. This uses a "Self-Healing Cycle":

1.  **Test Generation**: The agent is tasked with generating a comprehensive test suite (Unit/Feature) for a new module.
2.  **Validation execution**: The agent must execute the tests (`npm test`, `pytest`, or `phpunit`) and interpret the logs.
3.  **Automated Debugging**: If tests fail, the agent must analyze the stack trace, identify the root cause, and apply a fix without human intervention.

### Comparative Testing Strategies

| Stack | Primary Test Runner | AI Debugging Advantage |
| :--- | :--- | :--- |
| **PHP** | `PHPUnit` | Strong reflection capabilities allow AI to inspect class hierarchies easily. |
| **React/Vue** | `Vitest` / `Playwright` | Visual regression testing and DOM-based validation for UX consistency. |
| **Python** | `pytest` | Extremely readable tracebacks that allow AI to pinpoint logic errors in async code. |

## 5. Implementation Phases

### Phase 1: Baseline Environment Setup
- Deploy all four stacks using the provided `docker-compose.yml` configurations on identical hardware (e.g., a single Coolify node).
- Ensure all services are running without debug mode enabled (e.g., `APP_DEBUG=false` for PHP, `production` builds for JS).

### Phase 2: Synthetic Load Testing
Using **k6**, we will run scripts simulating concentrated traffic:
- **Scenario A (Static)**: Fetching the index page 500 times with 50 virtual users.
- **Scenario B (API)**: Fetching a 1MB JSON payload (simulated in PHP via `Model` and Python via `FastAPI`).
- **Scenario C (Stress)**: Gradually increasing users until the service returns 5xx errors or latency exceeds 2 seconds.

### Phase 3: AI Agent Efficiency Simulation
We will execute identical refactoring tasks across all stacks (e.g., "Add a JWT-protected secret endpoint") and measure:
- **Success Rate**: Did the agent produce functional code on the first attempt?
- **Revision Count**: How many turns did the agent need to fix linting/syntax errors?
- **Speed of Implementation**: Time elapsed for the agent to complete the task.

### Phase 4: Automated Data Collection
- Create a `benchmark-runner.sh` script to execute the tests sequentially.
- Export results to a central `benchmarks/results.json`.

### Phase 5: Analysis & Reporting
- Generate comparison charts (using `d3` or `recharts` in the CMS dashboard).
- Document "Sweet Spots" for each technology.

## 6. Expected Outcomes (Hypothesis)
- **Fastest Static Delivery**: React/Vue (served via optimized `serve` or Nginx).
- **Highest Concurrent Throughput**: Python FastAPI (due to its asynchronous event loop).
- **Highest AI Agent Velocity**: Python (high readability, low boilerplate) and PHP (mature, predictable MVC patterns).
- **Safest AI Refactoring**: React/Vue/Python (due to strict static typing and mature LSP support).
- **Fastest Development-to-Production**: PHP (due to the integrated CMS engine).

## 7. Tooling Recommendations
- **Load Testing**: [k6.io](https://k6.io/) (modern, JS-based scripts).
- **Frontend Vitals**: [Google Lighthouse CLI](https://github.com/GoogleChrome/lighthouse).
- **System Monitoring**: `Prometheus` + `Grafana` (for long-term container health).
