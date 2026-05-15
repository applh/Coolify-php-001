<script setup lang="ts">
import { ref, computed } from 'vue';

interface SiteConfig {
  name: string;
  url: string;
}

interface BenchmarkResult {
  name: string;
  url: string;
  status: 'pending' | 'running' | 'success' | 'error';
  responseTime?: number;
  statusCode?: number;
  error?: string;
  timestamp: string;
}

const defaultConfig: SiteConfig[] = [
  { name: 'Google', url: 'https://www.google.com' },
  { name: 'GitHub', url: 'https://github.com' },
  { name: 'Vue.js', url: 'https://vuejs.org' },
  { name: 'Local PHP CMS', url: window.location.origin }
];

const jsonConfig = ref(JSON.stringify(defaultConfig, null, 2));
const results = ref<BenchmarkResult[]>([]);
const isBenchmarking = ref(false);

const parsedConfig = computed(() => {
  try {
    const parsed = JSON.parse(jsonConfig.value);
    if (Array.isArray(parsed)) return parsed as SiteConfig[];
    return [];
  } catch {
    return [];
  }
});

const isValidJson = computed(() => {
  try {
    JSON.parse(jsonConfig.value);
    return true;
  } catch {
    return false;
  }
});

async function runBenchmark() {
  if (!isValidJson.value) return;
  
  isBenchmarking.value = true;
  results.value = parsedConfig.value.map(site => ({
    ...site,
    status: 'pending',
    timestamp: new Date().toLocaleTimeString()
  }));

  for (let i = 0; i < results.value.length; i++) {
    const result = results.value[i];
    result.status = 'running';
    
    const start = performance.now();
    try {
      const response = await fetch(`/api/benchmark?url=${encodeURIComponent(result.url)}`);
      const data = await response.json();
      const end = performance.now();
      
      if (data.status === 'success') {
        result.status = 'success';
        result.responseTime = data.responseTime;
        result.statusCode = data.statusCode;
      } else {
        result.status = 'error';
        result.error = data.error;
        result.responseTime = Math.round(end - start);
      }
    } catch (err) {
      const error = err as Error;
      result.status = 'error';
      result.error = error.message || 'Failed to fetch';
      result.responseTime = Math.round(performance.now() - start);
    }
  }
  
  isBenchmarking.value = false;
}

function clearResults() {
  results.value = [];
}
</script>

