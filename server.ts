import express from 'express';
import path from 'path';
import fs from 'fs/promises';
import { fileURLToPath } from 'url';
import cors from 'cors';
import AdmZip from 'adm-zip';
import db from './database.ts';

// Use DB for logging starts
try {
  db.prepare('CREATE TABLE IF NOT EXISTS logs (message TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)').run();
  db.prepare('INSERT INTO logs (message) VALUES (?)').run('Server starting...');
  
  db.prepare(`
    CREATE TABLE IF NOT EXISTS media_tasks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        site_id TEXT NOT NULL,
        target_path TEXT NOT NULL,
        prompt TEXT NOT NULL,
        status TEXT DEFAULT 'pending',
        error_message TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `).run();
} catch (err) {
  console.error('Database logging failed:', err);
}

console.log('Server.ts: Initializing...');

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const isProd = process.env.NODE_ENV === 'production' || process.env.PROD === 'true';

async function createServer() {
  const app = express();
  app.use(express.json({ limit: '10mb' }));
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

  // Restore images from individual .zip files for Git tracking
  try {
    const sitesDir = path.join(repoPhpPath, 'content');
    const sitesDirExists = await fs.access(sitesDir).then(() => true).catch(() => false);
    
    if (sitesDirExists) {
        async function walkAndRestore(dir: string) {
            const entries = await fs.readdir(dir, { withFileTypes: true });
            for (const entry of entries) {
                const fullPath = path.join(dir, entry.name);
                if (entry.isDirectory()) {
                    await walkAndRestore(fullPath);
                } else if (entry.name.endsWith('.zip')) {
                    const originalFile = fullPath.slice(0, -4);
                    try {
                        await fs.access(originalFile);
                    } catch {
                        // Original missing, restore from zip
                        console.log(`Restoring ${originalFile} from zip...`);
                        const zip = new AdmZip(fullPath);
                        const zipEntries = zip.getEntries();
                        if (zipEntries.length > 0) {
                            const buffer = zipEntries[0].getData();
                            await fs.writeFile(originalFile, buffer);
                            console.log(`  Restored ${originalFile}`);
                        }
                    }
                }
            }
        }
        await walkAndRestore(sitesDir);
    }
  } catch (err) {
    console.error('Error restoring media from individual zips:', err);
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

  // Media Tasks API
  app.get('/api/media-files', (req, res) => {
    try {
      const targetPath = String(req.query.path || '');
      const fullPath = path.join(repoPhpPath, targetPath);
      if (!fullPath.startsWith(repoPhpPath)) {
        return res.status(403).json({ error: 'Access denied' });
      }
      res.sendFile(fullPath);
    } catch {
      res.status(404).send('Not found');
    }
  });

  app.post('/api/media-tasks', (req, res) => {
    try {
      const { site_id, target_path, prompt } = req.body;
      const stmt = db.prepare(`
          INSERT INTO media_tasks (site_id, target_path, prompt) 
          VALUES (?, ?, ?)
      `);
      const info = stmt.run(site_id, target_path, prompt);
      res.json({ success: true, taskId: info.lastInsertRowid });
    } catch(err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.get('/api/media-tasks', (req, res) => {
    try {
      const tasks = db.prepare("SELECT * FROM media_tasks ORDER BY created_at DESC").all();
      res.json(tasks);
    } catch(err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.put('/api/media-tasks/:id', (req, res) => {
    try {
      const { status, error_message } = req.body;
      const stmt = db.prepare("UPDATE media_tasks SET status = ?, error_message = ? WHERE id = ?");
      stmt.run(status, error_message || null, req.params.id);
      res.json({ success: true });
    } catch(err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  // Base64 Save endpoint
  app.post('/api/media/save-base64', async (req, res) => {
    try {
      const { imageBase64, targetPath } = req.body;
      
      if (!imageBase64 || !targetPath) {
         return res.status(400).json({ error: 'Missing image data or target path.' });
      }

      // Convert base64 back to binary data
      const buffer = Buffer.from(imageBase64, 'base64');

      const fullPath = path.join(repoPhpPath, targetPath);
      
      // Security check to avoid path traversal
      if (!fullPath.startsWith(repoPhpPath)) {
          return res.status(403).json({ error: 'Invalid save path' });
      }

      // Ensure directory exists
      await fs.mkdir(path.dirname(fullPath), { recursive: true });

      // Save to the filesystem
      await fs.writeFile(fullPath, buffer);

      // Save individual zip for Git tracking
      try {
          const indivZip = new AdmZip();
          indivZip.addFile(path.basename(fullPath), buffer);
          indivZip.writeZip(fullPath + '.zip');
      } catch (err) {
          console.error('Failed to create individual media zip', err);
      }

      res.json({ success: true, savedPath: targetPath });

    } catch (error: unknown) {
      console.error('File Save Error:', error);
      res.status(500).json({ error: 'Failed to save generated image' });
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
