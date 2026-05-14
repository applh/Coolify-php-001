<template>
  <div class="max-w-6xl mx-auto flex flex-col h-full gap-4">
    <!-- Header with Repo Switcher -->
    <div class="flex flex-col gap-6">
      <div class="flex justify-between items-end">
        <div>
          <h2 class="text-3xl font-serif tracking-tight mb-1">
            Repository Explorer
          </h2>
          <p class="text-xs font-mono opacity-40 uppercase tracking-widest">
            Selected: <span class="text-[#F27D26]">{{ selectedRepo }}</span>
          </p>
        </div>
        
        <div class="flex items-center gap-3">
          <div class="relative">
            <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 opacity-30" />
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="Search files..."
              class="bg-[#181818] border border-[#2A2A2A] rounded px-10 py-2 text-xs focus:outline-none focus:border-[#F27D26] w-48 transition-all focus:w-64"
            >
          </div>
        </div>
      </div>

      <!-- Repo Cards -->
      <div class="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-2">
        <div 
          v-for="repo in repos" 
          :key="repo"
          @click="selectRepo(repo)"
          :class="[
            'cursor-pointer p-3 border rounded transition-all flex flex-col items-center gap-2 group',
            selectedRepo === repo 
              ? 'bg-[#1F1F1F] border-[#F27D26] shadow-[0_0_15px_rgba(242,125,38,0.1)]' 
              : 'bg-[#181818] border-[#2A2A2A] hover:border-white/20'
          ]"
        >
          <component 
            :is="getRepoIcon(repo)" 
            :size="20" 
            :class="selectedRepo === repo ? 'text-[#F27D26]' : 'text-white/30 group-hover:text-white/60'"
          />
          <span 
            class="text-[10px] font-mono tracking-tighter truncate w-full text-center"
            :class="selectedRepo === repo ? 'text-white font-bold' : 'text-white/40'"
          >
            {{ repo.replace('repo-', '') }}
          </span>
        </div>
      </div>
    </div>

    <!-- Breadcrumbs -->
    <div class="bg-[#181818] p-3 rounded border border-[#2A2A2A] flex items-center gap-2 text-sm text-[#A0A0A0]">
      <div 
        class="flex items-center gap-1 cursor-pointer hover:text-white"
        @click="explore('')"
      >
        <component :is="getRepoIcon(selectedRepo)" :size="14" class="opacity-50" />
        <span>{{ selectedRepo }}</span>
      </div>
      <template
        v-for="(part, index) in pathParts"
        :key="index"
      >
        <ChevronRight :size="14" class="opacity-30" />
        <span 
          class="cursor-pointer hover:text-white"
          @click="explore(getPathPrefix(index))"
        >
          {{ part }}
        </span>
      </template>
    </div>

    <!-- Main Views -->
    <div class="flex-grow flex gap-4 min-h-[500px]">
      <!-- List View -->
      <div
        v-if="viewMode === 'directory'"
        class="flex-grow bg-[#181818] rounded border border-[#2A2A2A] overflow-hidden flex flex-col"
      >
        <div class="bg-[#202020] border-b border-[#2A2A2A] p-2 flex justify-between text-[10px] uppercase tracking-widest opacity-40 font-mono">
          <span>Name</span>
          <span v-if="filteredItems.length > 0">{{ filteredItems.length }} items</span>
        </div>
        <ul class="flex flex-col">
          <li 
            v-if="currentPath" 
            class="p-3 border-b border-[#2A2A2A] hover:bg-[#202020] cursor-pointer flex items-center gap-3 text-sm"
            @click="explore(getParentPath())"
          >
            <FolderUp
              :size="16"
              class="text-white/50"
            />
            <span class="text-white/50 font-mono">..</span>
          </li>
          
          <li 
            v-for="item in filteredItems" 
            :key="item.path"
            class="p-3 border-b border-[#2A2A2A] hover:bg-[#202020] cursor-pointer flex items-center gap-3 text-sm group"
            @click="handleItemClick(item)"
          >
            <Folder
              v-if="item.isDirectory"
              :size="16"
              class="text-[#F27D26] opacity-70 group-hover:opacity-100"
            />
            <FileText
              v-else
              :size="16"
              class="text-[#888] group-hover:text-[#AAA]"
            />
            <span 
              class="truncate flex-1"
              :class="{ 'font-semibold text-white/90': item.isDirectory, 'text-gray-400': !item.isDirectory }"
            >
              {{ item.name }}
            </span>
            <ChevronRight v-if="item.isDirectory" :size="14" class="opacity-0 group-hover:opacity-30" />
          </li>
          
          <li
            v-if="filteredItems.length === 0"
            class="p-12 text-center text-gray-500 text-sm flex flex-col items-center gap-4"
          >
            <SearchX :size="32" class="opacity-20" />
            <span>No matching files found</span>
          </li>
        </ul>
      </div>

      <!-- File View -->
      <div
        v-if="viewMode === 'file'"
        class="flex-grow bg-[#181818] rounded border border-[#2A2A2A] flex flex-col overflow-hidden animate-in fade-in slide-in-from-right-4 duration-300"
      >
        <div class="flex items-center justify-between p-3 border-b border-[#2A2A2A] bg-[#202020]">
          <div class="flex items-center gap-3">
            <FileText
              :size="16"
              class="text-[#F27D26]"
            />
            <div class="flex flex-col">
              <span class="text-xs font-bold font-mono text-white/90">{{ currentFileName }}</span>
              <span class="text-[9px] font-mono opacity-30">{{ currentPath }}</span>
            </div>
          </div>
          <button 
            @click="explore(getParentPath())"
            class="text-[10px] uppercase tracking-widest bg-[#2A2A2A] px-2 py-1 rounded hover:bg-[#333] transition-all"
          >
            Close
          </button>
        </div>
        <div class="flex-grow relative">
          <textarea
            v-model="fileContent"
            class="w-full h-full p-6 bg-[#121212] font-mono text-sm resize-none outline-none text-[#D4D4D4] leading-relaxed"
            readonly
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { 
  Folder, 
  FolderUp, 
  FileText, 
  ChevronRight, 
  Search, 
  SearchX,
  Code,
  Terminal,
  Layers,
  Layout,
  Cpu,
  Settings,
  Smartphone,
  Zap,
  Box,
  Library,
  FileCode,
  Globe
} from 'lucide-vue-next';

