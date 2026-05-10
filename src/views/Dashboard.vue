<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { LayoutGrid, Plus, Globe, ArrowRight } from 'lucide-vue-next';

const sites = ref<string[]>([]);
const router = useRouter();

const fetchSites = async () => {
  const res = await fetch('/api/sites');
  sites.value = await res.json();
};

onMounted(fetchSites);

const openEditor = (site: string) => {
  router.push(`/editor/${site}`);
};
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex justify-between items-end mb-12">
      <div>
        <h2 class="text-3xl font-serif italic mb-2">Sites</h2>
        <p class="text-sm opacity-50 font-mono">Manage your multi-domain PHP application</p>
      </div>
      <button class="bg-[#F27D26] text-black px-4 py-2 text-xs font-bold uppercase tracking-wider rounded border border-[#F27D26] hover:bg-transparent hover:text-[#F27D26] transition-all flex items-center gap-2">
        <Plus :size="16" /> Add New Site
      </button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div 
        v-for="site in sites" 
        :key="site"
        @click="openEditor(site)"
        class="group border border-[#2A2A2A] p-6 bg-[#181818] cursor-pointer hover:border-[#F27D26] transition-all relative overflow-hidden"
      >
        <div class="flex justify-between items-start mb-8 relative z-10">
          <Globe :size="24" class="opacity-40 group-hover:text-[#F27D26] group-hover:opacity-100 transition-all" />
          <div class="text-[10px] uppercase tracking-widest opacity-30 font-mono">active</div>
        </div>
        
        <h3 class="text-xl font-serif italic mb-1">{{ site }}</h3>
        <p class="text-[10px] font-mono opacity-40 uppercase tracking-tighter mb-4">/repo-php/content/{{ site }}</p>
        
        <div class="flex items-center gap-2 text-[#F27D26] text-xs font-bold uppercase tracking-wider opacity-0 group-hover:opacity-100 transform translate-x-[-10px] group-hover:translate-x-0 transition-all">
          Manage <ArrowRight :size="14" />
        </div>

        <div class="absolute right-[-20px] bottom-[-20px] opacity-[0.03] scale-[4] group-hover:opacity-[0.07] transition-all pointer-events-none">
          <LayoutGrid :size="32" />
        </div>
      </div>
    </div>
    
    <div v-if="sites.length === 0" class="border border-dashed border-[#2A2A2A] py-20 text-center opacity-30">
        <p class="text-sm italic">No sites found in content directory.</p>
    </div>
  </div>
</template>
