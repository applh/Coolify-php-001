<template>
  <div class="max-w-6xl mx-auto flex flex-col h-full gap-4">
    <div class="flex justify-between items-center mb-4">
      <div>
        <h2 class="text-3xl font-serif tracking-tight">
          AI Media Task Queue
        </h2>
        <p class="text-sm text-gray-400 mt-1">
          Get your free Gemini API key from <a
            href="https://aistudio.google.com/app/apikey"
            target="_blank"
            class="text-[#F27D26] hover:underline"
          >Google AI Studio</a> to process this queue.
        </p>
      </div>
      <div class="flex gap-4">
        <input 
          v-model="apiKey"
          type="password" 
          placeholder="Gemini API Key" 
          class="bg-[#181818] border border-[#2A2A2A] rounded px-3 py-1.5 focus:outline-none focus:border-[#F27D26] text-sm"
        >
        <button 
          :disabled="isProcessing || !apiKey" 
          class="bg-[#F27D26] text-black px-4 py-1.5 rounded font-semibold text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          @click="processQueue"
        >
          {{ isProcessing ? 'Processing Queue...' : 'Run Queue' }}
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-3 gap-4 mb-4">
      <div class="bg-[#181818] border border-[#2A2A2A] p-4 rounded text-center">
        <div class="text-2xl font-bold text-white">
          {{ pendingTasks.length }}
        </div>
        <div class="text-sm text-gray-400">
          Pending
        </div>
      </div>
      <div class="bg-[#181818] border border-[#2A2A2A] p-4 rounded text-center">
        <div class="text-2xl font-bold text-green-500">
          {{ completedCount }}
        </div>
        <div class="text-sm text-gray-400">
          Completed
        </div>
      </div>
      <div class="bg-[#181818] border border-[#2A2A2A] p-4 rounded text-center">
        <div class="text-2xl font-bold text-red-500">
          {{ failedCount }}
        </div>
        <div class="text-sm text-gray-400">
          Failed
        </div>
      </div>
    </div>

    <!-- Task List -->
    <div class="bg-[#181818] rounded border border-[#2A2A2A] overflow-hidden flex-grow flex flex-col">
      <div class="overflow-y-auto w-full">
        <table class="w-full text-left border-collapse text-sm">
          <thead>
            <tr class="border-b border-[#2A2A2A] bg-[#202020] text-gray-400">
              <th class="p-3 font-medium w-10 text-center">
                <input
                  type="checkbox"
                  class="cursor-pointer bg-[#181818] border-[#2A2A2A]"
                  :checked="selectAllPending"
                  @change="toggleSelectAll"
                >
              </th>
              <th class="p-3 font-medium">
                Image
              </th>
              <th class="p-3 font-medium">
                ID
              </th>
              <th class="p-3 font-medium">
                Site
              </th>
              <th class="p-3 font-medium">
                Target Path
              </th>
              <th class="p-3 font-medium w-1/3">
                Prompt
              </th>
              <th class="p-3 font-medium">
                Status
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="task in allTasks"
              :key="task.id"
              class="border-b border-[#2A2A2A] hover:bg-[#202020]/50 transition-colors"
            >
              <td class="p-3 text-center">
                <input
                  v-if="task.status === 'pending' || task.status === 'failed'"
                  v-model="selectedTaskIds"
                  :value="task.id"
                  type="checkbox"
                  class="cursor-pointer bg-[#181818] border-[#2A2A2A]"
                >
              </td>
              <td class="p-3">
                <img
                  v-if="task.status === 'completed'"
                  :src="'/api/media-files?path=' + task.target_path + '&t=' + Date.now()"
                  class="w-16 h-16 object-cover rounded bg-[#2A2A2A]"
                  alt="Generated Image"
                  loading="lazy"
                >
              </td>
              <td class="p-3 font-mono text-gray-500">
                #{{ task.id }}
              </td>
              <td class="p-3">
                {{ task.site_id }}
              </td>
              <td class="p-3 font-mono text-xs text-gray-400">
                {{ task.target_path }}
              </td>
              <td class="p-3 text-gray-300 italic">
                {{ task.prompt }}
              </td>
              <td class="p-3">
                <span 
                  class="px-2 py-1 rounded text-xs font-semibold capitalize"
                  :class="{
                    'bg-gray-800 text-gray-300': task.status === 'pending',
                    'bg-blue-900/50 text-blue-400': task.status === 'processing',
                    'bg-green-900/50 text-green-400': task.status === 'completed',
                    'bg-red-900/50 text-red-400': task.status === 'failed'
                  }"
                >
                  {{ task.status }}
                </span>
                <div
                  v-if="task.error_message"
                  class="text-xs text-red-400 mt-1 max-w-[200px] truncate"
                  :title="task.error_message"
                >
                  {{ task.error_message }}
                </div>
              </td>
            </tr>
            <tr v-if="allTasks.length === 0">
              <td
                colspan="7"
                class="p-6 text-center text-gray-500"
              >
                No tasks found.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { GoogleGenAI } from '@google/genai';

