import cron from 'node-cron';
import db from './database.ts';

export function initScheduler() {
    console.log('Initializing task scheduler...');

    // A sample cron job that runs every minute
    // You can modify the cron expression and the task logic below
    cron.schedule('* * * * *', () => {
        console.log(`[Scheduler] Heartbeat task running at ${new Date().toISOString()}`);
        
        try {
            // Setup a simple scheduled cleanup or checking task for media_tasks
            const pendingTasks = db.prepare("SELECT COUNT(*) as count FROM media_tasks WHERE status = 'pending'").get() as { count: number };
            if (pendingTasks && pendingTasks.count > 0) {
                console.log(`[Scheduler] Found ${pendingTasks.count} pending media tasks.`);
            }

            // Process Agent Tasks
            const queuedTask = db.prepare(`
                SELECT id FROM agent_tasks 
                WHERE status = 'queued' 
                AND (agent_id IS NULL OR agent_id IN (SELECT id FROM agents WHERE status = 'idle'))
                ORDER BY priority DESC, created_at ASC 
                LIMIT 1
            `).get() as { id: number } | undefined;

            if (queuedTask) {
                console.log(`[Scheduler] Automatically starting agent task #${queuedTask.id}`);
                // Use a non-blocking trigger if possible, or just call a fetch to our own API
                // For now, since we are in the same process, we could call the logic directly,
                // but simpler is to just log it for now and let the user trigger it, 
                // OR we can implement a small fetch call to the run endpoint.
                
                const PORT = process.env.PORT || 3000;
                fetch(`http://localhost:${PORT}/api/agent-tasks/${queuedTask.id}/run`, { method: 'POST' })
                    .catch(e => console.error('[Scheduler] Failed to trigger agent task:', e.message));
            }

        } catch (err) {
            console.error('[Scheduler] Error checking media tasks:', err);
        }
    });

    console.log('Task scheduler initialized successfully.');
}
