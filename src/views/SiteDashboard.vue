<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { LayoutGrid, Plus, Globe, ArrowRight, Download, Upload } from 'lucide-vue-next';

const sites = ref<string[]>([]);
const repos = ref<string[]>([]);
const selectedRepo = ref('repo-php');
const isAddingSite = ref(false);
const siteToDelete = ref<string | null>(null);
const newSiteName = ref('');
const isSubmitting = ref(false);
const isDeleting = ref(false);
const router = useRouter();

const uploadInput = ref<HTMLInputElement | null>(null);
const siteToUpload = ref<string>('');

const fetchRepos = async () => {
  const res = await fetch('/api/repos');
  repos.value = await res.json();
};

const fetchSites = async () => {
  const res = await fetch(`/api/sites?repo=${selectedRepo.value}`);
  sites.value = await res.json();
};

onMounted(async () => {
  await fetchRepos();
  await fetchSites();
});

const onRepoChange = () => {
  fetchSites();
};

const openEditor = (site: string) => {
  router.push(`/editor/${site}?repo=${selectedRepo.value}`);
};

const downloadSite = (site: string) => {
  window.open(`/api/sites/${site}/download?repo=${selectedRepo.value}`, '_blank');
};

const triggerUpload = (site: string) => {
  siteToUpload.value = site;
  uploadInput.value?.click();
};

const onFileSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file || !siteToUpload.value) return;

  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await fetch(`/api/sites/${siteToUpload.value}/upload?repo=${selectedRepo.value}`, {
      method: 'POST',
      body: formData,
    });
    if (res.ok) {
      alert('Site updated successfully from zip!');
    } else {
      const e = await res.json();
      alert('Failed to upload site: ' + e.error);
    }
  } catch {
    alert('Upload error');
  }
  
  target.value = '';
};

const createSite = async () => {
  if (!newSiteName.value) return;
  isSubmitting.value = true;
  
  try {
    const res = await fetch('/api/sites', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        name: newSiteName.value,
        repo: selectedRepo.value
      }),
    });
    
    if (res.ok) {
      const data = await res.json();
      sites.value.push(data.name);
      isAddingSite.value = false;
      newSiteName.value = '';
      router.push(`/editor/${data.name}?repo=${selectedRepo.value}`);
    } else {
      const e = await res.json();
      alert('Error creating site: ' + e.error);
    }
  } catch {
    alert('Request failed');
  } finally {
    isSubmitting.value = false;
  }
};

