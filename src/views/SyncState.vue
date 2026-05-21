<script setup lang="ts">
import { ref, onMounted } from 'vue';

const importing = ref(false);
const importMessage = ref('');
const fileInput = ref<HTMLInputElement | null>(null);

const exportData = () => {
  const p = localStorage.getItem('admin_passkey') || '';
  window.open(`/api/sync/export?passkey=${p}`, '_blank');
};

const triggerImport = () => {
  fileInput.value?.click();
};

const handleImport = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  importing.value = true;
  importMessage.value = 'Uploading and importing...';

  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await fetch('/api/sync/import', {
      method: 'POST',
      body: formData,
    });
    const result = await response.json();
    if (result.success) {
      importMessage.value = result.message;
    } else {
      importMessage.value = 'Import failed: ' + result.error;
    }
  } catch (err: unknown) {
    importMessage.value = 'Error: ' + (err instanceof Error ? err.message : String(err));
  } finally {
    importing.value = false;
  }
};

const remoteUrl = ref(localStorage.getItem('sync_remote_url') || '');
const secretKey = ref(localStorage.getItem('sync_secret_key') || '');
const pushing = ref(false);
const pushMessage = ref('');

const pushToRemote = async () => {
  if (!remoteUrl.value || !secretKey.value) {
    pushMessage.value = 'Remote URL and Secret Key are required.';
    return;
  }

  localStorage.setItem('sync_remote_url', remoteUrl.value);
  localStorage.setItem('sync_secret_key', secretKey.value);

  pushing.value = true;
  pushMessage.value = 'Bundling and pushing to remote...';

  try {
    const response = await fetch('/api/sync/push', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        remoteUrl: remoteUrl.value,
        secretKey: secretKey.value,
      }),
    });
    
    const result = await response.json();
    if (response.ok) {
        pushMessage.value = 'Push successful: ' + (result.message || 'State updated.');
    } else {
        pushMessage.value = 'Push failed: ' + (result.error || 'Unknown error');
    }
  } catch (err: unknown) {
    pushMessage.value = 'Error: ' + (err instanceof Error ? err.message : String(err));
  } finally {
    pushing.value = false;
  }
};

interface GitTag {
  tag_name: string;
  created_at: string;
  message: string;
  is_git: boolean;
}

const tags = ref<GitTag[]>([]);
const loadingTags = ref(false);
const newTagName = ref('');
const newTagMessage = ref('');
const tagging = ref(false);
const tagStatusMessage = ref('');
const fetchTagsError = ref('');

const fetchTags = async () => {
  loadingTags.value = true;
  fetchTagsError.value = '';
  try {
    const res = await fetch('/api/git/tags');
    const data = await res.json();
    if (data.success) {
      tags.value = data.tags;
    } else {
      fetchTagsError.value = data.error || 'Failed to fetch version tags.';
    }
  } catch (err) {
    fetchTagsError.value = 'Failed to load version tags.';
    console.error(err);
  } finally {
    loadingTags.value = false;
  }
};

const createTag = async () => {
  if (!newTagName.value.trim()) {
    tagStatusMessage.value = 'Tag name is required.';
    return;
  }

  tagging.value = true;
  tagStatusMessage.value = 'Publishing code version...';
  
  try {
    const res = await fetch('/api/git/tags', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        tagName: newTagName.value.trim(),
        message: newTagMessage.value.trim()
      })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      tagStatusMessage.value = data.message;
      newTagName.value = '';
      newTagMessage.value = '';
      await fetchTags();
    } else {
      tagStatusMessage.value = 'Error: ' + (data.error || 'Unknown error');
    }
  } catch (err: unknown) {
    tagStatusMessage.value = 'Error: ' + (err instanceof Error ? err.message : String(err));
  } finally {
    tagging.value = false;
  }
};

onMounted(() => {
  fetchTags();
});
</script>

