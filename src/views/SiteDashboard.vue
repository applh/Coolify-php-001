<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { 
  Plus, 
  Globe, 
  Download, 
  Upload, 
  Database, 
  Trash2, 
  ChevronRight,
  ExternalLink,
  Layers
} from 'lucide-vue-next';
import BaseCard from '../components/BaseCard.vue';
import BaseButton from '../components/BaseButton.vue';
import BaseModal from '../components/BaseModal.vue';

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
  try {
    const res = await fetch('/api/repos');
    repos.value = await res.json();
  } catch (err) {
    console.error('Failed to fetch repos', err);
  }
};

const fetchSites = async () => {
  try {
    const res = await fetch(`/api/sites?repo=${selectedRepo.value}`);
    sites.value = await res.json();
  } catch (err) {
    console.error('Failed to fetch sites', err);
  }
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
  const p = localStorage.getItem('admin_passkey') || '';
  window.open(`/api/sites/${site}/download?repo=${selectedRepo.value}&passkey=${p}`, '_blank');
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
      // Custom toast or notification could go here
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
  <div class="max-w-6xl mx-auto p-8">
    <!-- Hero Section -->
    <div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-16">
      <div class="flex-1">
        <div class="flex items-center gap-3 mb-4">
          <div class="w-2 h-2 bg-[#FF3B30] rounded-full animate-pulse" />
          <span class="text-[10px] font-mono uppercase tracking-[0.3em] opacity-40">Local Instance</span>
        </div>
        <h2 class="text-6xl font-serif italic mb-6 leading-none tracking-tighter">
          App Registry
        </h2>
        
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl px-4 py-2">
            <Layers
              :size="14"
              class="text-[#FF3B30]"
            />
            <select 
              v-model="selectedRepo" 
              class="bg-transparent text-white text-[11px] font-bold uppercase tracking-widest focus:outline-none cursor-pointer"
              @change="onRepoChange"
            >
              <option
                v-for="repo in repos"
                :key="repo"
                :value="repo"
                class="bg-[#0A0A0A]"
              >
                {{ repo.split('-')[1]?.toUpperCase() || repo.toUpperCase() }} ENVIRONMENT
              </option>
            </select>
          </div>
          <div class="text-xs text-white/30 font-mono italic">
            Connected to {{ sites.length }} active domains
          </div>
        </div>
      </div>
      
      <BaseButton
        variant="primary"
        @click="isAddingSite = true"
      >
        <template #icon>
          <Plus :size="18" />
        </template>
        Provision New Site
      </BaseButton>
    </div>

    <!-- Sites Grid -->
    <div
      v-if="sites.length > 0"
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
    >
      <BaseCard
        v-for="site in sites" 
        :key="site"
        :title="site"
        :subtitle="`/${selectedRepo}/content/${site}`"
        hoverable
        @click="openEditor(site)"
      >
        <template #icon>
          <Globe
            :size="48"
            class="text-white"
          />
        </template>

        <div class="mt-8 flex flex-col gap-6">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <span class="w-1.5 h-1.5 bg-green-500 rounded-full" />
              <span class="text-[9px] font-mono uppercase tracking-widest text-green-500/80 font-bold">Authenticated</span>
            </div>
            <BaseButton
              variant="ghost"
              size="xs"
              @click.stop="siteToDelete = site"
            >
              <template #icon>
                <Trash2 :size="12" />
              </template>
              Purge
            </BaseButton>
          </div>

          <div class="flex items-center gap-2">
            <BaseButton
              variant="outline"
              size="xs"
              @click.stop="downloadSite(site)"
            >
              <template #icon>
                <Download :size="12" />
              </template>
              Backup
            </BaseButton>
            <BaseButton
              variant="outline"
              size="xs"
              @click.stop="triggerUpload(site)"
            >
              <template #icon>
                <Upload :size="12" />
              </template>
              Sync
            </BaseButton>
            <a 
              v-if="selectedRepo === 'repo-php'"
              :href="`/repo-php/public/index.php?__site=${site}`" 
              target="_blank"
              class="p-2 border border-white/10 rounded-lg hover:border-white/30 hover:bg-white/5 transition-all text-white/40 hover:text-[#FF3B30]"
              title="Launch Public View"
              @click.stop
            >
              <ExternalLink :size="14" />
            </a>
          </div>

          <div class="pt-4 border-t border-white/[0.03] flex justify-between items-center group-hover:translate-x-1 transition-transform">
            <span class="text-[10px] font-mono text-white/30 uppercase tracking-widest">Manage Instance</span>
            <ChevronRight
              :size="16"
              class="text-[#FF3B30]"
            />
          </div>
        </div>
      </BaseCard>
    </div>
    
    <!-- Empty State -->
    <div
      v-else
      class="border border-dashed border-white/10 rounded-3xl py-32 text-center"
    >
      <div class="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-6">
        <Database
          :size="24"
          class="text-white/20"
        />
      </div>
      <p class="text-white/40 italic font-serif text-lg">
        No active deployments found in the current registry.
      </p>
      <BaseButton
        variant="outline"
        size="sm"
        class="mt-8"
        @click="isAddingSite = true"
      >
        Initialize First Stack
      </BaseButton>
    </div>

    <!-- Hidden Upload Input -->
    <input
      ref="uploadInput"
      type="file"
      accept=".zip"
      class="hidden"
      @change="onFileSelected"
    >

    <!-- Modals -->
    <BaseModal
      v-model="isAddingSite"
      title="Provision New Instance"
    >
      <div class="space-y-6">
        <div>
          <label class="block text-[10px] uppercase tracking-[0.2em] font-bold text-white/40 mb-3">Domain / Instance ID</label>
          <input 
            v-model="newSiteName"
            type="text" 
            placeholder="e.g. creative-studio.com"
            class="w-full bg-white/5 border border-white/10 rounded-xl px-5 py-4 text-white focus:outline-none focus:border-[#FF3B30] transition-all font-mono"
            @keyup.enter="createSite"
          >
          <p class="mt-3 text-[9px] font-mono text-white/20 uppercase leading-relaxed">
            Specify the folder name within the content registry. This will be used for routing and identity.
          </p>
        </div>
      </div>
      <template #footer>
        <BaseButton
          variant="ghost"
          @click="isAddingSite = false"
        >
          Discard
        </BaseButton>
        <BaseButton 
          variant="primary" 
          :loading="isSubmitting" 
          :disabled="!newSiteName"
          @click="createSite"
        >
          Begin Provisioning
        </BaseButton>
      </template>
    </BaseModal>

    <BaseModal
      v-model:model-value="siteToDelete"
      :title="`Purge ${siteToDelete}`"
    >
      <div class="mb-4">
        <div class="p-6 bg-red-500/5 border border-red-500/10 rounded-2xl mb-6">
          <p class="text-sm text-red-200/70 leading-relaxed italic font-serif">
            Warning: This operation will permanently remove all source files, metadata, and linked assets for <span class="text-white font-mono not-italic">{{ siteToDelete }}</span>.
          </p>
        </div>
        <p class="text-[10px] uppercase tracking-widest text-white/30 font-bold">
          Irreversible Backend Operation
        </p>
      </div>
      <template #footer>
        <BaseButton
          variant="ghost"
          @click="siteToDelete = null"
        >
          Abort
        </BaseButton>
        <BaseButton 
          variant="danger" 
          :loading="isDeleting"
          @click="deleteSite"
        >
          Confirm Purge
        </BaseButton>
      </template>
    </BaseModal>
  </div>
</template>
