/**
 * AdminAiTasks - Specialized view for AI Media Queue
 * Demonstrates async data fetching and internal state management.
 */
import CmsTable from './CmsTable.js';
import CmsButton from './CmsButton.js';

export default {
    name: 'AdminAiTasks',
    components: {
        CmsTable,
        CmsButton
    },
    props: {
        passkey: {
            type: String,
            required: true
        }
    },
    setup(props) {
        const { ref, onMounted } = Vue;
        const tasks = ref([]);
        const loading = ref(false);
        const isBusy = ref(false);

        const columns = [
            { key: 'id', label: 'Task ID' },
            { key: 'site', label: 'Site' },
            { key: 'type', label: 'Type' },
            { key: 'status', label: 'Status' },
            { key: 'updated_at', label: 'Updated' }
        ];

        const fetchTasks = async () => {
            loading.value = true;
            try {
                const res = await fetch('/admin/api/ai/tasks', {
                    headers: { 'X-Admin-Passkey': props.passkey }
                });
                if (res.ok) {
                    const data = await res.json();
                    tasks.value = data.tasks.reverse();
                }
            } catch (e) {
                console.error('Error fetching tasks', e);
            } finally {
                loading.value = false;
            }
        };

        const triggerHeartbeat = async () => {
            isBusy.value = true;
            try {
                await fetch('/admin/api/ai/heartbeat', {
                    headers: { 'X-Admin-Passkey': props.passkey }
                });
                await fetchTasks();
            } catch (e) {
                console.error('Heartbeat failed', e);
            } finally {
                isBusy.value = false;
            }
        };

        const addSampleTask = async () => {
            try {
                await fetch('/admin/api/ai/tasks/add', {
                    method: 'POST',
                    headers: { 
                        'X-Admin-Passkey': props.passkey,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        site: 'site1.com',
                        type: 'improve_text',
                        payload: { text: "Quality services for you.", context: "Landing Page" }
                    })
                });
                await fetchTasks();
            } catch (e) {
                console.error('Add task failed', e);
            }
        };

        onMounted(fetchTasks);

        return { tasks, loading, isBusy, columns, triggerHeartbeat, addSampleTask };
    },
    template: `
        <div class="mt-16">
            <div class="flex justify-between items-end mb-6">
                <div>
                    <h2 class="text-2xl font-serif italic mb-1">AI Media Queue</h2>
                    <p class="text-[10px] font-mono opacity-40 uppercase tracking-tighter">Automated tasks via Heartbeat</p>
                </div>
                <div class="flex gap-2">
                    <cms-button @click="triggerHeartbeat" :loading="isBusy">Run Heartbeat</cms-button>
                    <cms-button variant="outline" @click="addSampleTask">Add Sample Task</cms-button>
                </div>
            </div>

            <cms-table :items="tasks" :columns="columns" :loading="loading">
                <template #col-type="{ value }">
                    <span class="px-1.5 py-0.5 bg-[#2A2A2A] rounded text-[9px] uppercase font-bold">{{ value }}</span>
                </template>
                <template #col-status="{ value }">
                    <span :class="{
                        'text-yellow-400': value === 'pending',
                        'text-blue-400': value === 'running',
                        'text-green-400': value === 'completed',
                        'text-red-400': value === 'failed'
                    }" class="font-bold lowercase italic text-sm">{{ value }}</span>
                </template>
                <template #col-updated_at="{ value }">
                    <span class="opacity-30 italic">{{ value }}</span>
                </template>
            </cms-table>
        </div>
    `
};
