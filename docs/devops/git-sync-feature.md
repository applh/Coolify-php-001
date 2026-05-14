# Implementation Plan: Git Sync Feature

This document outlines the architecture and workflow for the **Git Sync** feature, allowing users to synchronize their AI Studio project files with a remote Git repository (e.g., GitHub, GitLab, Bitbucket).

---

## 1. Objective
Enable a seamless bridge between the AI Studio's iterative environment and external version control systems.
The goal is to allow users to push their latest site modifications, PHP engine updates, and multi-stack assets directly to a remote repository for deployment persistence or team collaboration.

---

## 2. Configuration & Data Model

### A. Settings Storage
We will extend the backend configuration (or database) to store Git credentials:
- `git_remote_url`: The HTTPS/SSH URL of the destination repository.
- `git_branch`: The target branch (default: `main`).
- `git_auth_type`: `TOKEN` or `SSH`.
- `git_credentials`: Encrypted Personal Access Token (PAT) or Private SSH Key.

### B. UI Implementation
- **Git Settings Pane:** A new section in the Site Dashboard to configure the above parameters.
- **Sync Status Indicator:** Displays "In Sync", "Changes Pending", or "Last Sync: [Date]".
- **Sync Button:** A primary action button that triggers the backend synchronization workflow.

---

## 3. Synchronization Workflow

When the user clicks **"Sync"**, the following sequence is executed by the Node.js backend:

### Step 1: Workspace Preparation
- Locate (or create) a local temporary directory for the Git operation (e.g., `storage/git_workspace/`).
- If the directory does not contain a valid `.git` folder, perform a `git clone` of the remote repository.

### Step 2: Local Refresh
- Perform a `git pull` (or `git fetch` + `git reset`) to ensure the local workspace is up-to-date with the remote branch.
- Identify the project files to be synced (e.g., `/repo-php/content/`, `/repo-react/src/`).

### Step 3: Overlay & Merge
- Use a "Clear & Copy" or "Rsync-like" logic to overwrite the git workspace files with the latest AI Studio files.
- **Excluded Files:** Ensure `.env`, `node_modules`, and local SQLite databases are excluded via a standard `.gitignore` generated during sync.

### Step 4: Commit & Push
- Execute `git add .` to stage all changes.
- Automatically generate a commit message: `chore: sync with AI Studio [YYYY-MM-DD HH:mm]`.
- Execute `git push origin [branch]`.

---

## 4. Technical Stack

- **Backend Engine:** Node.js (within the existing `server.ts`).
- **Git Interface:** 
    - Preferred: `simple-git` or `isomorphic-git` for programmatic control.
    - Alternative: Spawning shell commands if `git` is pre-installed in the Docker environment.
- **Security:** Use environment variables for sensitive tokens; never log `git_credentials` in plaintext.

---

## 5. Potential Challenges & Mitigations

| Challenge | Mitigation |
| :--- | :--- |
| **Merge Conflicts** | Default to "Studio Wins" for automated syncs, or pause and alert the user to resolve manually via a CLI-like interface. |
| **Large Asset Sync** | Optimize by using `.gitignore` for large binaries or implementing chunked uploads if using Git LFS. |
| **Authentication Failures** | Provide clear error feedback in the UI with a link to Git Provider documentation (e.g., "How to generate a GitHub PAT"). |

---

## 6. Practical Lab: Git Workflow Automation
(Reference: `docs/training/PHASE-5-ADVANCED.md`)

**Exercise:**
Implement a script that uses `fs-extra` to mirror a subdirectory into a dummy git folder, ensuring that specific extensions (like `.log` or `.tmp`) are stripped during the process.