<template>
  <div class="max-w-4xl mx-auto py-10 px-6">
    <div class="mb-12 text-center">
      <h2 class="text-4xl font-serif italic mb-4">
        Platform Synchronization
      </h2>
      <p class="text-[#888] max-w-2xl mx-auto leading-relaxed">
        Keep your AI Studio preview and production VPS in total sync. 
        Export legacy ZIPs for manual backup, or use **Remote Push** for instant updates.
      </p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">
      <div class="bg-[#181818] border border-[#2A2A2A] p-8 rounded-lg flex flex-col justify-between">
        <div>
          <h3 class="text-xl mb-4 font-semibold text-[#F27D26]">
            Manual Export
          </h3>
          <p class="text-sm text-[#888] mb-6">
            Download a full snapshot of your database and site files.
          </p>
        </div>
        <button 
          class="bg-[#F27D26] text-black px-6 py-3 rounded font-bold hover:bg-[#ff8e3c] transition-colors w-full"
          @click="exportData"
        >
          Download Backup ZIP
        </button>
      </div>

      <div class="bg-[#181818] border border-[#2A2A2A] p-8 rounded-lg">
        <h3 class="text-xl mb-4 font-semibold text-[#F27D26]">
          Manual Import
        </h3>
        <p class="text-sm text-[#888] mb-6">
          Upload a backup archive. <span class="text-red-500 underline">Overwrites everything!</span>
        </p>
        <div class="flex flex-col gap-4">
          <input 
            ref="fileInput" 
            type="file" 
            class="hidden" 
            accept=".zip"
            @change="handleImport"
          >
          <button 
            :disabled="importing"
            class="border border-[#F27D26]/30 text-[#F27D26] px-6 py-3 rounded font-bold hover:border-[#F27D26] transition-all disabled:opacity-50 w-full"
            @click="triggerImport"
          >
            {{ importing ? 'Importing...' : 'Upload Sync ZIP' }}
          </button>
          
          <div
            v-if="importMessage"
            class="mt-4 p-4 rounded bg-[#222] border border-[#333] text-xs italic"
          >
            {{ importMessage }}
          </div>
        </div>
      </div>
    </div>

    <!-- Remote Push Section -->
    <div class="bg-[#181818] border-2 border-[#F27D26]/20 p-10 rounded-2xl">
      <div class="flex items-center gap-4 mb-8">
        <div class="p-3 bg-[#F27D26]/10 rounded-full">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-8 w-8 text-[#F27D26]"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"
            />
          </svg>
        </div>
        <div>
          <h3 class="text-2xl font-semibold">
            Remote Push (Source of Truth)
          </h3>
          <p class="text-sm text-[#777]">
            Instantly sync this environment to your production VPS.
          </p>
        </div>
      </div>

      <div class="space-y-6">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <label class="text-xs uppercase tracking-widest text-[#555] font-bold">Production URL</label>
            <input 
              v-model="remoteUrl"
              type="text" 
              placeholder="https://your-vps.com/api/sync/import"
              class="w-full bg-black border border-[#333] p-3 rounded text-sm focus:border-[#F27D26] outline-none transition-colors"
            >
          </div>
          <div class="space-y-2">
            <label class="text-xs uppercase tracking-widest text-[#555] font-bold">Sync Secret Key</label>
            <input 
              v-model="secretKey"
              type="password" 
              placeholder="Enter SYNC_SECRET_KEY"
              class="w-full bg-black border border-[#333] p-3 rounded text-sm focus:border-[#F27D26] outline-none transition-colors"
            >
          </div>
        </div>

        <button 
          :disabled="pushing"
          class="w-full bg-white text-black py-4 rounded-xl font-bold hover:bg-[#F27D26] hover:text-white transition-all transform hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:scale-100 uppercase tracking-widest text-sm"
          @click="pushToRemote"
        >
          {{ pushing ? 'Synchronizing Platforms...' : 'Push to Production' }}
        </button>

        <div
          v-if="pushMessage"
          class="p-4 rounded-lg bg-black border border-[#222] text-center text-sm font-mono"
          :class="pushMessage.includes('failed') || pushMessage.includes('Error') ? 'text-red-400' : 'text-[#F27D26]'"
        >
          {{ pushMessage }}
        </div>
      </div>
    </div>

    <!-- Git Version Tagging Section -->
    <div id="git-tag-section" class="mt-12 bg-[#181818] border border-[#2A2A2A] p-10 rounded-2xl">
      <div class="flex items-center gap-4 mb-8">
        <div class="p-3 bg-red-500/10 rounded-full">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-8 w-8 text-[#FF3B30]"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M7 7h.01M6 20a1 1 0 01-1-1v-4.586a1 1 0 01.293-.707l7-7a1 1 0 011.414 0l4.586 4.586a1 1 0 010 1.414l-7 7a1 1 0 01-.707.293H6z"
            />
          </svg>
        </div>
        <div>
          <h3 class="text-2xl font-semibold text-white">
            Git Version Tagging
          </h3>
          <p class="text-sm text-[#777]">
            Create annotated version tags in your local repository with automated database synchrony.
          </p>
        </div>
      </div>

      <!-- Tag Creation Form -->
      <form @submit.prevent="createTag" class="space-y-6">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <label class="text-xs uppercase tracking-widest text-[#555] font-bold">Tag / Version Name</label>
            <input 
              v-model="newTagName"
              type="text" 
              placeholder="e.g. v1.4.0"
              required
              class="w-full bg-black border border-[#333] p-3 rounded text-sm focus:border-[#FF3B30] outline-none transition-colors text-white font-mono"
            >
          </div>
          <div class="space-y-2">
            <label class="text-xs uppercase tracking-widest text-[#555] font-bold">Release Note / Message</label>
            <input 
              v-model="newTagMessage"
              type="text" 
              placeholder="e.g. Initial beta release with forms manager"
              class="w-full bg-black border border-[#333] p-3 rounded text-sm focus:border-[#FF3B30] outline-none transition-colors text-white"
            >
          </div>
        </div>

        <button 
          :disabled="tagging || !newTagName"
          type="submit"
          class="w-full bg-[#FF3B30] hover:bg-[#E6352B] text-white py-4 rounded-xl font-bold transition-all transform hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:scale-100 uppercase tracking-widest text-sm"
        >
          {{ tagging ? 'Publishing Version Tag...' : 'Publish Version Tag' }}
        </button>

        <div
          v-if="tagStatusMessage"
          class="p-4 rounded-lg bg-black border border-[#222] text-center text-sm font-mono"
          :class="tagStatusMessage.includes('failed') || tagStatusMessage.includes('Error') ? 'text-red-400' : 'text-[#FF3B30]'"
        >
          {{ tagStatusMessage }}
        </div>
      </form>

      <!-- Existing Tags List -->
      <div class="mt-10 border-t border-[#2A2A2A] pt-10">
        <div class="flex justify-between items-center mb-6">
          <h4 class="text-lg font-serif italic text-white">Release History & Version Tags</h4>
          <button 
            type="button"
            @click="fetchTags" 
            :disabled="loadingTags"
            class="text-xs font-mono uppercase tracking-wider text-[#FF3B30] hover:text-[#E6352B] transition-colors flex items-center gap-1.5 cursor-pointer"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" :class="{'animate-spin': loadingTags}" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 15H17" />
            </svg>
            Reload Tags
          </button>
        </div>

        <div v-if="loadingTags" class="text-center py-8 text-neutral-400 text-sm italic">
          Fetching release tags...
        </div>

        <div v-else-if="tags.length === 0" class="text-center py-8 text-neutral-500 text-sm italic border border-[#222] border-dashed rounded-lg bg-black/40">
          No version tags defined yet. Submit a tag above to tag your workspace.
        </div>

        <div v-else class="space-y-4 max-h-[350px] overflow-y-auto pr-2 custom-scrollbar justify-items-stretch">
          <div 
            v-for="tag in tags" 
            :key="tag.tag_name"
            class="bg-black border border-[#222] rounded-xl p-5 hover:border-[#FF3B30]/30 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
          >
            <div class="space-y-1.5 flex-1 min-w-0">
              <div class="flex items-center gap-2.5">
                <span class="text-xs font-bold font-mono text-white tracking-tight">{{ tag.tag_name }}</span>
                <span 
                  class="text-[9px] uppercase tracking-wider px-2 py-0.5 rounded font-black font-mono border"
                  :class="tag.is_git 
                    ? 'bg-green-500/10 text-green-400 border-green-500/20' 
                    : 'bg-amber-500/10 text-amber-400 border-amber-500/20'"
                >
                  {{ tag.is_git ? 'Git Tag' : 'DB Only (Fallback)' }}
                </span>
              </div>
              <p class="text-sm text-neutral-300 line-clamp-2 md:line-clamp-1">
                {{ tag.message || 'No release description.' }}
              </p>
            </div>
            <div class="text-right flex-shrink-0">
              <span class="text-xs font-mono text-neutral-500 block">
                {{ new Date(tag.created_at).toLocaleString() }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