const currentPath = ref('');
const selectedRepo = ref('repo-php');
const repos = ref([]);
const viewMode = ref('directory'); // 'directory' or 'file'
const items = ref([]);
const fileContent = ref('');
const currentFileName = ref('');
const searchQuery = ref('');

const pathParts = computed(() => {
  return currentPath.value ? currentPath.value.split('/').filter(Boolean) : [];
});

const filteredItems = computed(() => {
  if (!searchQuery.value) return items.value;
  const q = searchQuery.value.toLowerCase();
  return items.value.filter(item => item.name.toLowerCase().includes(q));
});

const getRepoIcon = (repo) => {
  if (repo.includes('php')) return Code;
  if (repo.includes('python')) return Terminal;
  if (repo.includes('react')) return Layers;
  if (repo.includes('vue')) return Layout;
  if (repo.includes('go')) return Cpu;
  if (repo.includes('rust')) return Settings;
  if (repo.includes('android')) return Smartphone;
  if (repo.includes('flutter')) return Zap;
  if (repo.includes('docs')) return Library;
  if (repo.includes('src')) return FileCode;
  if (repo.includes('public')) return Globe;
  return Box;
};

const getPathPrefix = (index) => {
  return pathParts.value.slice(0, index + 1).join('/');
};

const getParentPath = () => {
  if (viewMode.value === 'file') {
     const parts = currentPath.value.split('/');
     parts.pop();
     return parts.join('/');
  }
  const parts = pathParts.value;
  if (parts.length === 0) return '';
  return parts.slice(0, -1).join('/');
};

const fetchRepos = async () => {
  const res = await fetch('/api/repos');
  repos.value = await res.json();
};

const selectRepo = (repo) => {
  selectedRepo.value = repo;
  explore('');
};

const explore = async (pathStr) => {
  try {
    const res = await fetch(`/api/explorer?repo=${selectedRepo.value}&path=${encodeURIComponent(pathStr)}`);
    if (!res.ok) throw new Error('Failed to fetch');
    const data = await res.json();
    
    currentPath.value = pathStr;
    viewMode.value = data.type;
    
    if (data.type === 'directory') {
      items.value = data.items.sort((a, b) => {
        if (a.isDirectory === b.isDirectory) {
           return a.name.localeCompare(b.name);
        }
        return a.isDirectory ? -1 : 1;
      });
    } else {
      fileContent.value = data.content;
      currentFileName.value = pathStr.split('/').pop();
    }
  } catch (err) {
    console.error(err);
  }
};

const handleItemClick = (item) => {
  explore(item.path);
};

onMounted(async () => {
  await fetchRepos();
  explore('');
});

watch(selectedRepo, () => {
  searchQuery.value = '';
});
</script>
