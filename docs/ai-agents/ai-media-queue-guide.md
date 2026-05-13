# AI Media Queue Guide

The AI Media Queue is the engine that connects missing visual assets in your PHP sites with the Gemini generation engine.

## Lifecycle of a Media Task

1.  **Detection**: When you "Scan Sites" or navigate to the AI Media view, the backend parses all `.php` files in your site directories.
2.  **Extraction**: It looks for standard `<img>` tags and extracts the `src` attribute.
3.  **Validation**: It checks if the file exists in the corresponding `img/` or `assets/` directory.
4.  **Creation**: If the file is missing, a new task is created in the SQLite database with an AI-generated prompt based on the file name.
5.  **Processing**: You trigger the generation using your Gemini API key.
6.  **Resolution**: Once generated and saved, the task is marked as `completed`, and the image becomes immediately available to the PHP site.

## 1. Database Schema (SQLite)

We will use the existing `cms.db` (managed by `better-sqlite3`) to store a queue of image generation tasks.

```sql
CREATE TABLE IF NOT EXISTS media_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    site_id TEXT NOT NULL,         -- e.g., 'site1.com'
    target_path TEXT NOT NULL,     -- e.g., 'public/img/hero.jpg'
    prompt TEXT NOT NULL,          -- What Gemini should generate
    status TEXT DEFAULT 'pending', -- 'pending', 'completed', 'failed'
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## 2. Adding Tasks (The AI Studio Workflow)

When the AI agent creates a new HTML/PHP template for you, instead of hallucinating an image file, it will:
1. Use an Express API endpoint (`POST /api/media-tasks`) to register the required image.
2. Put a placeholder URL in the HTML (e.g., `<img src="/img/hero.jpg" alt="Hero">`), knowing the image will eventually exist there.

### Backend API for Queuing
```typescript
app.post('/api/media-tasks', (req, res) => {
    const { site_id, target_path, prompt } = req.body;
    const stmt = db.prepare(`
        INSERT INTO media_tasks (site_id, target_path, prompt) 
        VALUES (?, ?, ?)
    `);
    const info = stmt.run(site_id, target_path, prompt);
    res.json({ success: true, taskId: info.lastInsertRowid });
});

app.get('/api/media-tasks', (req, res) => {
    // Fetch pending tasks
    const tasks = db.prepare("SELECT * FROM media_tasks WHERE status = 'pending'").all();
    res.json(tasks);
});

app.put('/api/media-tasks/:id', (req, res) => {
    // Update task status and error message
    const { status, error_message } = req.body;
    const stmt = db.prepare("UPDATE media_tasks SET status = ?, error_message = ? WHERE id = ?");
    stmt.run(status, error_message || null, req.params.id);
    res.json({ success: true });
});
```

## 3. Vue 3 Admin Panel (Task Runner)

You will have a dedicated "Media Queue" view in your Vue dashboard.

**Flow:**
1. The admin panel fetches all `pending` tasks from `GET /api/media-tasks`.
2. The user enters their **Gemini API Key**.
3. The user clicks **"Run Queue"**.
4. The Vue app loops through the tasks:
   - Takes the `prompt`.
   - Calls the `@google/genai` SDK using `gemini-2.5-flash-image` directly from the browser.
   - Gets the Base64 image data.
   - Pushes the Base64 data to `/api/media/save-base64` (defined in our previous plan) setting the save location to `target_path`.
   - Marks the task as `completed` via `PUT /api/media-tasks/:id`.
   - If an error occurs, marks it as `failed` with the error reason.

### Example Vue Task Runner Logic

```javascript
const processQueue = async () => {
    if (!apiKey.value) return alert('API Key required');
    
    isProcessing.value = true;
    const ai = new GoogleGenAI({ apiKey: apiKey.value });

    for (let task of pendingTasks.value) {
        task.status = 'processing';
        try {
            // 1. Generate Image
            const response = await ai.models.generateContent({
                model: 'gemini-2.5-flash-image',
                contents: { parts: [{ text: task.prompt }] }
            });
            
            const base64Data = extractBase64(response); // custom extractor

            // 2. Save Image Base64 via Express
            await fetch('/api/media/save-base64', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    imageBase64: base64Data,
                    targetPath: task.target_path 
                    // Make sure targetPath includes the filename (e.g. site1.com/img/hero.png)
                })
            });

            // 3. Mark Completed
            await updateTaskStatus(task.id, 'completed');
            task.status = 'completed';

        } catch (err) {
            await updateTaskStatus(task.id, 'failed', err.message);
            task.status = 'failed';
        }
    }
    isProcessing.value = false;
};
```

## Summary of the "Offline AI" Strategy

This architecture elegantly solves the "AI Studio can't write binary files" problem:
1. **Design Time**: The AI agent builds the database records defining the visual requirements.
2. **Execution Time**: The human administrator provides the API credentials to physically manifest the media files via the Vue dashboard into the PHP environment.
