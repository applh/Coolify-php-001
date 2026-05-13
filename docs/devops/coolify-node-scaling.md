# Coolify Node.js Multi-Core Scaling

This guide explains how to scale your Node.js application across all available CPU cores when deploying with Coolify.

## The Problem
By default, Node.js (as well as Bun and Deno) runs on a **single CPU core**. If you deploy your application on a server with multiple CPU cores, your app will only utilize one of them, regardless of how much traffic you receive.

## The Solution: PM2 Cluster Mode
To utilize all available CPU cores in a Node.js environment, the most common solution is using **PM2** in **Cluster Mode**. This allows you to launch multiple instances of your application that share the same port.

### Technical Background
When using PM2 Cluster Mode, PM2 acts as a process manager that spawns multiple "worker" processes. It uses Node.js's native `cluster` module under the hood, which handles load balancing across these processes.

## Implementation Examples

### Dockerfile Example (PM2 Cluster Mode)
If you are using a custom `Dockerfile`, you can integrate PM2 as follows:

```dockerfile
FROM node:20-slim

WORKDIR /app

# Install PM2 globally
RUN npm install pm2 -g

COPY package*.json ./
RUN npm install --production

COPY . .

# Expose the application port
EXPOSE 3000

# Start the application in cluster mode (max instances)
CMD ["pm2-runtime", "start", "server.js", "-i", "max"]
```

### Nixpacks Example
If you are using Coolify's default Nixpacks builder, you have two options:

#### Option A: Environment Variable
Set the following environment variable in the Coolify dashboard for your application:
`NIXPACKS_START_CMD=pm2-runtime start server.js -i max`

#### Option B: nixpacks.toml
Create a `nixpacks.toml` file in your repository root:

```toml
[start]
cmd = "pm2-runtime start server.js -i max"
```

## Alternatives

### Bun (One-Line Multi-Core)
Bun supports multi-core scaling natively via the `--reuse-port` flag or using `SO_REUSEPORT` at the OS level.

**Dockerfile:**
```dockerfile
CMD ["bun", "run", "--reuse-port", "index.ts"]
```

### Deno (One-Line Multi-Core)
Similar to Bun, Deno can handle multiple processes on the same port.

**Dockerfile:**
```dockerfile
CMD ["deno", "run", "--allow-net", "main.ts"]
```

## Caveats
- **State Management**: Since processes are independent, you cannot share in-memory state (like global variables or local sessions) across cores. Use Redis or a database for shared state.
- **WebSocket Synchronization**: If using WebSockets, you will need a Pub/Sub mechanism (like Redis) to broadcast messages across all worker processes.
- **Memory Usage**: Each worker process has its own memory overhead. Ensure your server has enough RAM for `N` instances of your app.

## Best Practices for Node.js Performance

### 1. Memory Management
Docker containers often have memory limits. You should tell Node.js about these limits to prevent `OOMKilled` errors:
```bash
# In your Dockerfile or Start Command
node --max-old-space-size=2048 server.js
```

### 2. Graceful Shutdown
Ensure your application handles `SIGINT` and `SIGTERM` signals to allow PM2 to shut down processes cleanly without dropping active connections.

### 3. Use a Reverse Proxy
Coolify uses Traefik by default, which is excellent. Ensure you are not doing heavy SSL termination or complex routing inside your Node.js app when the proxy can handle it.

## Verifying Multi-Core Use
Once deployed, you can verify that multiple processes are running by executing the following command in the container's terminal:

```bash
pm2 list
# OR
top
```
You should see multiple instances of your application process running.
