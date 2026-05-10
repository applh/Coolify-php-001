<template>
  <div class="max-w-6xl mx-auto flex flex-col h-full gap-4">
    <div class="flex justify-between items-center mb-4">
      <h2 class="text-3xl font-serif tracking-tight">
        Repo PHP Explorer
      </h2>
    </div>

    <!-- Breadcrumbs -->
    <div class="bg-[#181818] p-3 rounded border border-[#2A2A2A] flex items-center gap-2 text-sm text-[#A0A0A0]">
      <span
        class="cursor-pointer hover:text-white"
        @click="explore('')"
      >repo-php</span>
      <template
        v-for="(part, index) in pathParts"
        :key="index"
      >
        <span>/</span>
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
        <ul class="flex flex-col">
          <li 
            v-if="currentPath" 
            class="p-3 border-b border-[#2A2A2A] hover:bg-[#202020] cursor-pointer flex items-center gap-3 text-sm"
            @click="explore(getParentPath())"
          >
            <FolderUp
              :size="16"
              class="text-white"
            />
            <span class="text-white opacity-80">..</span>
          </li>
          
          <li 
            v-for="item in items" 
            :key="item.path"
            class="p-3 border-b border-[#2A2A2A] hover:bg-[#202020] cursor-pointer flex items-center gap-3 text-sm"
            @click="handleItemClick(item)"
          >
            <Folder
              v-if="item.isDirectory"
              :size="16"
              class="text-[#F27D26]"
            />
            <FileText
              v-else
              :size="16"
              class="text-[#888]"
            />
            <span :class="{ 'font-semibold text-white': item.isDirectory, 'text-gray-300': !item.isDirectory }">
              {{ item.name }}
            </span>
          </li>
          
          <li
            v-if="items.length === 0"
            class="p-6 text-center text-gray-500 text-sm"
          >
            Folder is empty
          </li>
        </ul>
      </div>

      <!-- File View -->
      <div
        v-if="viewMode === 'file'"
        class="flex-grow bg-[#181818] rounded border border-[#2A2A2A] flex flex-col overflow-hidden"
      >
        <div class="flex items-center justify-between p-3 border-b border-[#2A2A2A] bg-[#202020]">
          <div class="flex items-center gap-2">
            <FileText
              :size="16"
              class="text-[#F27D26]"
            />
            <span class="text-sm font-semibold">{{ currentFileName }}</span>
          </div>
        </div>
        <div class="flex-grow relative">
          <textarea
            v-model="fileContent"
            class="w-full h-full p-4 bg-[#121212] font-mono text-sm resize-none outline-none focus:ring-1 focus:ring-[#F27D26] text-[#D4D4D4]"
            readonly
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { Folder, FolderUp, FileText } from 'lucide-vue-next';

const currentPath = ref('');
const viewMode = ref('directory'); // 'directory' or 'file'
const items = ref([]);
const fileContent = ref('');
const currentFileName = ref('');

const pathParts = computed(() => {
  return currentPath.value ? currentPath.value.split('/').filter(Boolean) : [];
});

const getPathPrefix = (index) => {
  return pathParts.value.slice(0, index + 1).join('/');
};

const getParentPath = () => {
  const parts = pathParts.value;
  if (parts.length <= 1) return '';
  return parts.slice(0, -1).join('/');
};

const explore = async (pathStr) => {
  try {
    const res = await fetch(`/api/explorer?path=${encodeURIComponent(pathStr)}`);
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
    alert('Error accessing path.\nCheck console for details.');
  }
};

const handleItemClick = (item) => {
  explore(item.path);
};

onMounted(() => {
  explore('');
});
</script>
