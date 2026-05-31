# Part 6: Polyglot Systems: Node, Go & Python (100 Hours)

Learn to build high-performance services using Node.js, Go, Rust, and Python.

## Modules (10 Hours Each)
- M051: Node.js & Express Real-Time
- M052: Go Fundamentals
- M053: Go Microservices
- M054: Python for Data APIs
- M055: Rust Fundamentals
- M056: Rust for System Tools
- M057: gRPC & Protobuf
- M058: Message Queues (Redis)
- M059: Serverless Functions
- M060: Project: Multi-Language API

## Practical Labs

### 1. Go Microservices (35h)
**Reference**: `repo-go/main.go`
- **Task**: Study the simplified Go implementation.
- **Exercise**: Add a health-check endpoint to the Go server.

### 2. Python Data API (35h)
**Reference**: `repo-python/main.py`
- **Task**: See how Python handles site data.
- **Exercise**: Create a script that generates a summary of all site configs.

### 3. Multi-Stack Feature Equivalence (30h)
**Reference**: `/docs/backend/multi-stack-equivalence-plan.md`
- **Goal**: Understand how different application runtimes (Express/Vite, FastAPI, Go, PHP) handle multi-tenant routing, ZIP archive transfers, and secure administrative dashboards.
- **Exercise**: Implement a standardized `?cms_debug=true` environment diagnostic endpoint in either `repo-go` or `repo-python` that checks directory permissions, active plugins, and reports server metrics, matching the behavior of `PHP's CMS::validateSetup`.
- **Complexity**: Part 3 (Medium difficulty).

### 4. Stack Selection & Benchmarking (20h)
**Reference**: `docs/stack-comparison.md`
- **Goal**: Understand the trade-offs of code volume, performance, security, and DX/AX across Go, Rust, Python, React/Vue, and PHP.
- **Exercise**: Conduct a load-testing benchmark using a tool like Apache Benchmark or `wrk` against the same endpoint (e.g., `/admin/api/sites`) hosted on Go, Rust, Python, and PHP. Document how the theoretical comparison in `stack-comparison.md` lines up with real-world latency, memory usage, and concurrency results.
- **Complexity**: Part 4 (Advanced).

### 5. Multi-Stack GLB 3D Integration & Binary Parsing API (35h) 🍓 NEW
**Reference**: `/docs/glb-multi-stack-integration-plan.md`
- **Goal**: Understand model loading, format validation, asset compression, and skeletal animation parameters within multiple backend and client runtimes (Kotlin, Dart, Go, PHP, Python, React, Vue, Rust).
- **Exercise**: Write a standalone, testable validation script in Go (under `repo-go/`) or Python (under `repo-python/`) that reads an uploaded or local binary file, verifies the presence of the GLB standard `0x46546C67` ("glTF") byte array magic headers, and parses the 12-byte container header limits safely without loading the full file structure into active heap memory.
- **Complexity**: Part 5 (Highly Advanced).

## Recommended Reading
- `docs/backend/node-cms-features.md`
- `docs/backend/multi-stack-equivalence-plan.md`
- `docs/glb-multi-stack-integration-plan.md`

