import express from 'express';
import path from 'path';
import fs from 'fs/promises';
import { fileURLToPath } from 'url';
import { execFile } from 'child_process';
import { promisify } from 'util';
import cors from 'cors';
import AdmZip from 'adm-zip';
import multer from 'multer';
import db from './database.ts';
import { initScheduler } from './scheduler.ts';
import { GoogleGenAI } from "@google/genai";

// Initialize Gemini
const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY || '',
  httpOptions: {
    headers: {
      'User-Agent': 'aistudio-build',
    }
  }
});

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
const isProd = process.env.NODE_ENV === 'production';

const execFileAsync = promisify(execFile);

async function getGitTags() {
  try {
    // Check if git repository is initialized
    await execFileAsync('git', ['rev-parse', '--is-inside-work-tree']);
  } catch {
    // If not, initialize it so git features can actually work
    try {
      await execFileAsync('git', ['init']);
      await execFileAsync('git', ['config', '--global', 'user.name', 'AI Coder Agent']);
      await execFileAsync('git', ['config', '--global', 'user.email', 'fraise-agent@ais.local']);
      
      const status = await execFileAsync('git', ['status', '--short']);
      if (status.stdout.trim()) {
        await execFileAsync('git', ['add', '.']);
        await execFileAsync('git', ['commit', '-m', 'Initial commit']);
      }
    } catch (e) {
      console.error('Failed to auto-init git repository:', e);
    }
  }

  try {
    // Fetch real git tags.
    const { stdout } = await execFileAsync('git', [
      'tag', 
      '-l', 
      '--format=%(refname:short)|%(creatordate:iso)|%(contents:subject)'
    ]);
    
    const lines = stdout.trim().split('\n').filter(line => line.length > 0);
    const tags = lines.map(line => {
      const parts = line.split('|');
      return {
        tag_name: parts[0] || '',
        created_at: parts[1] || new Date().toISOString(),
        message: parts[2]?.trim() || '',
        is_git: true
      };
    });
    return tags;
  } catch (err) {
    console.error('Real git tag retrieval failed, falling back to SQLite db:', err);
    return null;
  }
}

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

  // Authentication Middleware
  const adminPasskey = process.env.APP_ADMIN_PASSKEY;
  
  app.post('/api/auth/verify', (req, res) => {
    const { passkey } = req.body;
    if (!adminPasskey) {
      return res.json({ success: true, noPasskey: true });
    }
    if (passkey === adminPasskey) {
      return res.json({ success: true });
    }
    return res.status(401).json({ success: false, error: 'Invalid passkey' });
  });

  // Protect all API routes except auth
  app.use('/api', (req, res, next) => {
    if (req.path === '/auth/verify' || !adminPasskey) {
      return next();
    }
    
    const providedPasskey = req.headers['x-admin-passkey'] || req.query.passkey;
    if (providedPasskey === adminPasskey) {
      return next();
    }
    
    res.status(401).json({ error: 'Unauthorized: Admin passkey required' });
  });

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

  // AI Agents Team API
  app.get('/api/agents', (req, res) => {
    try {
      const agents = db.prepare("SELECT * FROM agents").all();
      res.json(agents);
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.post('/api/agents', (req, res) => {
    try {
      const { name, role, skills, avatar_url } = req.body;
      const stmt = db.prepare("INSERT INTO agents (name, role, skills, avatar_url) VALUES (?, ?, ?, ?)");
      const info = stmt.run(name, role, JSON.stringify(skills), avatar_url || null);
      res.json({ success: true, id: info.lastInsertRowid });
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.put('/api/agents/:id', (req, res) => {
    try {
      const { name, role, skills, status, avatar_url } = req.body;
      const stmt = db.prepare("UPDATE agents SET name = ?, role = ?, skills = ?, status = ?, avatar_url = ? WHERE id = ?");
      stmt.run(name, role, JSON.stringify(skills), status, avatar_url || null, req.params.id);
      res.json({ success: true });
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.delete('/api/agents/:id', (req, res) => {
    try {
      db.prepare("DELETE FROM agents WHERE id = ?").run(req.params.id);
      res.json({ success: true });
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.get('/api/agent-tasks', (req, res) => {
    try {
      const tasks = db.prepare(`
        SELECT t.*, a.name as agent_name 
        FROM agent_tasks t 
        LEFT JOIN agents a ON t.agent_id = a.id 
        ORDER BY t.priority DESC, t.created_at DESC
      `).all();
      res.json(tasks);
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.post('/api/agent-tasks', (req, res) => {
    try {
      const { agent_id, title, description, input_data, priority } = req.body;
      const stmt = db.prepare(`
        INSERT INTO agent_tasks (agent_id, title, description, input_data, priority) 
        VALUES (?, ?, ?, ?, ?)
      `);
      const info = stmt.run(agent_id || null, title, description || null, input_data || null, priority || 0);
      res.json({ success: true, id: info.lastInsertRowid });
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.put('/api/agent-tasks/:id', (req, res) => {
    try {
      const { status, output_data, agent_id } = req.body;
      const stmt = db.prepare("UPDATE agent_tasks SET status = ?, output_data = ?, agent_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?");
      stmt.run(status, output_data || null, agent_id || null, req.params.id);
      res.json({ success: true });
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.post('/api/agent-tasks/:id/run', async (req, res) => {
    const taskId = req.params.id;
    try {
      const task = db.prepare(`
        SELECT t.*, a.name as agent_name, a.role as agent_role, a.skills as agent_skills 
        FROM agent_tasks t 
        LEFT JOIN agents a ON t.agent_id = a.id 
        WHERE t.id = ?
      `).get(taskId) as { 
        id: number; 
        agent_id: number | null; 
        title: string; 
        description: string | null; 
        input_data: string | null;
        agent_name: string | null;
        agent_role: string | null;
        agent_skills: string | null;
      } | undefined;

      if (!task) return res.status(404).json({ error: 'Task not found' });

      // Update status to in-progress
      db.prepare("UPDATE agent_tasks SET status = 'in-progress', updated_at = CURRENT_TIMESTAMP WHERE id = ?").run(taskId);
      if (task.agent_id) {
        db.prepare("UPDATE agents SET status = 'working' WHERE id = ?").run(task.agent_id);
      }

      // Prepare prompt
      const agentContext = task.agent_id ? 
        `You are ${task.agent_name}, a ${task.agent_role} with skills: ${task.agent_skills}.` :
        `You are a general AI agent.`;
      
      const prompt = `
        ${agentContext}
        
        Objective: ${task.title}
        Context: ${task.description || 'No additional context provided.'}
        Input Data: ${task.input_data || 'None'}
        
        Please provide a detailed response or artifact as the result of this task.
      `;

      // Call Gemini
      const result = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: prompt,
      });

      const output = result.text || 'No output generated.';

      // Update task with result
      db.prepare("UPDATE agent_tasks SET status = 'completed', output_data = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?").run(output, taskId);
      if (task.agent_id) {
        db.prepare("UPDATE agents SET status = 'idle' WHERE id = ?").run(task.agent_id);
      }

      res.json({ success: true, output });

    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err);
      console.error('AI Task Execution failed:', errorMsg);
      db.prepare("UPDATE agent_tasks SET status = 'failed', output_data = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?").run(errorMsg, taskId);
      res.status(500).json({ error: errorMsg });
    }
  });

  app.get('/api/android/download', async (req, res) => {
    const apkPath = path.join(rootDir, 'repo-android', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
    try {
      const stats = await fs.stat(apkPath);
      const date = stats.mtime;
      const YY = String(date.getFullYear()).slice(-2);
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const dd = String(date.getDate()).padStart(2, '0');
      const H = String(date.getHours()).padStart(2, '0');
      const M = String(date.getMinutes()).padStart(2, '0');
      const S = String(date.getSeconds()).padStart(2, '0');
      const filename = `fraise-${YY}${mm}${dd}-${H}${M}${S}.apk`;
      
      res.download(apkPath, filename);
    } catch {
      res.status(404).json({ error: 'APK build not found. Please run build in repo-android.' });
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

  app.post('/api/generate-image', async (req, res) => {
    try {
      const prompt = req.body.prompt;
      const apiKey = req.body.apiKey;
      if (!prompt) {
         return res.status(400).json({ error: 'Prompt is required' });
      }
      const client = new GoogleGenAI({ apiKey: apiKey || process.env.GEMINI_API_KEY, httpOptions: { headers: { 'User-Agent': 'aistudio-build' } } });
      const response = await client.models.generateContent({
        model: 'gemini-3.1-flash-image-preview',
        contents: prompt,
        config: {
            imageConfig: {
                aspectRatio: "1:1",
                imageSize: "1K"
            }
        }
      });
      let base64Data = null;
      if (response.candidates && response.candidates[0].content.parts) {
          for (const part of response.candidates[0].content.parts) {
             if (part.inlineData) {
                 base64Data = part.inlineData.data;
                 break;
             }
          }
      }
      if (!base64Data) throw new Error('No image generated by AI.');
      res.json({ base64Data });
    } catch (e: unknown) {
      const errorMsg = e instanceof Error ? e.message : String(e);
      console.error('Image Generation Error:', e);
      res.status(500).json({ error: errorMsg || 'Image generation failed' });
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

  // Git Version Tagging Router
  app.get('/api/git/tags', async (req, res) => {
    try {
      let tags = await getGitTags();
      
      const dbTags = db.prepare("SELECT tag_name, message, created_at FROM version_tags ORDER BY created_at DESC").all() as Array<{tag_name: string; message: string; created_at: string}>;
      
      if (!tags) {
        tags = dbTags.map(t => ({
          tag_name: t.tag_name,
          message: t.message,
          created_at: t.created_at,
          is_git: false
        }));
      } else {
        const gitTagNames = new Set(tags.map(t => t.tag_name));
        for (const dbT of dbTags) {
          if (!gitTagNames.has(dbT.tag_name)) {
            tags.push({
              tag_name: dbT.tag_name,
              message: dbT.message,
              created_at: dbT.created_at,
              is_git: false
            });
          }
        }
      }

      tags.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());
      res.json({ success: true, tags });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: 'Failed to fetch tags: ' + msg });
    }
  });

  app.post('/api/git/tags', async (req, res) => {
    const { tagName, message } = req.body;
    
    if (!tagName) {
      return res.status(400).json({ error: 'Tag name is required' });
    }
    
    if (!/^[a-zA-Z0-9.\-_/]+$/.test(tagName)) {
      return res.status(400).json({ error: 'Invalid tag name format. Use letters, numbers, dots, hyphens, and underscores.' });
    }

    try {
      const stmtInsert = db.prepare("INSERT OR REPLACE INTO version_tags (tag_name, message) VALUES (?, ?)");
      stmtInsert.run(tagName, message || '');

      let gitSuccess = false;
      let gitError = '';
      try {
        try {
          await execFileAsync('git', ['rev-parse', 'HEAD']);
        } catch {
          await execFileAsync('git', ['add', 'metadata.json']);
          await execFileAsync('git', ['commit', '-m', 'chore: initial commit for versioning']);
        }

        try {
          await execFileAsync('git', ['show-ref', '--tags', 'refs/tags/' + tagName]);
          await execFileAsync('git', ['tag', '-d', tagName]);
        } catch {
          // tag doesn't exist
        }

        await execFileAsync('git', ['config', '--global', 'user.name', 'AI Coder Agent']);
        await execFileAsync('git', ['config', '--global', 'user.email', 'fraise-agent@ais.local']);

        if (message) {
          await execFileAsync('git', ['tag', '-a', tagName, '-m', message]);
        } else {
          await execFileAsync('git', ['tag', tagName]);
        }
        gitSuccess = true;
      } catch (e: unknown) {
        gitError = e instanceof Error ? e.message : String(e);
        console.warn('Real Git tag creation failed:', gitError);
      }

      res.json({ 
        success: true, 
        message: gitSuccess 
          ? `Successfully created Git tag "${tagName}"` 
          : `Saved version "${tagName}" to database fallback (Git unavailable: ${gitError})`,
        gitSuccess
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      res.status(500).json({ error: 'Failed to save version tag: ' + msg });
    }
  });

  interface GlbFileRecord {
    name: string;
    relPath: string;
    actualSize: number;
    modifiedAt: string;
    magicMatches: boolean;
    version: number;
    declaredLength: number;
    declaredSizeMatchesActual: boolean;
    isValidGLB: boolean;
    errorMessage: string;
    headerHex: string;
  }

  // GLB Scanning and Byte-level Validation Endpoint
  async function findGLBFilesRecursively(dir: string, baseDir: string): Promise<GlbFileRecord[]> {
    const glbs: GlbFileRecord[] = [];
    try {
      const entries = await fs.readdir(dir, { withFileTypes: true });
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          if (entry.name === 'node_modules' || entry.name === '.git' || entry.name === 'dist' || entry.name.startsWith('import-tmp')) {
            continue;
          }
          const subGlbs = await findGLBFilesRecursively(fullPath, baseDir);
          glbs.push(...subGlbs);
        } else if (entry.name.toLowerCase().endsWith('.glb')) {
          const stats = await fs.stat(fullPath);
          const relPath = path.relative(baseDir, fullPath).replace(/\\/g, '/');
          
          let magicMatches = false;
          let version = 0;
          let declaredLength = 0;
          let declaredSizeMatchesActual = false;
          let isValidGLB = false;
          let errorMessage = '';
          let headerHex = '';
          
          try {
            const fd = await fs.open(fullPath, 'r');
            const buffer = Buffer.alloc(12);
            const { bytesRead } = await fd.read(buffer, 0, 12, 0);
            await fd.close();
            
            if (bytesRead < 12) {
              errorMessage = `Truncated file: read only ${bytesRead} bytes instead of 12-byte header.`;
            } else {
              headerHex = buffer.toString('hex', 0, 12);
              const magicStr = buffer.toString('ascii', 0, 4);
              magicMatches = (magicStr === 'glTF');
              version = buffer.readUInt32LE(4);
              declaredLength = buffer.readUInt32LE(8);
              
              if (!magicMatches) {
                errorMessage = `Invalid magic header: Expected 'glTF', got '${magicStr.replace(/[^ -~]/g, '?')}' (Hex: ${buffer.toString('hex', 0, 4)})`;
              } else if (version !== 2) {
                errorMessage = `Unsupported GLB version: Expected 2, got ${version}`;
              } else if (declaredLength !== stats.size) {
                errorMessage = `Corrupted length: Declared size ${declaredLength} bytes but file size on disk is ${stats.size} bytes.`;
              } else {
                isValidGLB = true;
              }
              declaredSizeMatchesActual = (declaredLength === stats.size);
            }
          } catch (err: unknown) {
            errorMessage = `Failed to read file headers: ${err instanceof Error ? err.message : String(err)}`;
          }
          
          glbs.push({
            name: entry.name,
            relPath,
            actualSize: stats.size,
            modifiedAt: stats.mtime.toISOString(),
            magicMatches,
            version,
            declaredLength,
            declaredSizeMatchesActual,
            isValidGLB,
            errorMessage,
            headerHex
          });
        }
      }
    } catch (err) {
      console.error(`Error reading directory ${dir}:`, err);
    }
    return glbs;
  }

  app.get('/api/glb/list', async (req, res) => {
    try {
      const glbs = await findGLBFilesRecursively(rootDir, rootDir);
      res.json({ success: true, glbs });
    } catch (err: unknown) {
      res.status(500).json({ error: 'Failed to scan GLB files: ' + (err instanceof Error ? err.message : String(err)) });
    }
  });

  app.get('/api/glb/raw', async (req, res) => {
    try {
      const relPath = String(req.query.path || '');
      if (!relPath || relPath.includes('..')) {
        return res.status(403).json({ error: 'Access denied: Invalid path segment' });
      }
      
      const fullPath = path.join(rootDir, relPath);
      if (!fullPath.startsWith(rootDir)) {
        return res.status(403).json({ error: 'Access denied: Out of workspace scope' });
      }
      
      await fs.access(fullPath);
      res.setHeader('Content-Type', 'model/gltf-binary');
      res.sendFile(fullPath);
    } catch (err: unknown) {
      res.status(404).send('GLB file not found: ' + (err instanceof Error ? err.message : String(err)));
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

  initScheduler();

  const PORT = process.env.PORT || 3000;
  // NOTE: In this sandboxed environment, port 3000 is the ONLY externally accessible port.
  // Ensure the public URL used for callbacks or external access includes this port correctly.
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
