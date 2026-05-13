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

## 3. Implementation Phases

### Phase 1: Baseline Environment Setup
- Deploy all four stacks using the provided `docker-compose.yml` configurations on identical hardware (e.g., a single Coolify node).
- Ensure all services are running without debug mode enabled (e.g., `APP_DEBUG=false` for PHP, `production` builds for JS).

### Phase 2: Synthetic Load Testing
Using **k6**, we will run scripts simulating concentrated traffic:
- **Scenario A (Static)**: Fetching the index page 500 times with 50 virtual users.
- **Scenario B (API)**: Fetching a 1MB JSON payload (simulated in PHP via `Model` and Python via `FastAPI`).
- **Scenario C (Stress)**: Gradually increasing users until the service returns 5xx errors or latency exceeds 2 seconds.

### Phase 3: Automated Data Collection
- Create a `benchmark-runner.sh` script to execute the tests sequentially.
- Export results to a central `benchmarks/results.json`.

### Phase 4: Analysis & Reporting
- Generate comparison charts (using `d3` or `recharts` in the CMS dashboard).
- Document "Sweet Spots" for each technology.

## 4. Expected Outcomes (Hypothesis)
- **Fastest Static Delivery**: React/Vue (served via optimized `serve` or Nginx).
- **Highest Concurrent Throughput**: Python FastAPI (due to its asynchronous event loop).
- **Lowest Memory Overhead**: Python (for simple routes) or PHP (for single-tenant requests).
- **Fastest Development-to-Production**: PHP (due to the integrated CMS engine).

## 5. Tooling Recommendations
- **Load Testing**: [k6.io](https://k6.io/) (modern, JS-based scripts).
- **Frontend Vitals**: [Google Lighthouse CLI](https://github.com/GoogleChrome/lighthouse).
- **System Monitoring**: `Prometheus` + `Grafana` (for long-term container health).
