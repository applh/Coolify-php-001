import Database from 'better-sqlite3';
import path from 'path';
import { mkdirSync } from 'fs';

const dbPath = path.join(process.cwd(), 'data', 'cms.db');
const dbDir = path.dirname(dbPath);

try {
  mkdirSync(dbDir, { recursive: true });
} catch {
  // Directory might already exist
}

const db = new Database(dbPath);

// Initialize tables
db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE,
    password TEXT
  );

  CREATE TABLE IF NOT EXISTS sites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    domain TEXT UNIQUE,
    folder_name TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE IF NOT EXISTS agents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    role TEXT NOT NULL,
    skills TEXT,
    status TEXT DEFAULT 'idle',
    avatar_url TEXT
  );

  CREATE TABLE IF NOT EXISTS agent_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    agent_id INTEGER,
    title TEXT NOT NULL,
    description TEXT,
    input_data TEXT,
    output_data TEXT,
    status TEXT DEFAULT 'queued',
    priority INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(agent_id) REFERENCES agents(id)
  );

  CREATE TABLE IF NOT EXISTS version_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tag_name TEXT UNIQUE,
    message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );
`);

// Seed default agents if none exist
const agentsCount = db.prepare('SELECT COUNT(*) as count FROM agents').get() as { count: number };
if (agentsCount.count === 0) {
  const seedAgents = [
    { name: 'Aria', role: 'System Architect', skills: JSON.stringify(['Architecture Design', 'Database Modeling', 'API Planning']) },
    { name: 'Cypher', role: 'Security Analyst', skills: JSON.stringify(['Penetration Testing', 'Code Audit', 'Encryption']) },
    { name: 'Lumina', role: 'UX/UI Specialist', skills: JSON.stringify(['Interface Design', 'Accessibility', 'Prototype']) },
    { name: 'Kael', role: 'Full-stack Developer', skills: JSON.stringify(['React', 'Node.js', 'PostgreSQL']) },
    { name: 'Echo', role: 'DevOps Engineer', skills: JSON.stringify(['CI/CD', 'Docker', 'Cloud Scaling']) },
  ];

  const insertStmt = db.prepare('INSERT INTO agents (name, role, skills) VALUES (?, ?, ?)');
  for (const agent of seedAgents) {
    insertStmt.run(agent.name, agent.role, agent.skills);
  }
  console.log('Seeded default AI agents.');
}

export default db;
