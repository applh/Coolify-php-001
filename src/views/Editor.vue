<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { FileText, Save, ChevronLeft, Loader2 } from 'lucide-vue-next';

const route = useRoute();
const site = route.params.site as string;
const files = ref<string[]>([]);
const selectedFile = ref<string | null>(null);
const content = ref('');
const saving = ref(false);

const fetchFiles = async () => {
  const res = await fetch(`/api/sites/${site}/files`);
  files.value = await res.json();
};

const loadFile = async (file: string) => {
  selectedFile.value = file;
  const res = await fetch(`/api/sites/${site}/files/${file}`);
  const data = await res.json();
  content.value = data.content;
};

const saveFile = async () => {
  if (!selectedFile.value) return;
  saving.value = true;
  try {
    await fetch(`/api/sites/${site}/files/${selectedFile.value}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: content.value }),
    });
  } catch (err) {
    console.error('Save failed', err);
  } finally {
    saving.value = false;
  }
};

onMounted(fetchFiles);
</script>

<template>
  <div class="h-full flex flex-col gap-6">
    <div class="flex justify-between items-center">
      <div class="flex items-center gap-4">
        <router-link to="/" class="p-2 hover:bg-[#2A2A2A] rounded transition-colors">
          <ChevronLeft :size="20" />
        </router-link>
        <div>
          <h2 class="text-2xl font-serif italic">{{ site }}</h2>
          <p class="text-[10px] font-mono opacity-40 uppercase">Editor Mode</p>
        </div>
      </div>
      
      <button 
        @click="saveFile"
        :disabled="!selectedFile || saving"
        class="bg-[#F27D26] text-black px-6 py-2 text-xs font-bold uppercase tracking-wider rounded disabled:opacity-30 disabled:grayscale flex items-center gap-2 hover:brightness-110 transition-all font-sans"
      >
        <Save v-if="!saving" :size="16" />
        <Loader2 v-else :size="16" class="animate-spin" />
        {{ saving ? 'Saving...' : 'Save Changes' }}
      </button>
    </div>

    <div class="flex-grow grid grid-cols-12 gap-6 min-h-[500px]">
      <!-- File List -->
      <div class="col-span-3 border border-[#2A2A2A] bg-[#181818] p-2 flex flex-col gap-1 overflow-y-auto rounded shadow-inner">
        <div class="px-3 py-2 text-[10px] uppercase tracking-widest opacity-30 font-bold border-b border-[#2A2A2A] mb-2">Files</div>
        <button 
          v-for="file in files" 
          :key="file"
          @click="loadFile(file)"
          :class="[
            'flex items-center gap-3 px-3 py-2 text-sm rounded transition-all text-left group',
            selectedFile === file ? 'bg-[#2A2A2A] text-[#F27D26]' : 'hover:bg-[#202020] opacity-70'
          ]"
        >
          <FileText :size="14" class="opacity-50 group-hover:opacity-100" />
          <span class="truncate">{{ file }}</span>
        </button>
      </div>

      <!-- Editor -->
      <div class="col-span-9 border border-[#2A2A2A] bg-[#0A0A0A] relative flex flex-col rounded shadow-2xl">
        <div v-if="!selectedFile" class="absolute inset-0 flex items-center justify-center opacity-20 italic">
          Select a file to edit
        </div>
        <textarea 
          v-else
          v-model="content"
          class="w-full h-full p-8 bg-transparent text-sm font-mono focus:outline-none resize-none leading-relaxed text-[#D0D0D0]"
          spellcheck="false"
        ></textarea>
      </div>
    </div>
  </div>
</template>
