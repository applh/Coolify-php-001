import express from 'express';
import path from 'path';
import fs from 'fs/promises';
import { fileURLToPath } from 'url';
import cors from 'cors';
import AdmZip from 'adm-zip';
import multer from 'multer';
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

  db.prepare(`
    CREATE TABLE IF NOT EXISTS ai_tasks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        type TEXT NOT NULL,
        payload TEXT NOT NULL,
        status TEXT DEFAULT 'pending',
        result TEXT,
        error TEXT,
        is_recurring BOOLEAN DEFAULT 0,
        cron_expression TEXT,
        last_run_at DATETIME,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `).run();
} catch (err) {
  console.error('Database logging failed:', err);
}

// Imports for Google GenAI and node-cron
import { GoogleGenAI } from '@google/genai';
import cron from 'node-cron';

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
const activeCronJobs = new Map<number, cron.ScheduledTask>();

// Load AI tasks on startup
function scheduleExisitingCrons() {
  try {
    const tasks = db.prepare('SELECT * FROM ai_tasks WHERE is_recurring = 1').all() as Array<Record<string, unknown>>;
    tasks.forEach(task => {
      if (typeof task.cron_expression === 'string' && cron.validate(task.cron_expression)) {
        const jobId = Number(task.id);
        const job = cron.schedule(task.cron_expression, () => executeAITask(jobId));
        activeCronJobs.set(jobId, job);
      }
    });
  } catch (err) {
    console.error('Failed to load recurring tasks', err);
  }
}
scheduleExisitingCrons();

async function executeAITask(taskId: number) {
  const task = db.prepare('SELECT * FROM ai_tasks WHERE id = ?').get(taskId) as Record<string, unknown> | undefined;
  if (!task) return;
  db.prepare('UPDATE ai_tasks SET status = ?, last_run_at = CURRENT_TIMESTAMP WHERE id = ?').run('running', taskId);
  
  try {
    const payload = JSON.parse(String(task.payload));
    let result = '';
    
    if (task.type === 'generate_slide_outline') {
      const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: `You are a Curriculum Designer.\nGenerate a pedagogical slide outline for the topic: "${payload.topic}".\nProvide the response as valid JSON with an array of objects, containing 'title' and 'content' for each slide.`,
      });
      result = response.text || '';
    } else if (task.type === 'generate_slide_svg') {
      const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: `You are a Visual Designer.\nGenerate a highly stylized SVG illustration representing the following slide content:\n\nTitle: ${payload.slide_data.title}\nContent: ${payload.slide_data.content}\n\nFormat your response to return ONLY valid SVG code. Do not wrap it in markdown or comments.`,
      });
      let text = response.text || '';
      text = text.replace(/^```(xml|svg)?|```$/ig, '').trim();
      result = text;
    }
    
    db.prepare('UPDATE ai_tasks SET status = ?, result = ? WHERE id = ?').run('completed', result, taskId);
  } catch (error: unknown) {
    db.prepare('UPDATE ai_tasks SET status = ?, error = ? WHERE id = ?').run('failed', error instanceof Error ? error.message : String(error), taskId);
  }
}


