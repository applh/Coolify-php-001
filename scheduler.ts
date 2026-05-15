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
        } catch (err) {
            console.error('[Scheduler] Error checking media tasks:', err);
        }
    });

    console.log('Task scheduler initialized successfully.');
}