const apiKey = ref('');
const isProcessing = ref(false);
const allTasks = ref([]);
const selectedTaskIds = ref([]);

const toggleSelectAll = (e) => {
    if (e.target.checked) {
        selectedTaskIds.value = allTasks.value
            .filter(t => t.status === 'pending' || t.status === 'failed')
            .map(t => t.id);
    } else {
        selectedTaskIds.value = [];
    }
};

const selectAllPending = computed(() => {
    const processable = allTasks.value.filter(t => t.status === 'pending' || t.status === 'failed');
    return processable.length > 0 && selectedTaskIds.value.length === processable.length;
});

const pendingTasks = computed(() => allTasks.value.filter(t => t.status === 'pending'));
const completedCount = computed(() => allTasks.value.filter(t => t.status === 'completed').length);
const failedCount = computed(() => allTasks.value.filter(t => t.status === 'failed').length);

const fetchTasks = async () => {
    try {
        await fetch('/api/scan-media', { method: 'POST' });
        const res = await fetch('/api/media-tasks');
        if (!res.ok) throw new Error('Failed to fetch');
        allTasks.value = await res.json();
        // Select all pending initially
        selectedTaskIds.value = allTasks.value
            .filter(t => t.status === 'pending' || t.status === 'failed')
            .map(t => t.id);
    } catch (err) {
        console.error('Failed to load tasks', err);
    }
};

const updateTaskStatus = async (id, status, error_message = null) => {
    try {
        await fetch(`/api/media-tasks/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status, error_message })
        });
    } catch (error) {
        console.error(`Failed to update task ${id}`, error);
    }
};

const processQueue = async () => {
    const tasksToProcess = allTasks.value.filter(t => selectedTaskIds.value.includes(t.id) && (t.status === 'pending' || t.status === 'failed'));
    
    if (tasksToProcess.length === 0) {
        alert('Please select at least one pending or failed task to run.');
        return;
    }

    if (!apiKey.value) {
        alert('API Key required');
        return;
    }
    
    isProcessing.value = true;
    const ai = new GoogleGenAI({ apiKey: apiKey.value });

    // Iterate through current pending tasks
    // We update the table dynamically
    for (const task of tasksToProcess) {
        task.status = 'processing';
        task.error_message = null;

        try {
            // 1. Generate Image
            const response = await ai.models.generateContent({
                model: 'gemini-2.5-flash-image',
                contents: { parts: [{ text: task.prompt }] },
                config: {
                    imageConfig: {
                        aspectRatio: "1:1"
                    }
                }
            });
            
            // Extract base64
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

            // 2. Save Image Base64 via Express
            const saveRes = await fetch('/api/media/save-base64', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    imageBase64: base64Data,
                    targetPath: task.target_path 
                })
            });

            if (!saveRes.ok) {
                const errData = await saveRes.json();
                throw new Error(errData.error || 'Failed to save base64 via API');
            }

            // 3. Mark Completed
            await updateTaskStatus(task.id, 'completed');
            task.status = 'completed';

        } catch (err) {
            console.error('Task failed', task.id, err);
            await updateTaskStatus(task.id, 'failed', err.message);
            task.status = 'failed';
            task.error_message = err.message;
        }
    }
    
    isProcessing.value = false;
};

onMounted(() => {
    fetchTasks();
});
</script>
