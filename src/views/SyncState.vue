<script setup lang="ts">
import { ref } from 'vue';

const importing = ref(false);
const importMessage = ref('');
const fileInput = ref<HTMLInputElement | null>(null);

const exportData = () => {
  window.open('/api/sync/export', '_blank');
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
</script>

<template>
  <div class="max-w-4xl mx-auto py-10 px-6">
    <div class="mb-12">
      <h2 class="text-3xl font-serif italic mb-4">
        Sync & Backup
      </h2>
      <p class="text-[#888] mb-8 leading-relaxed">
        Export your entire CMS state (database and PHP site files) as a single ZIP archive, 
        or import a previously exported archive to sync between environments.
      </p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
      <div class="bg-[#181818] border border-[#2A2A2A] p-8 rounded-lg">
        <h3 class="text-xl mb-4 font-semibold text-[#F27D26]">
          Export
        </h3>
        <p class="text-sm text-[#888] mb-6">
          Download a full backup of your CMS and all site content.
        </p>
        <button 
          class="bg-[#F27D26] text-black px-6 py-2 rounded font-bold hover:bg-[#ff8e3c] transition-colors"
          @click="exportData"
        >
          Download Export ZIP
        </button>
      </div>

      <div class="bg-[#181818] border border-[#2A2A2A] p-8 rounded-lg">
        <h3 class="text-xl mb-4 font-semibold text-[#F27D26]">
          Import
        </h3>
        <p class="text-sm text-[#888] mb-6">
          Upload a sync ZIP archive to restore your CMS state. 
          <span class="text-red-500 block mt-2">Warning: This will overwrite current database and PHP site files!</span>
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
            class="border border-[#F27D26] text-[#F27D26] px-6 py-2 rounded font-bold hover:bg-[#F27D26] hover:text-black transition-all disabled:opacity-50"
            @click="triggerImport"
          >
            {{ importing ? 'Importing...' : 'Upload Sync ZIP' }}
          </button>
          
          <div
            v-if="importMessage"
            class="mt-4 p-4 rounded bg-[#222] border border-[#333] text-sm italic"
          >
            {{ importMessage }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
