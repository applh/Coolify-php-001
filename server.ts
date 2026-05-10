import express from 'express';
import path from 'path';
import fs from 'fs/promises';
import { fileURLToPath } from 'url';
import cors from 'cors';
import db from './database.ts';

// Use DB for logging starts
try {
  db.prepare('CREATE TABLE IF NOT EXISTS logs (message TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)').run();
  db.prepare('INSERT INTO logs (message) VALUES (?)').run('Server starting...');
} catch (err) {
  console.error('Database logging failed:', err);
}

console.log('Server.ts: Initializing...');

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const isProd = process.env.NODE_ENV === 'production' || process.env.PROD === 'true';

async function createServer() {
  const app = express();
  app.use(express.json());
  app.use(cors());

  app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);
    next();
  });

  // Since we run server.ts with tsx, __dirname is always the project root
  const rootDir = __dirname;
  const repoPhpPath = path.join(rootDir, 'repo-php');
  let contentPath = path.join(repoPhpPath, 'my-data');
  
  // Check if my-data exists, fallback to content
  try {
    await fs.access(contentPath);
    console.log('Using persistent data from my-data');
  } catch {
    contentPath = path.join(repoPhpPath, 'content');
    console.log('Using default templates from content');
  }

  // Ensure resolved content path exists
  try {
    await fs.mkdir(contentPath, { recursive: true });
  } catch (err) {
    console.error('Failed to ensure directory exists:', contentPath, err);
  }

  // API Routes
  app.get('/api/sites', async (req, res) => {
    try {
      const folders = await fs.readdir(contentPath, { withFileTypes: true });
      const sites = folders
        .filter(dirent => dirent.isDirectory())
        .map(dirent => dirent.name);
      res.json(sites);
    } catch (error) {
      console.error('API Error /sites:', error);
      res.status(500).json({ error: 'Failed to read sites' });
    }
  });

  app.get('/api/sites/:site/files', async (req, res) => {
    const site = req.params.site;
    const sitePath = path.join(contentPath, site);
    try {
      const files = await fs.readdir(sitePath);
      res.json(files);
    } catch (error) {
       console.error(`API Error /sites/${site}/files:`, error);
       res.status(500).json({ error: 'Failed to read files' });
    }
  });

  app.get('/api/sites/:site/files/:file', async (req, res) => {
    const { site, file } = req.params;
    const filePath = path.join(contentPath, site, file);
    try {
      const content = await fs.readFile(filePath, 'utf-8');
      res.json({ content });
    } catch (error) {
       console.error(`API Error /sites/${site}/files/${file}:`, error);
       res.status(500).json({ error: 'Failed to read file' });
    }
  });

  app.post('/api/sites/:site/files/:file', async (req, res) => {
    const { site, file } = req.params;
    const { content } = req.body;
    const filePath = path.join(contentPath, site, file);
    try {
      await fs.writeFile(filePath, content, 'utf-8');
      res.json({ success: true });
    } catch (error) {
       console.error(`API Error saving /sites/${site}/files/${file}:`, error);
       res.status(500).json({ error: 'Failed to save file' });
    }
  });

  app.get('/api/explorer', async (req, res) => {
    try {
      const explorePath = req.query.path ? String(req.query.path) : '';
      const fullPath = path.join(repoPhpPath, explorePath);
      
      // Basic security check to prevent directory traversal
      if (!fullPath.startsWith(repoPhpPath)) {
        return res.status(403).json({ error: 'Access denied' });
      }

      const stats = await fs.stat(fullPath);
      if (stats.isDirectory()) {
         const items = await fs.readdir(fullPath, { withFileTypes: true });
         const results = items.map(item => ({
             name: item.name,
             isDirectory: item.isDirectory(),
             path: path.join(explorePath, item.name).replace(/\\/g, '/')
         }));
         res.json({ type: 'directory', items: results });
      } else {
         const content = await fs.readFile(fullPath, 'utf-8');
         res.json({ type: 'file', content });
      }
    } catch (error) {
      console.error(`API Error /explorer:`, error);
      res.status(500).json({ error: 'Failed to explore path' });
    }
  });

  // Frontend routes
  if (!isProd) {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
      root: process.cwd(),
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(rootDir, 'dist');
    app.use(express.static(distPath));
    app.get(/.*/, (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  const PORT = 3000;
  try {
    app.listen(PORT, () => {
      console.log(`CMS Server successfully started and listening at http://localhost:${PORT}`);
    });
  } catch (err) {
    console.error('Failed to start server:', err);
  }
}

console.log('Server.ts: Calling createServer()...');
createServer().catch(err => {
  console.error('Fatal error in createServer:', err);
});