<template>
  <div class="space-y-8 max-w-5xl mx-auto">
    <div class="flex justify-between items-end">
      <div class="space-y-1">
        <p class="text-[10px] uppercase tracking-[0.2em] text-[#F27D26] font-bold">Utilities</p>
        <h2 class="text-3xl serif italic leading-none">Site Benchmarker</h2>
      </div>
      <div class="flex gap-4">
         <button 
          @click="clearResults"
          class="px-4 py-2 border border-[#2A2A2A] text-[10px] uppercase tracking-widest hover:bg-white hover:text-black transition-all font-mono"
        >
          Clear
        </button>
        <button 
          @click="runBenchmark"
          :disabled="!isValidJson || isBenchmarking"
          class="px-6 py-2 bg-[#F27D26] text-black text-[10px] uppercase font-bold tracking-widest hover:bg-[#ff9d54] transition-all disabled:opacity-30"
        >
          {{ isBenchmarking ? 'Running...' : 'Run Benchmark' }}
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- Input Panel -->
      <div class="space-y-4">
        <div class="flex justify-between items-center bg-[#181818] p-3 border border-[#2A2A2A]">
          <span class="text-[10px] uppercase tracking-widest opacity-50 font-mono">Configuration (JSON)</span>
          <span v-if="!isValidJson" class="text-[10px] text-red-500 font-mono uppercase">Invalid JSON</span>
          <span v-else class="text-[10px] text-green-500 font-mono uppercase">Valid</span>
        </div>
        <textarea
          v-model="jsonConfig"
          class="w-full h-[400px] bg-[#0A0A0A] border border-[#2A2A2A] p-4 font-mono text-xs text-[#E4E3E0] focus:outline-none focus:border-[#F27D26] transition-colors resize-none"
          placeholder='[{"name": "Example", "url": "https://example.com"}]'
        ></textarea>
      </div>

      <!-- Results Panel -->
      <div class="space-y-4">
        <div class="bg-[#181818] p-3 border border-[#2A2A2A]">
          <span class="text-[10px] uppercase tracking-widest opacity-50 font-mono">Live Metrics</span>
        </div>
        
        <div class="border border-[#2A2A2A] bg-[#0A0A0A] min-h-[400px] overflow-hidden">
          <div v-if="results.length === 0" class="h-full flex items-center justify-center text-[10px] uppercase tracking-[0.2em] opacity-30 italic">
            Waiting for benchmark start...
          </div>
          
          <table v-else class="w-full text-left border-collapse">
            <thead>
              <tr class="border-b border-[#2A2A2A] text-[10px] uppercase tracking-widest opacity-50 font-mono">
                <th class="p-4 font-normal italic serif normal-case text-xs">Site</th>
                <th class="p-4 font-normal">Response</th>
                <th class="p-4 font-normal">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="res in results" 
                :key="res.name"
                class="border-b border-[#2A2A2A]/50 hover:bg-[#181818] transition-colors"
                :class="{'opacity-50': res.status === 'pending'}"
              >
                <td class="p-4">
                  <div class="font-serif italic text-sm">{{ res.name }}</div>
                  <div class="text-[9px] opacity-40 font-mono truncate max-w-[150px]">{{ res.url }}</div>
                </td>
                <td class="p-4 font-mono text-xs">
                  <span v-if="res.status === 'running'" class="animate-pulse text-[#F27D26]">MEASURING...</span>
                  <span v-else-if="res.responseTime" :class="res.responseTime > 500 ? 'text-yellow-500' : 'text-green-500'">
                    {{ res.responseTime }}ms
                  </span>
                  <span v-else-if="res.status === 'pending'" class="opacity-20">---</span>
                  <span v-else class="text-red-500">FAILED</span>
                </td>
                <td class="p-4 font-mono text-[10px]">
                  <div class="flex items-center gap-2">
                    <div 
                      class="w-1.5 h-1.5 rounded-full"
                      :class="{
                        'bg-yellow-500 animate-pulse': res.status === 'running',
                        'bg-green-500': res.status === 'success',
                        'bg-red-500': res.status === 'error',
                        'bg-gray-700': res.status === 'pending'
                      }"
                    ></div>
                    <span class="uppercase tracking-tighter">
                      {{ res.status }}
                      <span v-if="res.statusCode && res.statusCode > 0">({{ res.statusCode }})</span>
                    </span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="results.length > 0" class="p-4 bg-[#181818] border border-[#2A2A2A] space-y-2">
           <div class="flex justify-between text-[10px] uppercase tracking-widest opacity-50 font-mono">
             <span>Overall Progress</span>
             <span>{{ results.filter(r => r.status === 'success' || r.status === 'error').length }} / {{ results.length }}</span>
           </div>
           <div class="h-1 bg-[#2A2A2A] w-full">
             <div 
               class="h-full bg-[#F27D26] transition-all duration-500" 
               :style="{ width: (results.filter(r => r.status === 'success' || r.status === 'error').length / results.length * 100) + '%' }"
             ></div>
           </div>
        </div>
      </div>
    </div>

    <!-- Info Section -->
    <div class="p-8 border border-[#2A2A2A] bg-[#181818] space-y-4">
      <h3 class="text-xl serif italic">Technical Note</h3>
      <p class="text-xs leading-relaxed opacity-70 max-w-3xl">
        This benchmarker performs client-side <code>fetch</code> requests. Due to browser security (CORS), detailed status codes and body analysis are restricted for third-party domains. For precise server-to-server benchmarking, consider using the <span class="text-[#F27D26]">AI Media Tasks</span> system which contextually understands external resource health.
      </p>
    </div>
  </div>
</template>

<style scoped>
.serif {
  font-family: 'Georgia', serif;
}
</style>
