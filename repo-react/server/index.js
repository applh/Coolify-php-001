import express from 'express';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = process.env.PORT || 3000;

// Helper to get plugins
function getPlugins() {
    const pluginPath = path.join(__dirname, '../plugins');
    if (!fs.existsSync(pluginPath)) return [];
    return fs.readdirSync(pluginPath).filter(f => {
        return f !== '.' && f !== '..' && fs.statSync(path.join(pluginPath, f)).isDirectory();
    });
}

// Helper to get sites from content
function getSitesFromContent(contentPath) {
    const sites = [];
    if (fs.existsSync(contentPath)) {
        const items = fs.readdirSync(contentPath);
        for (const item of items) {
            if (['.', '..', 'my-data'].includes(item)) continue;
            if (fs.statSync(path.join(contentPath, item)).isDirectory()) {
                sites.push(item);
            }
        }
    }
    return sites;
}

// CMS Diagnostic Endpoint (Express middleware for overriding)
app.use((req, res, next) => {
    if (req.query.cms_debug === 'true') {
        const contentPath = path.join(__dirname, '../content');
        let isWritable = false;
        try {
            if (fs.existsSync(contentPath)) {
                fs.accessSync(contentPath, fs.constants.W_OK);
                isWritable = true;
            }
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        } catch (_e) {
            // ignore
        }

        return res.json({
            content_path: contentPath,
            is_writable: isWritable,
            sites: getSitesFromContent(contentPath),
            available_plugins: getPlugins(),
            server_software: 'express',
            node_version: process.version
        });
    }
    next();
});

// Load sites json map
function loadSites() {
    try {
        const data = fs.readFileSync(path.join(__dirname, '../src/sites.json'), 'utf8');
        return JSON.parse(data);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (_e) {
        return [];
    }
}

// Passkey verification middleware
const verifyAdmin = (req, res, next) => {
    const expectedPasskey = process.env.APP_ADMIN_PASSKEY || process.env.app_admin_passkey;
    if (!expectedPasskey) return res.status(403).json({ error: 'No passkey configured' });
    
    const passkey = req.headers['x-admin-passkey'] || req.cookies?.admin_passkey;
    if (passkey !== expectedPasskey) {
        return res.status(403).json({ error: 'Unauthorized' });
    }
    next();
};

// Mount shared PHP admin JS components so Vue UI works out-of-the-box
app.use('/js', express.static(path.join(__dirname, '../../repo-php/public/js')));

app.get('/admin/api/sites', verifyAdmin, (req, res) => {
    const sites = loadSites().map(s => s.domain);
    res.json({ status: 'success', sites });
});

app.get('/admin/api/forms', verifyAdmin, (req, res) => {
    res.json({ status: 'success', forms: [] });
});

app.post('/admin/api/forms/save', verifyAdmin, (req, res) => {
    res.json({ status: 'success' });
});

app.post('/admin/api/forms/delete', verifyAdmin, (req, res) => {
    res.json({ status: 'success' });
});

app.get('/admin/api/forms/submissions', verifyAdmin, (req, res) => {
    res.json({ status: 'success', submissions: [] });
});

app.get('/admin', (req, res) => {
    res.sendFile(path.join(__dirname, '../admin.html'));
});

// Serve Vite build outputs securely, routing __site/domains if needed
app.use(express.static(path.join(__dirname, '../dist')));

app.get('/api/sites', (req, res) => {
    res.json(loadSites());
});

app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, '../dist/index.html'));
});

app.listen(port, () => {
    console.log(`Server running on port ${port}`);
});