const deleteSite = async () => {
  if (!siteToDelete.value) return;
  isDeleting.value = true;
  
  try {
    const res = await fetch(`/api/sites/${siteToDelete.value}?repo=${selectedRepo.value}`, {
      method: 'DELETE',
    });
    
    if (res.ok) {
      sites.value = sites.value.filter(s => s !== siteToDelete.value);
      siteToDelete.value = null;
    } else {
      const e = await res.json();
      alert('Error deleting site: ' + e.error);
    }
  } catch {
    alert('Request failed');
  } finally {
    isDeleting.value = false;
  }
};
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex justify-between items-end mb-12">
      <div>
        <h2 class="text-3xl font-serif italic mb-2">
          Workspace
        </h2>
        <div class="flex items-center gap-4">
          <select 
            v-model="selectedRepo" 
            @change="onRepoChange"
            class="bg-[#181818] border border-[#2A2A2A] text-[#F27D26] text-xs font-bold uppercase tracking-wider px-3 py-2 rounded focus:outline-none focus:border-[#F27D26]"
          >
            <option v-for="repo in repos" :key="repo" :value="repo">{{ repo }}</option>
          </select>
          <p class="text-sm opacity-50 font-mono">
            Manage your multi-stack application
          </p>
        </div>
      </div>
      <button 
        class="bg-[#F27D26] text-black px-4 py-2 text-xs font-bold uppercase tracking-wider rounded border border-[#F27D26] hover:bg-transparent hover:text-[#F27D26] transition-all flex items-center gap-2"
        @click="isAddingSite = true"
      >
        <Plus :size="16" /> Add New Site
      </button>
    </div>

    <!-- Add Site Modal -->
    <div v-if="isAddingSite" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div class="bg-[#121212] border border-[#2A2A2A] p-8 w-full max-w-md relative animate-in fade-in zoom-in duration-200">
        <h3 class="text-2xl font-serif italic mb-6">Create New Site</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-[10px] uppercase tracking-widest opacity-40 mb-2 font-mono">Site Domain / Folder Name</label>
            <input 
              v-model="newSiteName"
              type="text" 
              placeholder="e.g. mynewproject.com"
              class="w-full bg-[#181818] border border-[#2A2A2A] rounded px-4 py-3 text-white focus:outline-none focus:border-[#F27D26] transition-colors"
              @keyup.enter="createSite"
            >
          </div>
          <div class="flex gap-3 pt-4">
            <button 
              class="flex-1 px-4 py-3 text-xs font-bold uppercase tracking-wider opacity-40 hover:opacity-100 transition-all"
              @click="isAddingSite = false"
            >
              Cancel
            </button>
            <button 
              :disabled="!newSiteName || isSubmitting"
              class="flex-1 bg-[#F27D26] text-black px-4 py-3 text-xs font-bold uppercase tracking-wider rounded disabled:opacity-50 disabled:cursor-not-allowed"
              @click="createSite"
            >
              {{ isSubmitting ? 'Creating...' : 'Create Site' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="siteToDelete" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div class="bg-[#121212] border border-[#2A2A2A] p-8 w-full max-w-md relative animate-in fade-in zoom-in duration-200">
        <h3 class="text-2xl font-serif italic mb-4">Delete Site?</h3>
        <p class="text-sm opacity-50 mb-8 leading-relaxed">
          Are you sure you want to delete <span class="text-white font-mono">{{ siteToDelete }}</span>? This action is permanent and will remove all site files and media assets.
        </p>
        <div class="flex gap-3">
          <button 
            class="flex-1 px-4 py-3 text-xs font-bold uppercase tracking-wider opacity-40 hover:opacity-100 transition-all font-mono"
            @click="siteToDelete = null"
          >
            Cancel
          </button>
          <button 
            :disabled="isDeleting"
            class="flex-1 bg-red-600 text-white px-4 py-3 text-xs font-bold uppercase tracking-wider rounded hover:bg-red-700 transition-all disabled:opacity-50"
            @click="deleteSite"
          >
            {{ isDeleting ? 'Deleting...' : 'Delete Permanently' }}
          </button>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div 
        v-for="site in sites" 
        :key="site"
        class="group border border-[#2A2A2A] p-6 bg-[#181818] cursor-pointer hover:border-[#F27D26] transition-all relative overflow-hidden"
        @click="openEditor(site)"
      >
        <div class="flex justify-between items-start mb-8 relative z-10">
          <Globe
            :size="24"
            class="opacity-40 group-hover:text-[#F27D26] group-hover:opacity-100 transition-all"
          />
          <div class="flex items-center gap-2">
            <button 
              @click.stop="siteToDelete = site"
              class="text-[10px] uppercase tracking-widest opacity-0 group-hover:opacity-30 hover:opacity-100 font-mono text-red-500 transition-all"
            >
              Delete
            </button>
            <div class="text-[10px] uppercase tracking-widest opacity-30 font-mono">
              active
            </div>
          </div>
        </div>
        
        <h3 class="text-xl font-serif italic mb-1">
          {{ site }}
        </h3>
        <p class="text-[10px] font-mono opacity-40 uppercase tracking-tighter mb-4">
          /{{ selectedRepo }}/content/{{ site }}
        </p>
        
        <div class="flex items-center gap-2 mt-4 z-10 relative">
          <button @click.stop="downloadSite(site)" class="bg-[#2A2A2A] text-white px-3 py-1.5 text-xs font-bold uppercase tracking-wider rounded hover:bg-[#F27D26] hover:text-black transition-all flex items-center gap-1" title="Download ZIP">
            <Download :size="14" /> Download
          </button>
          <button @click.stop="triggerUpload(site)" class="bg-[#2A2A2A] text-white px-3 py-1.5 text-xs font-bold uppercase tracking-wider rounded hover:bg-[#F27D26] hover:text-black transition-all flex items-center gap-1" title="Upload ZIP to Overwrite">
            <Upload :size="14" /> Upload
          </button>
          <a 
            v-if="selectedRepo === 'repo-php'"
            :href="`/repo-php/public/index.php?__site=${site}`" 
            target="_blank"
            @click.stop
            class="bg-[#2A2A2A] text-white px-3 py-1.5 text-xs font-bold uppercase tracking-wider rounded hover:bg-[#F27D26] hover:text-black transition-all flex items-center gap-1"
          >
            <Globe :size="14" /> View
          </a>
          <div class="flex-1"></div>
          <div class="flex items-center gap-1 text-[#F27D26] text-xs font-bold uppercase tracking-wider opacity-0 group-hover:opacity-100 transform translate-x-[-10px] group-hover:translate-x-0 transition-all">
            Manage <ArrowRight :size="14" />
          </div>
        </div>

        <div class="absolute right-[-20px] bottom-[-20px] opacity-[0.03] scale-[4] group-hover:opacity-[0.07] transition-all pointer-events-none">
          <LayoutGrid :size="32" />
        </div>
      </div>
    </div>
    
    <input type="file" accept=".zip" ref="uploadInput" class="hidden" @change="onFileSelected" />
    
    <div
      v-if="sites.length === 0"
      class="border border-dashed border-[#2A2A2A] py-20 text-center opacity-30"
    >
      <p class="text-sm italic">
        No sites found in content directory.
      </p>
    </div>
  </div>
</template>
