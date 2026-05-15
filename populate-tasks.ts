import db from './database.ts';

const tasks = [
    {
        site_id: 'babiblog.fr',
        target_path: 'content/babiblog.fr/img/chambre-bebe.jpg',
        prompt: 'A beautifully decorated cozy baby room with soft pastel colors, modern crib, and warm lighting'
    },
    {
        site_id: 'babiblog.fr',
        target_path: 'content/babiblog.fr/img/recette-saine.jpg',
        prompt: 'Healthy and colorful baby food puree in small glass jars, fresh ingredients on a wooden table setup'
    },
    {
        site_id: 'babiblog.fr',
        target_path: 'content/babiblog.fr/img/organisation.jpg',
        prompt: 'Neatly organized baby closet with folded tiny clothes and labeled storage bins, minimalist design'
    },
    {
         site_id: 'sambazen.net',
         target_path: 'content/sambazen.net/img/hero.jpg',
         prompt: 'A calm and serene Zen meditation space with bamboo, soft sunlight filtering through windows, and neutral tones'
    },
    {
         site_id: 'sambazen.net',
         target_path: 'content/sambazen.net/img/meditation.jpg',
         prompt: 'Silhouette of a person doing a peaceful yoga or meditation pose outdoors at sunrise, beautiful landscape'
    },
    {
         site_id: 'sambazen.net',
         target_path: 'content/sambazen.net/img/class.jpg',
         prompt: 'A bright airy yoga studio with wooden floors and people engaged in a calming mindfulness class'
    },
    {
         site_id: 'sambazen.net',
         target_path: 'content/sambazen.net/img/retreat.jpg',
         prompt: 'A lush nature retreat center for wellness and meditation, green forest surround a wooden cabin, peaceful atmosphere'
    },
    {
         site_id: 'site1.com',
         target_path: 'content/site1.com/img/site1-logo.png',
         prompt: 'A modern, abstract, and minimalist logo for a tech startup, black background, vibrant neon accents'
    },
    {
         site_id: 'site2.com',
         target_path: 'content/site2.com/assets/site2-banner.jpg',
         prompt: 'A wide panoramic banner of a futuristic glowing cityscape at night'
    }
];

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

const insertStmt = db.prepare(`
    INSERT INTO media_tasks (site_id, target_path, prompt) 
    VALUES (?, ?, ?)
`);

let count = 0;
for (const task of tasks) {
    insertStmt.run(task.site_id, task.target_path, task.prompt);
    count++;
}

console.log(`${count} tasks successfully inserted.`);
