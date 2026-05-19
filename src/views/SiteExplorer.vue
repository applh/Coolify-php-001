<script setup lang="ts">
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
  Globe,
  HardDrive
} from 'lucide-vue-next';
import BaseButton from '../components/BaseButton.vue';
import BaseInput from '../components/BaseInput.vue';
import BaseCard from '../components/BaseCard.vue';

interface FileSystemItem {
  name: string;
  path: string;
  isDirectory: boolean;
  size?: number;
  extension?: string;
  updatedAt?: string;
}

const currentPath = ref('');
const selectedRepo = ref('repo-php');
const repos = ref<string[]>([]);
const viewMode = ref('directory'); // 'directory' or 'file'
const items = ref<FileSystemItem[]>([]);
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

const getRepoIcon = (repo: string) => {
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

const getPathPrefix = (index: number) => {
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
  try {
    const res = await fetch('/api/repos');
    repos.value = await res.json();
  } catch (err) {
    console.error(err);
  }
};

const selectRepo = (repo: string) => {
  selectedRepo.value = repo;
  explore('');
};

const explore = async (pathStr: string) => {
  try {
    const res = await fetch(`/api/explorer?repo=${selectedRepo.value}&path=${encodeURIComponent(pathStr)}`);
    if (!res.ok) throw new Error('Failed to fetch');
    const data = await res.json();
    
    currentPath.value = pathStr;
    viewMode.value = data.type;
    
    if (data.type === 'directory') {
      items.value = data.items.sort((a: FileSystemItem, b: FileSystemItem) => {
        if (a.isDirectory === b.isDirectory) {
           return a.name.localeCompare(b.name);
        }
        return a.isDirectory ? -1 : 1;
      });
    } else {
      fileContent.value = data.content;
      currentFileName.value = pathStr.split('/').pop() || '';
    }
  } catch (err) {
    console.error(err);
  }
};

const handleItemClick = (item: FileSystemItem) => {
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

<template>
  <div class="max-w-7xl mx-auto p-8 flex flex-col h-[calc(100vh-160px)] gap-8">
    <!-- Header with Repo Switcher -->
    <div class="flex flex-col gap-8">
      <div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
        <div>
          <div class="flex items-center gap-3 mb-4">
            <HardDrive
              :size="14"
              class="text-white/20"
            />
            <span class="text-[10px] font-mono uppercase tracking-[0.3em] opacity-40">Filesystem Explorer</span>
          </div>
          <h2 class="text-5xl font-serif italic mb-2 tracking-tighter">
            Environment Assets
          </h2>
          <p class="text-[10px] font-mono opacity-30 uppercase tracking-[0.2em]">
            Navigating root cluster: <span class="text-[#FF3B30]">{{ selectedRepo }}</span>
          </p>
        </div>
        
        <div class="w-full md:w-80">
          <BaseInput 
            v-model="searchQuery" 
            placeholder="Global asset search..."
            :icon="Search"
            clearable
          />
        </div>
      </div>

      <!-- Repo Cards -->
      <div class="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-10 gap-3">
        <button 
          v-for="repo in repos" 
          :key="repo"
          :class="[
            'p-3 border rounded-xl transition-all flex flex-col items-center gap-2 group relative overflow-hidden',
            selectedRepo === repo 
              ? 'bg-white/5 border-[#FF3B30] shadow-lg shadow-red-900/10' 
              : 'bg-[#0F0F0F] border-white/5 hover:border-white/10 hover:bg-white/[0.02]'
          ]"
          @click="selectRepo(repo)"
        >
          <component 
            :is="getRepoIcon(repo)" 
            :size="18" 
            :class="selectedRepo === repo ? 'text-[#FF3B30]' : 'text-white/20 group-hover:text-white/40'"
          />
          <span 
            class="text-[9px] font-mono uppercase tracking-widest truncate w-full text-center"
            :class="selectedRepo === repo ? 'text-white font-black' : 'text-white/30'"
          >
            {{ repo.split('-')[1] || repo }}
          </span>
          <div
            v-if="selectedRepo === repo"
            class="absolute bottom-0 left-0 right-0 h-0.5 bg-[#FF3B30]"
          />
        </button>
      </div>
    </div>

    <!-- Breadcrumbs -->
    <div class="bg-[#0F0F0F] px-5 py-3 rounded-xl border border-white/5 flex items-center gap-3 overflow-x-auto no-scrollbar">
      <div 
        class="flex items-center gap-2 cursor-pointer text-white/40 hover:text-white transition-colors flex-shrink-0"
        @click="explore('')"
      >
        <component
          :is="getRepoIcon(selectedRepo)"
          :size="14"
          class="opacity-50"
        />
        <span class="text-[10px] font-mono uppercase tracking-widest">{{ selectedRepo }}</span>
      </div>
      <template
        v-for="(part, index) in pathParts"
        :key="index"
      >
        <ChevronRight
          :size="10"
          class="opacity-20 flex-shrink-0"
        />
        <span 
          class="text-[10px] font-mono uppercase tracking-widest cursor-pointer text-white/40 hover:text-white transition-colors flex-shrink-0"
          @click="explore(getPathPrefix(index))"
        >
          {{ part }}
        </span>
      </template>
    </div>

    <!-- Main Views -->
    <div class="flex-grow flex gap-4 overflow-hidden">
      <!-- Directory Content View -->
      <BaseCard
        v-if="viewMode === 'directory'"
        class="flex-grow !p-0 overflow-hidden flex flex-col"
      >
        <div class="bg-white/[0.03] border-b border-white/5 px-6 py-3 flex justify-between">
          <span class="text-[10px] uppercase tracking-[0.2em] font-black text-white/20">Name</span>
          <span
            v-if="filteredItems.length > 0"
            class="text-[9px] font-mono text-white/20 uppercase"
          >{{ filteredItems.length }} Objects found</span>
        </div>
        
        <div class="overflow-y-auto flex-grow custom-scrollbar">
          <ul class="flex flex-col">
            <li 
              v-if="currentPath" 
              class="px-6 py-4 border-b border-white/[0.03] hover:bg-white/[0.02] cursor-pointer flex items-center gap-4 group transition-colors"
              @click="explore(getParentPath())"
            >
              <div class="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center text-white/20 group-hover:text-white/40 transition-colors">
                <FolderUp :size="16" />
              </div>
              <span class="text-xs font-mono text-white/40 group-hover:text-white/60 transition-colors italic">Parent Directory</span>
            </li>
            
            <li 
              v-for="item in filteredItems" 
              :key="item.path"
              class="px-6 py-4 border-b border-white/[0.03] hover:bg-white/[0.02] cursor-pointer flex items-center gap-4 group transition-colors"
              @click="handleItemClick(item)"
            >
              <div 
                class="w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-300"
                :class="item.isDirectory ? 'bg-[#FF3B30]/5 text-[#FF3B30]' : 'bg-white/5 text-white/30 group-hover:bg-white/10 group-hover:text-white/60'"
              >
                <Folder
                  v-if="item.isDirectory"
                  :size="20"
                />
                <FileText
                  v-else
                  :size="20"
                />
              </div>
              
              <div class="flex-1 min-w-0">
                <span 
                  class="block truncate text-sm transition-colors"
                  :class="item.isDirectory ? 'font-serif italic text-white group-hover:text-[#FF3B30]' : 'font-mono text-white/70 group-hover:text-white'"
                >
                  {{ item.name }}
                </span>
                <div class="flex items-center gap-3 mt-0.5">
                  <span class="block text-[9px] font-mono opacity-20 uppercase tracking-tighter">
                    {{ item.isDirectory ? 'System Collection' : 'Source Manifest' }}
                  </span>
                  <span v-if="!item.isDirectory && item.size !== undefined" class="block text-[9px] font-mono opacity-20 uppercase tracking-tighter">
                    • {{ item.size < 1024 ? item.size + ' B' : (item.size < 1024 * 1024 ? (item.size / 1024).toFixed(1) + ' KB' : (item.size / (1024 * 1024)).toFixed(1) + ' MB') }}
                  </span>
                  <span v-if="item.updatedAt" class="block text-[9px] font-mono opacity-20 uppercase tracking-tighter">
                    • {{ new Date(item.updatedAt).toLocaleString() }}
                  </span>
                </div>
              </div>
              
              <ChevronRight
                v-if="item.isDirectory"
                :size="14"
                class="opacity-0 group-hover:opacity-30 transform group-hover:translate-x-1 transition-all"
              />
            </li>
            
            <!-- Empty Search -->
            <div
              v-if="filteredItems.length === 0"
              class="py-24 text-center"
            >
              <div class="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-6">
                <SearchX
                  :size="24"
                  class="text-white/10"
                />
              </div>
              <p class="text-sm font-serif italic text-white/20">
                No matching artifacts detected in the current scope.
              </p>
            </div>
          </ul>
        </div>
      </BaseCard>

      <!-- File Content View -->
      <BaseCard
        v-if="viewMode === 'file'"
        class="flex-grow !p-0 overflow-hidden flex flex-col border-[#FF3B30]/30 shadow-2xl shadow-red-900/5 animate-in fade-in slide-in-from-right-4 duration-500"
      >
        <div class="bg-white/[0.03] border-b border-white/5 px-6 py-4 flex justify-between items-center">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 bg-[#FF3B30] rounded-xl flex items-center justify-center text-white shadow-lg shadow-red-900/20">
              <FileText :size="20" />
            </div>
            <div>
              <h3 class="text-sm font-mono font-black text-white leading-none mb-1">
                {{ currentFileName }}
              </h3>
              <p class="text-[9px] font-mono text-white/20 tracking-tighter uppercase">
                {{ currentPath }}
              </p>
            </div>
          </div>
          <BaseButton
            variant="ghost"
            size="sm"
            @click="explore(getParentPath())"
          >
            Disconnect
          </BaseButton>
        </div>
        
        <div class="flex-grow overflow-hidden relative">
          <div class="absolute inset-0 bg-[#0A0A0A] overflow-auto custom-scrollbar">
            <pre 
              class="p-8 font-mono text-xs leading-relaxed text-white/70 select-text"
              style="tab-size: 2;"
            ><code>{{ fileContent }}</code></pre>
          </div>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.1);
}
</style>

