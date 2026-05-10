import express from 'express';
import { createServer as createViteServer } from 'vite';
import path from 'path';
import fs from 'fs/promises';
import { fileURLToPath } from 'url';
import cors from 'cors';
import db from './database.ts';

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

  const repoPhpPath = path.join(__dirname, 'repo-php');
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
       res.status(500).json({ error: 'Failed to save file' });
    }
  });

  // Frontend routes
  if (!isProd) {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
      root: process.cwd(),
    });
    app.use(vite.middlewares);
  } else {
    app.use(express.static(path.resolve(__dirname, 'dist')));
    app.get('*', (req, res) => {
      res.sendFile(path.resolve(__dirname, 'dist/index.html'));
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
