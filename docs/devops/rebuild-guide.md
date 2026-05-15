# Rebuild & Deployment Tutorial: From AI Studio to Production

This tutorial provides a step-by-step roadmap to rebuild and deploy this Multi-Stack CMS Manager (or any AI Studio project) using modern cloud infrastructure and Coolify.

---

## phase 1: Infrastructure & Accounts

### 1.1 Google Account & AI Studio
1. **Create a Gmail Account**: If you don't have one, create a new Google account.
2. **Access Google AI Studio**: Go to [aistudio.google.com](https://aistudio.google.com/).
3. **Start a New App**:
    - Click on **"Create New App"** (or use the "Build" tab).
    - **The Prompt**: Use a detailed initial prompt. 
    - *Example Prompt*: "Build a full-stack CMS manager with a Node.js Express backend, a Vue 3 dashboard, and a multi-tenant PHP architecture. Include a task queue for AI media generation using Gemini and ensure every stack is containerized with Docker."

### 1.2 Version Control (GitHub)
1. **Create a GitHub Account**: Sign up at [github.com](https://github.com).
2. **Create a Repository**: Create a new private (or public) repository named `multi-stack-cms`.
3. **Connect AI Studio**: In the AI Studio settings, use the "Export to GitHub" feature to link your project to this repository.

### 1.3 Domain & Hosting (VPS)
1. **Rent a Domain**: Use providers like Namecheap, Godaddy, or Porkbun to buy a domain (e.g., `your-cms-app.com`).
2. **Rent a VPS**: Get a Virtual Private Server from providers like Hetzner, DigitalOcean, or Linode.
    - **Recommended Specs**: At least 4GB RAM, 2 vCPUs, and Ubuntu 22.04 LTS.
3. **DNS Setup**: Point your domain's A-record to the IP address of your new VPS.

---

## Phase 2: Server Setup (Coolify)

### 2.1 Install Coolify
1. **SSH into your VPS**: `ssh root@your-vps-ip`
2. **Run the Install Script**:
   ```bash
   curl -fsSL https://get.coollabs.io/coolify/install.sh | bash
   ```
3. **Access Coolify**: Open `http://your-vps-ip:3000` in your browser and create your admin account.

### 2.2 Configure Coolify
1. **Setup Wildcard Domain**: In Coolify settings, configure your main domain (e.g., `https://your-cms-app.com`) to allow automatic subdomain generation.
2. **Connect GitHub**: 
    - Go to **Sources** -> **GitHub App**.
    - Follow the prompts to install the Coolify GitHub App on your account and give it access to your `multi-stack-cms` repository.

---

## Phase 3: Project Deployment

### 3.1 Deploying the Management Dashboard (Node.js)
1. **Create New Application**: In Coolify, select **New Service** -> **Application**.
2. **Select Repo**: Choose your GitHub repo and the branch (usually `main`).
3. **Base Directory**: Set this to `/` (the root).
4. **Configuration**: Coolify should detect the `Dockerfile` in the root. 
5. **Environment Variables**: Add necessary variables (e.g., `GEMINI_API_KEY`, `APP_ADMIN_PASSKEY`).
6. **Deploy**: Hit **Deploy**.

### 3.2 Deploying Sub-Stacks (PHP, React, Vue, Python)
For each stack (e.g., `repo-php`, `repo-react`):
1. **New Application**: Repeat the process but set the **Base Directory** to the specific folder (e.g., `/repo-react`).
2. **Docker Compose**: Coolify will find the `docker-compose.yml` inside that folder.
3. **FQDN**: Assign a unique subdomain (e.g., `https://react.your-cms-app.com`).

---

## Phase 4: Maintenance & Iteration

### 4.1 Syncing AI Studio Changes
When you make changes in Google AI Studio:
1. **Export to GitHub**: Use the built-in export tool to push your latest code to your repo.
2. **Auto-Deploy**: If configured in Coolify, your production app will automatically rebuild and redeploy upon detecting the new commit.

### 4.2 Data Persistence
Ensure you configure **Volumes** in Coolify for any directory that requires persistent data (like `repo-php/my-data` or the SQLite `cms.db`). Without volumes, data will be lost on the next deployment.
