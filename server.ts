import express from "express";
import ViteExpress from "vite-express";

const app = express();

app.use(express.json());

// State
let agentsEnabled = false; // "Check that agents are disabled at server startup."

const ADMIN_PASSKEY = process.env.APP_ADMIN_PASSKEY || "defaultpasskey";

// Endpoint to authenticate
app.post("/api/login", (req, res) => {
  const { passkey } = req.body;
  if (passkey === ADMIN_PASSKEY) {
    res.json({ success: true, token: "admin-token-123" });
  } else {
    res.status(401).json({ success: false, error: "Invalid passkey" });
  }
});

// Middleware for auth
const authMiddleware = (req: express.Request, res: express.Response, next: express.NextFunction) => {
  const token = req.headers.authorization;
  if (token === "Bearer admin-token-123") {
    next();
  } else {
    res.status(403).json({ error: "Unauthorized" });
  }
};

// API to manage agents
app.get("/api/agents/status", authMiddleware, (req, res) => {
  res.json({ enabled: agentsEnabled });
});

app.post("/api/agents/toggle", authMiddleware, (req, res) => {
  const { enabled } = req.body;
  agentsEnabled = !!enabled;
  console.log(`Agents status updated: ${agentsEnabled ? "Enabled" : "Disabled"}`);
  res.json({ success: true, enabled: agentsEnabled });
});

const PORT = 3000;
ViteExpress.listen(app, PORT, () => {
  console.log(`Server is listening on port ${PORT}...`);
  console.log(`Agents are initially disabled: ${agentsEnabled}`);
});