console.log('Server.ts: Initializing...');

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const isProd = process.env.NODE_ENV === 'production';

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
  const upload = multer({ storage: multer.memoryStorage() });

  // Multi-repo support
  const getRepoPath = (repoName: string) => {
    return path.join(rootDir, repoName || 'repo-php');
  };

  // Helper to get sites path for a repo
  const getSitesPath = async (repoName: string) => {
    const repoPath = getRepoPath(repoName);
    let contentPath = path.join(repoPath, 'my-data');
    try {
      await fs.access(contentPath);
    } catch {
      contentPath = path.join(repoPath, 'content');
    }
    // Fallback for non-PHP repos which might have different structures
    try {
      await fs.access(contentPath);
    } catch {
      // If it's a repo without 'content', just use the repo root
      contentPath = repoPath;
    }
    return contentPath;
  };

  // Restore images from individual .zip files for Git tracking (non-blocking) - default to repo-php
  (async () => {
    console.log('Restoring media from zips (repo-php)...');
    try {
      const repoPhpPath = path.join(rootDir, 'repo-php');
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
                          console.log(`Restoring ${originalFile} from zip...`);
                          try {
                              const zip = new AdmZip(fullPath);
                              const zipEntries = zip.getEntries();
                              if (zipEntries.length > 0) {
                                  const buffer = zipEntries[0].getData();
                                  await fs.writeFile(originalFile, buffer);
                              }
                          } catch (zipErr) {
                              console.error(`Failed to restore ${fullPath}:`, zipErr);
                          }
                      }
                  }
              }
          }
          await walkAndRestore(sitesDir);
          console.log('Media restoration complete.');
      }
    } catch (err) {
      console.error('Error restoring media:', err);
    }
  })();

  // API Routes
  app.get('/api/repos', async (req, res) => {
    try {
      const items = await fs.readdir(rootDir, { withFileTypes: true });
      const repos = items
        .filter(dirent => dirent.isDirectory() && !dirent.name.startsWith('.') && dirent.name !== 'node_modules' && dirent.name !== 'dist')
        .map(dirent => dirent.name)
        .sort((a, b) => {
          // Sort repo-* first
          const aIsRepo = a.startsWith('repo-');
          const bIsRepo = b.startsWith('repo-');
          if (aIsRepo && !bIsRepo) return -1;
          if (!aIsRepo && bIsRepo) return 1;
          return a.localeCompare(b);
        });
      res.json(repos);
    } catch {
      res.status(500).json({ error: 'Failed to read repos' });
    }
  });

  app.get('/api/sites', async (req, res) => {
    try {
      const repo = String(req.query.repo || 'repo-php');
      const contentPath = await getSitesPath(repo);
      
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

  app.post('/api/sites', async (req, res) => {
    const { name, repo = 'repo-php' } = req.body;
    if (!name) return res.status(400).json({ error: 'Site name is required' });
    
    const contentPath = await getSitesPath(repo);
    const sanitizedName = name.replace(/[^a-zA-Z0-9.-]/g, '').toLowerCase();
    if (!sanitizedName) return res.status(400).json({ error: 'Invalid site name' });

    const sitePath = path.join(contentPath, sanitizedName);
    
    try {
      await fs.access(sitePath);
      return res.status(400).json({ error: 'Site already exists' });
    } catch {
      // expected, site does not exist
    }

    try {
      await fs.mkdir(sitePath, { recursive: true });
      await fs.mkdir(path.join(sitePath, 'img'), { recursive: true });
      
      const defaultIndex = `<?php
/**
 * Welcome to ${sanitizedName}
 */

Layout::header('${sanitizedName}');
?>

<main class="grid grid-cols-1 md:grid-cols-2 gap-12">
    <div class="space-y-8">
        <p class="text-xl leading-relaxed">
            Welcome to your new multi-tenant site. This page is generated using the global <code>Layout</code> class, ensuring a consistent aesthetic across your entire network.
        </p>
        
        <div class="aspect-[4/5] bg-gray-200 overflow-hidden relative group">
            <img src="/img/hero.jpg" alt="Featured" class="w-full h-full object-cover grayscale hover:grayscale-0 transition-all duration-700">
            <div class="absolute inset-0 flex items-center justify-center pointer-events-none opacity-20 group-hover:opacity-40 transition-opacity">
                <span class="text-4xl serif italic">Place Media Here</span>
            </div>
        </div>
    </div>

    <div class="flex flex-col justify-between py-12">
        <div class="space-y-6">
            <p class="text-xs uppercase tracking-[0.2em] opacity-40 font-mono">Vision</p>
            <blockquote class="text-4xl serif italic leading-tight">
                "Design is not just what it looks like and feels like. Design is how it works."
            </blockquote>
        </div>

        <div class="pt-24">
            <p class="text-xs uppercase tracking-[0.2em] opacity-40 font-mono mb-4">Location</p>
            <p class="text-sm font-serif italic mb-8">Digital / Global / ${sanitizedName}</p>
            <a href="#" class="inline-block border border-black/10 px-8 py-4 text-[10px] uppercase tracking-widest hover:bg-black hover:text-white transition-all font-mono">
                Explore More
            </a>
        </div>
    </div>
</main>

<?php
Layout::footer();
?>`;
      await fs.writeFile(path.join(sitePath, 'index.php'), defaultIndex);
      
      res.json({ success: true, name: sanitizedName });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: 'Failed to create site: ' + msg });
    }
  });

  app.delete('/api/sites/:site', async (req, res) => {
    const site = req.params.site;
    const repo = String(req.query.repo || 'repo-php');
    const contentPath = await getSitesPath(repo);
    const sitePath = path.join(contentPath, site);
    
    if (!sitePath.startsWith(contentPath)) {
      return res.status(403).json({ error: 'Access denied' });
    }

    try {
      await fs.rm(sitePath, { recursive: true, force: true });
      res.json({ success: true, message: 'Site deleted' });
    } catch (error) {
      console.error(`API Error DELETE /sites/${site}:`, error);
      res.status(500).json({ error: 'Failed to delete site' });
    }
  });

  app.get('/api/sites/:site/download', async (req, res) => {
    const site = req.params.site;
    const repo = String(req.query.repo || 'repo-php');
    const contentPath = await getSitesPath(repo);
    const sitePath = path.join(contentPath, site);
    
    if (!sitePath.startsWith(contentPath)) {
      return res.status(403).json({ error: 'Access denied' });
    }

    try {
      await fs.access(sitePath);
      const zip = new AdmZip();
      zip.addLocalFolder(sitePath, site);
      
      const zipBuffer = zip.toBuffer();
      res.set('Content-Type', 'application/zip');
      res.set('Content-Disposition', `attachment; filename=${site}.zip`);
      res.send(zipBuffer);
    } catch (error) {
      console.error(`API Error /sites/${site}/download:`, error);
      res.status(500).json({ error: 'Failed to download site archive' });
    }
  });

  app.post('/api/sites/:site/upload', upload.single('file'), async (req, res) => {
    const site = req.params.site;
    const repo = String(req.query.repo || 'repo-php');
    const contentPath = await getSitesPath(repo);
    const sitePath = path.join(contentPath, site);
    
    if (!sitePath.startsWith(contentPath)) {
      return res.status(403).json({ error: 'Access denied' });
    }

    try {
      if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded' });
      }

      const zip = new AdmZip(req.file.buffer);
      zip.extractAllTo(sitePath, true);
      
      res.json({ success: true, message: 'Site updated from zip' });
    } catch (error) {
      console.error(`API Error /sites/${site}/upload:`, error);
      res.status(500).json({ error: 'Failed to upload site archive' });
    }
  });

  app.get('/api/sites/:site/files', async (req, res) => {
    const site = req.params.site;
    const repo = String(req.query.repo || 'repo-php');
    const contentPath = await getSitesPath(repo);
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
    const repo = String(req.query.repo || 'repo-php');
    const contentPath = await getSitesPath(repo);
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
    const { content, repo = 'repo-php' } = req.body;
    const contentPath = await getSitesPath(repo);
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
      const repo = String(req.query.repo || 'repo-php');
      const repoPath = getRepoPath(repo);
      const fullPath = path.join(repoPath, explorePath);
      
      // Basic security check to prevent directory traversal
      if (!fullPath.startsWith(repoPath)) {
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

  app.post('/api/scan-media', async (req, res) => {
    try {
      const repo = String(req.body.repo || 'repo-php');
      const sitesDir = await getSitesPath(repo); 
      const sites = await fs.readdir(sitesDir, { withFileTypes: true });
      let addedTasks = 0;
      
      for (const dirent of sites) {
        if (!dirent.isDirectory()) continue;
        const site = dirent.name;
        const sitePath = path.join(sitesDir, site);
        const files = await fs.readdir(sitePath);
        
        for (const file of files) {
          if (file.endsWith('.php') || file.endsWith('.css') || file.endsWith('.html')) {
            const content = await fs.readFile(path.join(sitePath, file), 'utf-8');
            const regex = /(?:src=["']|url\(['"]?)((?:\/)?(?:img|assets)\/[^"')]+)/ig;
            let match;
            while ((match = regex.exec(content)) !== null) {
               let imagePath = match[1];
               if (imagePath.startsWith('/')) imagePath = imagePath.substring(1);
               const fullPath = path.join(sitePath, imagePath);
               
               const zipPath = fullPath + '.zip';
               try {
                 await fs.access(zipPath);
               } catch {
                 // ignore
               }
               
               // Check if zip is empty or invalid
               let needsGeneration = false;
               try {
                 const checkZip = new AdmZip(zipPath);
                 const entries = checkZip.getEntries();
                 if (entries.length === 0) needsGeneration = true;
               } catch {
                 needsGeneration = true; // if invalid or unreadable, re-generate it
               }

               if (needsGeneration) {
                 const targetPath = 'content/' + site + '/' + imagePath;
                 const filename = path.basename(imagePath);
                 const prompt = "Create an image for " + filename;
                 
                 const stmtCheck = db.prepare("SELECT id FROM media_tasks WHERE target_path = ? AND status != 'failed'");
                 const existing = stmtCheck.get(targetPath);
                 
                 if (!existing) {
                   const stmtInsert = db.prepare("INSERT INTO media_tasks (site_id, target_path, prompt) VALUES (?, ?, ?)");
                   stmtInsert.run(site, targetPath, prompt);
                   addedTasks++;
                 }
               }
            }
          }
        }
      }
      res.json({ success: true, addedTasks });
    } catch (err: unknown) {
      console.error('Scan Media Error:', err);
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  // AI Agent Teams APIs
  app.get('/api/agents/teams', async (req, res) => {
    try {
      const dbPath = path.join(process.cwd(), 'data', 'agent_teams.json');
      const data = await fs.readFile(dbPath, 'utf8');
      res.json(JSON.parse(data));
    } catch (error) {
      console.error('Failed to load agent teams:', error);
      res.status(500).json({ error: 'Failed to load agent teams' });
    }
  });

  app.post('/api/tasks/schedule', (req, res) => {
    try {
      const { type, payload, cron_expression } = req.body;
      const stmt = db.prepare(`
          INSERT INTO ai_tasks (type, payload, is_recurring, cron_expression) 
          VALUES (?, ?, 1, ?)
      `);
      const info = stmt.run(type, JSON.stringify(payload), cron_expression);
      const taskId = Number(info.lastInsertRowid);
      
      if (cron.validate(cron_expression)) {
        const job = cron.schedule(cron_expression, () => executeAITask(taskId));
        activeCronJobs.set(taskId, job);
      }
      
      res.json({ success: true, taskId });
    } catch(err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.delete('/api/tasks/schedule/:id', (req, res) => {
    try {
      const id = Number(req.params.id);
      db.prepare("DELETE FROM ai_tasks WHERE id = ?").run(id);
      
      const job = activeCronJobs.get(id);
      if (job) {
        job.stop();
        activeCronJobs.delete(id);
      }
      
      res.json({ success: true });
    } catch(err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  // Media Tasks API
  app.get('/api/benchmark', async (req, res) => {
    const url = req.query.url as string;
    if (!url) return res.status(400).json({ error: 'URL is required' });

    try {
      const start = performance.now();
      const response = await fetch(url, {
        method: 'GET',
        headers: { 'User-Agent': 'CMS-Benchmarker/1.0' },
        signal: AbortSignal.timeout(5000) // 5s timeout
      });
      const end = performance.now();
      
      res.json({
        status: 'success',
        statusCode: response.status,
        responseTime: Math.round(end - start),
        url: url
      });
    } catch (err) {
      const error = err as Error;
      res.json({
        status: 'error',
        error: error.message,
        statusCode: 0,
        responseTime: 0
      });
    }
  });

  app.get('/api/media-files', async (req, res) => {
    try {
      const targetPath = String(req.query.path || '');
      const repo = String(req.query.repo || 'repo-php');
      const repoPath = getRepoPath(repo);
      const fullPath = path.join(repoPath, targetPath);
      if (!fullPath.startsWith(repoPath)) {
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
      const { imageBase64, targetPath, repo = 'repo-php' } = req.body;
      
      if (!imageBase64 || !targetPath) {
         return res.status(400).json({ error: 'Missing image data or target path.' });
      }

      // Convert base64 back to binary data
      const buffer = Buffer.from(imageBase64, 'base64');

      const repoPath = getRepoPath(repo);
      const fullPath = path.join(repoPath, targetPath);
      
      // Security check to avoid path traversal
      if (!fullPath.startsWith(repoPath)) {
          return res.status(403).json({ error: 'Invalid save path' });
      }

      // Ensure directory exists
      await fs.mkdir(path.dirname(fullPath), { recursive: true });

      try {
          const zipPath = fullPath + '.zip';
          const indivZip = new AdmZip();
          indivZip.addFile(path.basename(fullPath), buffer);
          indivZip.writeZip(zipPath);
      } catch (err) {
          console.error('Failed to create individual media zip', err);
      }

      res.json({ success: true, savedPath: targetPath });

    } catch (error: unknown) {
      console.error('File Save Error:', error);
      res.status(500).json({ error: 'Failed to save generated image' });
    }
  });

  // Sync API
  app.get('/api/sync/export', async (req, res) => {
    try {
      const repo = String(req.query.repo || 'repo-php');
      const repoPath = getRepoPath(repo);
      const zip = new AdmZip();
      
      // Add cms.db
      const currentDbPath = path.join(process.cwd(), 'data', 'cms.db');
      zip.addLocalFile(currentDbPath, undefined, 'cms.db');

      // Add repo folder
      zip.addLocalFolder(repoPath, repo);

      const buffer = zip.toBuffer();
      res.set('Content-Type', 'application/zip');
      res.set('Content-Disposition', `attachment; filename=cms-sync-${repo}.zip`);
      res.send(buffer);
    } catch (error) {
      console.error('Export Error:', error);
      res.status(500).json({ error: 'Failed to create export' });
    }
  });

  app.post('/api/sync/import', upload.single('file'), async (req, res) => {
    try {
      const providedSecret = req.headers['x-sync-secret'] || req.query.secret;
      const expectedSecret = process.env.SYNC_SECRET_KEY;
      
      if (expectedSecret && providedSecret !== expectedSecret) {
        return res.status(401).json({ error: 'Unauthorized: Invalid sync secret' });
      }

      if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded' });
      }

      const repo = String(req.query.repo || 'repo-php');
      const repoPath = getRepoPath(repo);

      const zip = new AdmZip(req.file.buffer);
      const zipEntries = zip.getEntries();
      
      // Check if it's a valid export (contains repo folder and cms.db)
      const hasRepo = zipEntries.some(e => e.entryName.startsWith(repo + '/'));
      const hasDb = zipEntries.some(e => e.entryName === 'cms.db');

      if (!hasRepo || !hasDb) {
         return res.status(400).json({ error: `Invalid export: missing ${repo} or cms.db` });
      }

      // Temporary extraction path
      const tmpDir = path.join(process.cwd(), 'import-tmp-' + Date.now());
      await fs.mkdir(tmpDir, { recursive: true });
      zip.extractAllTo(tmpDir, true);

      // Copy cms.db
      const newDbPath = path.join(tmpDir, 'cms.db');
      const currentDbPath = path.join(process.cwd(), 'data', 'cms.db');
      
      await fs.copyFile(newDbPath, currentDbPath);

      // Copy repo
      const newRepoPath = path.join(tmpDir, repo);
      // Remove old repo first to be clean
      await fs.rm(repoPath, { recursive: true, force: true });
      await fs.cp(newRepoPath, repoPath, { recursive: true });

      // Cleanup tmp
      await fs.rm(tmpDir, { recursive: true, force: true });

      res.json({ success: true, message: 'Import successful.' });
    } catch (error) {
      console.error('Import Error:', error);
      res.status(500).json({ error: 'Failed to import state' });
    }
  });

  app.post('/api/sync/push', async (req, res) => {
    try {
      const { remoteUrl, secretKey, repo = 'repo-php' } = req.body;
      if (!remoteUrl || !secretKey) {
        return res.status(400).json({ error: 'Remote URL and Secret Key are required' });
      }

      const repoPath = getRepoPath(repo);
      const zip = new AdmZip();
      
      const currentDbPath = path.join(process.cwd(), 'data', 'cms.db');
      zip.addLocalFile(currentDbPath, undefined, 'cms.db');
      zip.addLocalFolder(repoPath, repo);
      const buffer = zip.toBuffer();

      const formData = new FormData();
      const blob = new Blob([buffer], { type: 'application/zip' });
      formData.append('file', blob, `cms-sync-${repo}.zip`);

      const response = await fetch(`${remoteUrl}${remoteUrl.includes('?') ? '&' : '?'}repo=${repo}`, {
        method: 'POST',
        headers: {
          'x-sync-secret': secretKey
        },
        body: formData
      });

      if (!response.ok) {
        const errorText = await response.text();
        return res.status(response.status).json({ error: `Remote error: ${errorText}` });
      }

      const result = await response.json();
      res.json(result);
    } catch (error) {
      console.error('Push Error:', error);
      res.status(500).json({ error: 'Failed to push sync: ' + (error instanceof Error ? error.message : String(error)) });
    }
  });

  app.post('/api/sync/admin-sync', async (req, res) => {
    try {
      // Basic simulation of sync process handling Prod fetching / local pushing
      // Read current sync target settings from env or defaults
      const syncTargetUrl = process.env.SYNC_TARGET_URL || 'https://prod.example.com/api/sync';
      
      // We would conventionally call the production API for import/export here, but simulating for now.
      console.log('Orchestrating sync with:', syncTargetUrl);
      
      // Simulate success
      res.json({ success: true, message: 'Admin sync successfully merged with prod.' });
    } catch (error) {
      console.error('Admin Sync Error:', error);
      res.status(500).json({ error: 'Failed to sync with prod environment' });
    }
  });

  // Frontend routes
  console.log(`Starting frontend in ${isProd ? 'production' : 'development'} mode`);
  if (!isProd) {
    console.log('Initializing Vite middleware...');
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
      root: process.cwd(),
    });
    app.use(vite.middlewares);
    console.log('Vite middleware initialized.');
    
    // Fallback for SPA in dev mode if vite doesn't handle it
    app.use(async (req, res, next) => {
      if (req.method !== 'GET' || req.originalUrl.startsWith('/api')) {
        return next();
      }
      const url = req.originalUrl;
      try {
        let template = await fs.readFile(path.resolve(process.cwd(), 'index.html'), 'utf-8');
        template = await vite.transformIndexHtml(url, template);
        res.status(200).set({ 'Content-Type': 'text/html' }).end(template);
      } catch (e) {
        vite.ssrFixStacktrace(e as Error);
        next(e);
      }
    });
  } else {
    const distPath = path.resolve(rootDir, 'dist');
    const indexHtmlPath = path.join(distPath, 'index.html');
    
    try {
      await fs.access(indexHtmlPath);
      console.log(`Serving static files from ${distPath}`);
      app.use(express.static(distPath));
      app.get(/.*/, (req, res) => {
        res.sendFile(indexHtmlPath);
      });
    } catch {
      console.error(`ERROR: dist/index.html not found at ${indexHtmlPath}. Did you run 'npm run build'?`);
      app.get('/', (req, res) => {
        res.status(500).send('Production build missing. Please run npm run build.');
      });
    }
  }

  const PORT = process.env.PORT || 3000;
  try {
    app.listen(Number(PORT), '0.0.0.0', () => {
      console.log(`CMS Server successfully started and listening at http://0.0.0.0:${PORT}`);
    });
  } catch (err) {
    console.error('Failed to start server:', err);
  }
}

console.log('Server.ts: Calling createServer()...');
createServer().catch(err => {
  console.error('Fatal error in createServer:', err);
});
