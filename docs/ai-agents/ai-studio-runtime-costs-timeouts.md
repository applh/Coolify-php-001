# AI Studio Agent Runtime: Timeouts, Container Lifecycle, & Transaction Costs

This architectural reference outlines the execution bounds, platform timeouts, server container lifecycles, and transaction cost variables for developers implementing advanced **AI Studio Coding Agents** inside the Google AI Studio cloud sandbox. 

---

## ⏳ 1. Execution Lifetimes and Timeouts

When building workflows involving file modifications, tool execution, and code compilations, agents operate across two distinct runtime environments: the **Model Inference Turn**, and the **Ephemeral Linux Container Sandbox**.

```
                           [ AI Studio Platform ]
                                     │
         ┌───────────────────────────┴───────────────────────────┐
         ▼                                                       ▼
[ Model Turn / RPC Window ]                            [ Ephemeral Container Sandbox ]
  ──> 10-Minute Timeout Limit                            ──> Cloud Run Container Workspace
  ──> Handled via Synchronous Fetch                      ──> Sleep Policy: 15-30m idle
  ──> Ideal for AST Parsing, quick commands              ──> Active Dev Servers on Port 3000
```

### Turn Limits vs Background Execution
1.  **Model Turn Gateway Limit (10 Minutes / 600,000ms)**:
    - Every synchronous prompt-response cycle initiated between the client-facing UI shell and the backend model engine runs inside a standard RPC deadline.
    - If a task takes longer than **10 minutes** to complete (such as a large Webpack build, extensive database indexing, or downloading millions of files), the gateway will terminate the connection, resulting in a `URL_TIMEOUT` or a gRPC `INTERNAL` exception.
    - **Developer Action**: To prevent timeouts, configure the agent to initiate heavy jobs as **asynchronous background CLI processes** (e.g., using `run_command` with non-blocking, background execution flags), then monitor progress using short-lived poll loops or fallback timers.
2.  **Container Workspace Sleep Policies (15 to 30 Minutes Input Idle)**:
    - The underlying environment hosting your full-stack repository runs inside an isolated, containerized environment (typically backed by Google Cloud Run).
    - To prevent CPU and memory resource leaks, the platform monitors user and agent activity. If no prompts, file saves, shell commands, or preview interactions are registered for **15 to 30 minutes**, the container is placed into sleep mode.
    - **Active Exception**: When web servers (such as Node or Vite on **Port 3000**) are actively listening for incoming HTTP preview traffic, or if an background scheduling system is executing active loops, the container will remain awake for the remainder of the session (usually capped at several hours or until the user closes their browser interface).

---

## 💸 2. Financial Economics of Agent Workflows

Google AI Studio provides a highly cost-effective paradigm for software prototyping. Understanding the pricing tiers and mechanics is critical to avoiding runaway budget spend during automated workflows.

### Free Tier vs Paid Pay-As-You-Go Tier
*   **The Prototyping Free Tier**:
    *   Fully functional, completely free of charge.
    *   **Gemini 1.5 / 2.5 Flash**: Capped at 15 Requests Per Minute (RPM), 1 Million Tokens Per Minute (TPM), and 1,500 Requests Per Day (RPD).
    *   **Gemini 1.5 / 2.5 Pro**: Capped at 2 RPM, 32,000 TPM, and 50 RPD.
    *   *Usage Guardrail*: Your prompts and generations are compiled to train Google models. Confidential variables should be kept inside the secure Vault, or developers should switch to the Paid Tier.
*   **The Paid Pay-As-You-Go Tier (Google Cloud Billing)**:
    *   Prompts and outputs are completely private and never monitored for model training.
    *   Charges are calculated purely back to token consumption. No flat infrastructure or container runtime fees are assessed for running developer server tests.

### Paid Token Rate Structure 

| Model Alias | Sub-128k Input (per 1M) | Sub-128k Output (per 1M) | Over-128k Input (per 1M) | Over-128k Output (per 1M) |
| :--- | :--- | :--- | :--- | :--- |
| **Gemini 2.5 Flash** | **$0.075 USD** | **$0.30 USD** | **$0.150 USD** | **$0.60 USD** |
| **Gemini 2.5 Pro** | **$1.250 USD** | **$5.00 USD** | **$2.500 USD** | **$10.00 USD** |

---

## 💾 3. Context Caching: The Ultimate Speed and Cost Hack

For codebase agents, the largest financial bottleneck is reading the **entire project context tree** repeatedly on every turn. If your workspace contains 100,000 tokens of code documentation, source files, and structure templates, feeding that entire list into every turn gets expensive.

### How Context Caching Works
*   When a prompt contains more than **32,768 tokens**, Google AI Studio can cache that context in local memory for a specific duration (TTL - Time To Live).
*   Subsequent agent requests that share the exact same context segment "hit" the cache, reducing both latency (speedups of up to 5x) and input costs by **50%**.

### Context Cache Pricing

| Model Alias | Hourly Cache Cost (per 1M tokens) | Cached Input Cost Reduction |
| :--- | :---: | :---: |
| **Gemini 2.5 Flash** | **~$0.01875 USD** | **50% cheaper** ($0.0375 / 1M) |
| **Gemini 2.5 Pro** | **~$0.31250 USD** | **50% cheaper** ($0.6250 / 1M) |

*Best Practice*: Keep file systems clean. Do not commit large binary assets, images, zip bundles, or generated databases to directories indexed by the agent, so that your project's text footprint stays within the optimal caching bounds.

---

## 🎯 4. Architecting Low-Cost Multi-Agent Routing

To maximize execution speeds while minimizing project costs, employ a **Tiered Routing Cascade Pattern** as pictured below:

```
                      [ User Task Prompt ]
                               │
                               ▼
               [ Router: Gemini 2.5 Flash ] ──> Simple translations?
                               │                 Simple code edits?
                               │                 (Processed at $0.075/1M)
                               ▼
            [ Escalator: Gemini 2.5 Pro ] ──> Complex refactoring?
                                                 Relational schema edits?
                                                 AST transformations?
                                                 (Processed at $1.250/1M)
```

1.  **The Flash First-Responder**:
    - Route general queries, single-file edits, standard markdown documentation updates, translation tasks, and linter check fixes to **Gemini 2.5 Flash**. Over 85% of standard workspace updates can be completed here for fractions of a cent.
2.  **The Pro Escalator**:
    - Escalate critical tasks—such as complex multi-file architectural refactors, deep dependency resolution, or high-risk multi-table relational schema designs—to **Gemini 2.5 Pro**.
3.  **Clean Cache Hygiene**:
    - Keep test directories segregated. By organizing larger static catalogs outside of primary source paths, agents do not re-read large content logs, preserving cache hits.
