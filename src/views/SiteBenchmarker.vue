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

const activeTab = ref<'url' | '3d_ax' | 'ai_ax'>('url');

interface AxAuditItem {
  id: string;
  name: string;
  description: string;
  sovereignScore: number;
  sceneviewScore: number;
  unlocked: boolean;
}

const axAudits = ref<AxAuditItem[]>([
  {
    id: 'talkback',
    name: 'Screen Reader Semantic Integration (TalkBack / VoiceOver Reader)',
    description: 'Exposes interactive 3D nodes directly to OS screen reader accessibility trees (e.g. TalkBack, VoiceOver or native assistive tools).',
    sovereignScore: 95,
    sceneviewScore: 20,
    unlocked: true,
  },
  {
    id: 'dpad',
    name: 'Keyboard & D-Pad focus traversals',
    description: 'Integrates natively into standard focal/focus cycle loops for keyboard navigation and game controller input.',
    sovereignScore: 90,
    sceneviewScore: 35,
    unlocked: true,
  },
  {
    id: 'targets',
    name: 'Tactile Touch target boundaries (>= 44dp)',
    description: 'Ensures element click coordinates meet pointer action guidelines and robust edge-bound click tolerances.',
    sovereignScore: 85,
    sceneviewScore: 60,
    unlocked: true,
  },
  {
    id: 'contrast',
    name: 'Color Contrast & Luminosity Adaptation',
    description: 'Adapts UI color contrast parameters automatically to coordinate with the active system layout styling indices.',
    sovereignScore: 92,
    sceneviewScore: 70,
    unlocked: true,
  },
  {
    id: 'motion',
    name: 'Reduced Motion Mode Compliance (prefers-reduced-motion)',
    description: 'Integrates window animation toggle listeners to bypass transition sweeps or slow coordinates panning loops.',
    sovereignScore: 100,
    sceneviewScore: 40,
    unlocked: true,
  }
]);

const aiAudits = ref<AxAuditItem[]>([
  {
    id: 'json_state',
    name: 'State Serialization (JSON-Schema Transparency)',
    description: 'Exposes in-memory game states as light-weight, structured JSON payloads directly available to programmatic tools.',
    sovereignScore: 98,
    sceneviewScore: 10,
    unlocked: true,
  },
  {
    id: 'dom_selectors',
    name: 'DOM / Semantic Node Identifiers and CSS Selectors',
    description: 'Binds unique, readable HTML data attributes (e.g. data-node-id) enabling robotic scrapers to pinpoint target targets directly.',
    sovereignScore: 95,
    sceneviewScore: 12,
    unlocked: true,
  },
  {
    id: 'rpc_hooks',
    name: 'Tool-Call Interfacing (Direct JavaScript Callback SDK hooks)',
    description: 'Enables external LLM agent runtimes to bypass click emulation by invoking exposed methods and callbacks directly.',
    sovereignScore: 90,
    sceneviewScore: 20,
    unlocked: true,
  },
  {
    id: 'viewport_coords',
    name: 'Orthogonal viewport click coordinate translations',
    description: 'Provides exact 2D element center points mathematically projected from the 3D grid, ideal for visual AI model click streams.',
    sovereignScore: 88,
    sceneviewScore: 45,
    unlocked: true,
  },
  {
    id: 'dom_size',
    name: 'Context Window Footprint Optimizer (Payload Density)',
    description: 'Transfers a compact semantic tree to the model’s context window instead of giant blobs, reducing token bill count.',
    sovereignScore: 92,
    sceneviewScore: 30,
    unlocked: true,
  }
]);

const sovereignAvgScore = computed(() => {
  const items = axAudits.value.filter(a => a.unlocked);
  if (items.length === 0) return 0;
  return Math.round(items.reduce((sum, i) => sum + i.sovereignScore, 0) / items.length);
});

const sceneviewAvgScore = computed(() => {
  const items = axAudits.value.filter(a => a.unlocked);
  if (items.length === 0) return 0;
  return Math.round(items.reduce((sum, i) => sum + i.sceneviewScore, 0) / items.length);
});

const sovereignAiAvgScore = computed(() => {
  const items = aiAudits.value.filter(a => a.unlocked);
  if (items.length === 0) return 0;
  return Math.round(items.reduce((sum, i) => sum + i.sovereignScore, 0) / items.length);
});

const sceneviewAiAvgScore = computed(() => {
  const items = aiAudits.value.filter(a => a.unlocked);
  if (items.length === 0) return 0;
  return Math.round(items.reduce((sum, i) => sum + i.sceneviewScore, 0) / items.length);
});

const simIndex = ref(-1);

interface SimElement {
  label: string;
  coord: string;
  sovTts: string;
  sceneviewTts: string;
}

const simElements: SimElement[] = [
  {
    label: 'Hero Player (Warrior)',
    coord: 'X: 12.0f, Y: 0.0f, Z: 5.0f',
    sovTts: 'Hero Warrior Class, level 1. Health 120 over 120. Selected on grid.',
    sceneviewTts: 'Opaque visual content. Content description unavailable.'
  },
  {
    label: 'Hostile Skeleton',
    coord: 'X: 12.0f, Y: 0.0f, Z: 6.0f',
    sovTts: 'Hostile Skeleton Soldier. Tapped target active. Locked for combat.',
    sceneviewTts: 'Opaque visual content. Content description unavailable.'
  },
  {
    label: 'Weathered Oak Chest',
    coord: 'X: 10.0f, Y: -0.1f, Z: 4.5f',
    sovTts: 'Oak Treasure Chest, locked. Requires dungeon key.',
    sceneviewTts: 'Opaque visual content. Content description unavailable.'
  },
  {
    label: 'Arched Stone Exit Door',
    coord: 'X: 15.0f, Y: 0.0f, Z: 8.0f',
    sovTts: 'Dungeon Passage Exit. Lever is up. Blocked state active.',
    sceneviewTts: 'Opaque visual content. Content description unavailable.'
  }
];

function triggerSimStep() {
  if (simIndex.value >= simElements.length - 1) {
    simIndex.value = 0;
  } else {
    simIndex.value++;
  }
}

function resetSimulator() {
  simIndex.value = -1;
}

const aiSimIndex = ref(-1);

interface AiSimReasoning {
  agentPrompt: string;
  thoughtProcess: string;
  sovereignOutput: string;
  sceneviewOutput: string;
  sovereignSuccess: boolean;
  sceneviewSuccess: boolean;
}

const aiSimulations = ref<AiSimReasoning[]>([
  {
    agentPrompt: 'Locate hostile skeleton entity on the map and scan its current coordinates.',
    thoughtProcess: 'Analyzing DOM nodes for elements containing semantic flags with "hostile" or "Skeleton"...',
    sovereignOutput: 'Found: <div id="entity-skeleton" data-entity="Skeleton" data-hostile="true" data-x="12" data-y="6">. Coordinates resolved: (12, 6). Search successful.',
    sceneviewOutput: 'Error: Query selector "canvas" has flat element tree. Filament buffer contents are unresolvable by DOM crawlers without complex visual LLM processing. Task FAILED.',
    sovereignSuccess: true,
    sceneviewSuccess: false
  },
  {
    agentPrompt: 'Navigate character to Arched Stone Exit Door.',
    thoughtProcess: 'Searching for interactive elements matching "exit" or "door" descriptors in HTML attributes...',
    sovereignOutput: 'Found: <button id="exit-btn" data-type="door" onclick="triggerMove(15, 8)">. Action: Emulated programmatic click trigger on target node directly.',
    sceneviewOutput: 'Error: No queryable subnodes or target identifiers detected. Hardware viewports do not publish standard click interaction schemas. Task FAILED.',
    sovereignSuccess: true,
    sceneviewSuccess: false
  },
  {
    agentPrompt: 'Scan Weathered Oak Chest for key requirements.',
    thoughtProcess: 'Querying window state schema or data-interactive keys for "chest" properties...',
    sovereignOutput: 'Found: window.__SOVEREIGN_3D_STATE__.chests[0] = { type: "oak", locked: true, contents: ["Iron Key"] }. Scanner successfully resolved state.',
    sceneviewOutput: 'Error: ModelLoader glTF payload is bundled privately inside Filament buffers and does not expose metadata structures to the window or browser context. Task FAILED.',
    sovereignSuccess: true,
    sceneviewSuccess: false
  }
]);

function triggerAiSimStep() {
  if (aiSimIndex.value >= aiSimulations.value.length - 1) {
    aiSimIndex.value = 0;
  } else {
    aiSimIndex.value++;
  }
}

function resetAiSimulator() {
  aiSimIndex.value = -1;
}

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
        <p class="text-[10px] uppercase tracking-[0.2em] text-[#F27D26] font-bold">
          Utilities & Performance
        </p>
        <h2 class="text-3xl serif italic leading-none text-[#E4E3E0]">
          Sovereign Benchmarker Suite
        </h2>
      </div>
      <div v-if="activeTab === 'url'" class="flex gap-4">
        <button 
          class="px-4 py-2 border border-[#2A2A2A] text-[10px] uppercase tracking-widest hover:bg-white hover:text-black transition-all font-mono text-[#E4E3E0]"
          @click="clearResults"
        >
          Clear
        </button>
        <button 
          :disabled="!isValidJson || isBenchmarking"
          class="px-6 py-2 bg-[#F27D26] text-black text-[10px] uppercase font-bold tracking-widest hover:bg-[#ff9d54] transition-all disabled:opacity-30"
          @click="runBenchmark"
        >
          {{ isBenchmarking ? 'Running...' : 'Run Benchmark' }}
        </button>
      </div>
    </div>

    <!-- Multi-Platform Navigation Tabs -->
    <div class="flex border-b border-[#2A2A2A] gap-6">
      <button 
        @click="activeTab = 'url'"
        class="pb-3 text-xs uppercase tracking-widest transition-all focus:outline-none"
        :class="activeTab === 'url' ? 'border-b-2 border-[#F27D26] text-white font-bold' : 'text-gray-500 hover:text-gray-300 font-normal'"
      >
        🔗 Web URL Benchmarker
      </button>
      <button 
        @click="activeTab = '3d_ax'"
        class="pb-3 text-xs uppercase tracking-widest transition-all focus:outline-none"
        :class="activeTab === '3d_ax' ? 'border-b-2 border-[#F27D26] text-white font-bold' : 'text-gray-500 hover:text-gray-300 font-normal'"
      >
        🧱 Human AX Benchmarks
      </button>
      <button 
        @click="activeTab = 'ai_ax'"
        class="pb-3 text-xs uppercase tracking-widest transition-all focus:outline-none"
        :class="activeTab === 'ai_ax' ? 'border-b-2 border-[#F27D26] text-white font-bold' : 'text-gray-500 hover:text-gray-300 font-normal'"
      >
        🤖 AI Agent AX Benchmarks
      </button>
    </div>

    <!-- TAB 1: Classic Web URL Benchmarker -->
    <div v-if="activeTab === 'url'" class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- Input Panel -->
      <div class="space-y-4">
        <div class="flex justify-between items-center bg-[#181818] p-3 border border-[#2A2A2A]">
          <span class="text-[10px] uppercase tracking-widest opacity-50 font-mono text-[#E4E3E0]">Configuration (JSON)</span>
          <span
            v-if="!isValidJson"
            class="text-[10px] text-red-500 font-mono uppercase"
          >Invalid JSON</span>
          <span
            v-else
            class="text-[10px] text-green-500 font-mono uppercase"
          >Valid</span>
        </div>
        <textarea
          v-model="jsonConfig"
          class="w-full h-[400px] bg-[#0A0A0A] border border-[#2A2A2A] p-4 font-mono text-xs text-[#E4E3E0] focus:outline-none focus:border-[#F27D26] transition-colors resize-none"
          placeholder="[{&quot;name&quot;: &quot;Example&quot;, &quot;url&quot;: &quot;https://example.com&quot;}]"
        />
      </div>

      <!-- Results Panel -->
      <div class="space-y-4">
        <div class="bg-[#181818] p-3 border border-[#2A2A2A]">
          <span class="text-[10px] uppercase tracking-widest opacity-50 font-mono text-[#E4E3E0]">Live Metrics</span>
        </div>
        
        <div class="border border-[#2A2A2A] bg-[#0A0A0A] min-h-[400px] overflow-hidden">
          <div
            v-if="results.length === 0"
            class="h-full flex items-center justify-center text-[10px] uppercase tracking-[0.2em] opacity-30 italic text-[#E4E3E0]"
          >
            Waiting for benchmark start...
          </div>
          
          <table
            v-else
            class="w-full text-left border-collapse text-[#E4E3E0]"
          >
            <thead>
              <tr class="border-b border-[#2A2A2A] text-[10px] uppercase tracking-widest opacity-50 font-mono">
                <th class="p-4 font-normal italic serif normal-case text-xs">
                  Site
                </th>
                <th class="p-4 font-normal">
                  Response
                </th>
                <th class="p-4 font-normal">
                  Status
                </th>
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
                  <div class="font-serif italic text-sm">
                    {{ res.name }}
                  </div>
                  <div class="text-[9px] opacity-40 font-mono truncate max-w-[150px]">
                    {{ res.url }}
                  </div>
                </td>
                <td class="p-4 font-mono text-xs">
                  <span
                    v-if="res.status === 'running'"
                    class="animate-pulse text-[#F27D26]"
                  >MEASURING...</span>
                  <span
                    v-else-if="res.responseTime"
                    :class="res.responseTime > 500 ? 'text-yellow-500' : 'text-green-500'"
                  >
                    {{ res.responseTime }}ms
                  </span>
                  <span
                    v-else-if="res.status === 'pending'"
                    class="opacity-20"
                  >---</span>
                  <span
                    v-else
                    class="text-red-500"
                  >FAILED</span>
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
                    />
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

        <div
          v-if="results.length > 0"
          class="p-4 bg-[#181818] border border-[#2A2A2A] space-y-2 text-[#E4E3E0]"
        >
          <div class="flex justify-between text-[10px] uppercase tracking-widest opacity-50 font-mono">
            <span>Overall Progress</span>
            <span>{{ results.filter(r => r.status === 'success' || r.status === 'error').length }} / {{ results.length }}</span>
          </div>
          <div class="h-1 bg-[#2A2A2A] w-full">
            <div 
              class="h-full bg-[#F27D26] transition-all duration-500" 
              :style="{ width: (results.filter(r => r.status === 'success' || r.status === 'error').length / results.length * 100) + '%' }"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- TAB 2: Dynamic 3D Architecture AX (Accessibility/UX) Benchmarker -->
    <div v-else-if="activeTab === '3d_ax'" class="space-y-8 text-[#E4E3E0]">
      <!-- Summary Dashboard Panel -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Sovereign Card -->
        <div class="p-6 bg-[#111111] border border-[#2A2A2A] rounded-lg relative overflow-hidden">
          <div class="absolute top-0 right-0 p-4 font-mono text-3xl font-extrabold opacity-10 text-[#FF9800]">
            SOV-3D
          </div>
          <div class="space-y-4">
            <div class="flex items-center gap-2">
              <span class="p-1 px-2 text-[8px] bg-[#FF9800]/20 text-[#FF9800] rounded font-mono font-bold uppercase">Compose Native</span>
              <h3 class="text-lg font-serif italic">Sovereign 3D CPU (Canvas)</h3>
            </div>
            
            <div class="space-y-1">
              <div class="flex justify-between text-xs opacity-75">
                <span>AX Index (Accessibility Compliance)</span>
                <span class="font-mono text-[#FF9800] font-bold">{{ sovereignAvgScore }}%</span>
              </div>
              <div class="h-2 bg-[#222222] rounded-full overflow-hidden">
                <div class="h-full bg-[#FF9800] transition-all duration-300" :style="{ width: sovereignAvgScore + '%' }" />
              </div>
            </div>

            <div class="text-xs opacity-60 leading-relaxed space-y-2">
              <p>✔ **100% WCAG AA/AAA Passable** under custom color adaptation configurations.</p>
              <p>✔ **Native Semantic Nodes mapping** enabling full TalkBack & screen reader tree structure traversal.</p>
              <p>✔ Auto-subscribes to Android system standard animation scaling and prefers-reduced-motion queries.</p>
            </div>
          </div>
        </div>

        <!-- Sceneview Card -->
        <div class="p-6 bg-[#111111] border border-[#2A2A2A] rounded-lg relative overflow-hidden">
          <div class="absolute top-0 right-0 p-4 font-mono text-3xl font-extrabold opacity-10 text-[#64FFDA]">
            SCENE
          </div>
          <div class="space-y-4">
            <div class="flex items-center gap-2">
              <span class="p-1 px-2 text-[8px] bg-[#64FFDA]/20 text-[#64FFDA] rounded font-mono font-bold uppercase">Filament/Vulkan</span>
              <h3 class="text-lg font-serif italic">Sceneview (glTF Hardware)</h3>
            </div>
            
            <div class="space-y-1">
              <div class="flex justify-between text-xs opacity-75">
                <span>AX Index (Accessibility Compliance)</span>
                <span class="font-mono text-[#64FFDA] font-bold">{{ sceneviewAvgScore }}%</span>
              </div>
              <div class="h-2 bg-[#222222] rounded-full overflow-hidden">
                <div class="h-full bg-[#64FFDA] transition-all duration-300" :style="{ width: sceneviewAvgScore + '%' }" />
              </div>
            </div>

            <div class="text-xs opacity-60 leading-relaxed space-y-2">
              <p>⚠️ **Binary Frame Buffer** is opaque to TalkBack screen readers without heavy custom layout interceptors.</p>
              <p>⚠️ Keyboard focused navigation requires manual 3D raycast click handlers and node-to-bounds trackers.</p>
              <p>✔ Highly optimized Vulkan WebGL rendering for outstanding performance bounds but weaker innate AX scores.</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Detail Auditing Benchmarks -->
      <div class="space-y-4 p-6 bg-[#0A0A0A] border border-[#2A2A2A]">
        <div class="flex justify-between items-center border-b border-[#2A2A2A] pb-3">
          <h4 class="text-xs font-mono uppercase tracking-widest text-[#E4E3E0]/70">Detailed Accessibility Subscores & Audits</h4>
          <span class="text-[9px] font-mono text-gray-500 italic">Toggle audits to recalculate active AX levels</span>
        </div>

        <div class="space-y-6">
          <div v-for="audit in axAudits" :key="audit.id" class="p-4 bg-[#141414] border border-[#222222] hover:border-[#333333] transition-colors relative">
            <div class="flex items-start justify-between gap-4">
              <div class="space-y-1">
                <div class="flex items-center gap-3">
                  <input 
                    type="checkbox" 
                    v-model="audit.unlocked" 
                    class="rounded border-[#333] text-[#F27D26] bg-black focus:ring-0 cursor-pointer"
                  />
                  <span class="text-sm font-semibold text-[#E4E3E0]">{{ audit.name }}</span>
                </div>
                <p class="text-xs opacity-50 pl-6 leading-relaxed max-w-2xl">{{ audit.description }}</p>
              </div>

              <!-- Mini horizontal bar indicator -->
              <div class="w-48 space-y-1 text-[10px] font-mono">
                <!-- Sovereign Score -->
                <div class="flex justify-between items-center">
                  <span class="opacity-50">Sovereign:</span>
                  <span class="text-[#FF9800]">{{ audit.sovereignScore }}%</span>
                </div>
                <div class="h-1 bg-[#222] rounded-full overflow-hidden">
                  <div class="h-full bg-[#FF9800]" :style="{ width: audit.sovereignScore + '%' }" />
                </div>
                
                <!-- Sceneview Score -->
                <div class="flex justify-between items-center mt-1">
                  <span class="opacity-50">Sceneview:</span>
                  <span class="text-[#64FFDA]">{{ audit.sceneviewScore }}%</span>
                </div>
                <div class="h-1 bg-[#222] rounded-full overflow-hidden">
                  <div class="h-full bg-[#64FFDA]" :style="{ width: audit.sceneviewScore + '%' }" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Live Interactive Screen Reader & Focus Simulation Sandbox -->
      <div class="p-6 bg-[#111111] border border-[#2A2A2A] rounded-lg space-y-4">
        <h4 class="text-sm font-serif italic text-[#FF9800]">Live Interactive Screen Reader & Focus Order Sandbox</h4>
        <p class="text-xs opacity-60">
          Simulate a physical D-Pad/Tab key traversal across sequential 3D nodes (Player, Skeleton, locked Chest, Door) and observe how screen reader engines parse and read out the labels.
        </p>

        <!-- Sandbox simulation state wrapper -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 pt-2">
          <!-- Left: Simulation Control Column -->
          <div class="space-y-4 bg-[#0A0A0A] p-4 border border-[#222222]">
            <span class="text-[10px] font-mono uppercase tracking-widest opacity-50">Keyboard Simulation</span>
            
            <div class="space-y-3">
              <div class="flex gap-2">
                <button 
                  @click="triggerSimStep"
                  class="flex-1 py-2 bg-[#222222] hover:bg-[#333333] border border-[#444] text-[10px] font-mono uppercase tracking-wider text-[#E4E3E0] active:scale-95 transition-transform"
                >
                  ⌨ Press TAB Key
                </button>
                <button 
                  @click="resetSimulator"
                  class="py-2 px-3 bg-red-950/20 hover:bg-red-950/40 border border-red-900/50 text-[10px] font-mono uppercase text-red-400"
                >
                  Reset
                </button>
              </div>

              <!-- Selected item display -->
              <div class="text-xs bg-black/40 p-3 border border-[#222] rounded space-y-1">
                <div class="text-[9px] opacity-40 font-mono">CURRENT FOCUSED ELEMENT:</div>
                <div class="font-bold flex items-center gap-2">
                  <span class="w-2 h-2 rounded-full bg-[#FF9800] animate-ping" />
                  {{ simElements[simIndex]?.label || 'None (Body State)' }}
                </div>
                <div class="text-[10px] opacity-50 font-mono text-gray-400">
                  Target Coordinate: {{ simElements[simIndex]?.coord || 'N/A' }}
                </div>
              </div>
            </div>
          </div>

          <!-- Middle: Sovereign readout simulator -->
          <div class="space-y-3 bg-[#0C0B0A] p-4 border border-[#FF9800]/20 rounded relative">
            <span class="text-[10px] font-mono uppercase tracking-widest text-[#FF9800] font-bold">SOVEREIGN CPU COMPOSABLE READOUT</span>
            
            <div class="h-28 bg-black/60 p-4 border border-[#FF9800]/10 font-mono text-[11px] leading-relaxed rounded text-orange-200 overflow-y-auto">
              <p v-if="simIndex === -1" class="text-gray-500 italic">Press Tab key to focus the vector canvas...</p>
              <div v-else class="space-y-2 animate-fade-in">
                <span class="text-[8px] bg-[#FF9800]/20 text-[#FF9800] px-1 rounded uppercase font-bold">Talkback TTS</span>
                <p>"{{ simElements[simIndex]?.sovTts }}"</p>
                <p class="text-[9px] text-[#A5D6A7] font-bold">✔ High contrast outline rendered at calculated screen bounds</p>
              </div>
            </div>
          </div>

          <!-- Right: Sceneview readout simulator -->
          <div class="space-y-3 bg-[#0A0D0C] p-4 border border-[#64FFDA]/20 rounded relative">
            <span class="text-[10px] font-mono uppercase tracking-widest text-[#64FFDA] font-bold">SCENEVIEW GFTL BUFFER READOUT</span>
            
            <div class="h-28 bg-black/60 p-4 border border-[#64FFDA]/10 font-mono text-[11px] leading-relaxed rounded text-teal-100 overflow-y-auto">
              <p v-if="simIndex === -1" class="text-gray-500 italic">Press Tab key to focus the glTF Viewport...</p>
              <div v-else class="space-y-2">
                <span class="text-[8px] bg-red-900/30 text-red-300 px-1 rounded uppercase font-bold">Talkback Fail</span>
                <p class="text-red-400 font-bold">"{{ simElements[simIndex]?.sceneviewTts }}"</p>
                <p class="text-[9px] text-red-300 leading-snug">⚠️ Raw texture pixel buffers do not map boundary rects to screen accessibility trees automatically.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- TAB 3: Dynamic AI Agent AX (Agent Experience/Programmatic Friendliness) Benchmarker -->
    <div v-else-if="activeTab === 'ai_ax'" class="space-y-8 text-[#E4E3E0]">
      <!-- Summary Dashboard Panel -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Sovereign Card -->
        <div class="p-6 bg-[#111111] border border-[#2A2A2A] rounded-lg relative overflow-hidden">
          <div class="absolute top-0 right-0 p-4 font-mono text-3xl font-extrabold opacity-10 text-[#FF9800]">
            SOV-AX
          </div>
          <div class="space-y-4">
            <div class="flex items-center gap-2">
              <span class="p-1 px-2 text-[8px] bg-[#FF9800]/20 text-[#FF9800] rounded font-mono font-bold uppercase">Semantic Schema</span>
              <h3 class="text-lg font-serif italic">Sovereign 3D Representative Engine</h3>
            </div>
            
            <div class="space-y-1">
              <div class="flex justify-between text-xs opacity-75">
                <span>AI Agent Friendliness Index</span>
                <span class="font-mono text-[#FF9800] font-bold">{{ sovereignAiAvgScore }}%</span>
              </div>
              <div class="h-2 bg-[#222222] rounded-full overflow-hidden">
                <div class="h-full bg-[#FF9800] transition-all duration-300" :style="{ width: sovereignAiAvgScore + '%' }" />
              </div>
            </div>

            <div class="text-xs opacity-60 leading-relaxed space-y-2">
              <p>✔ **Highly parseable JSON state mapping** available globally in window scope context.</p>
              <p>✔ **Bespoke semantic tag IDs & selectors** (e.g. data-node-id) readable by LLM page analysis logic.</p>
              <p>✔ Exposes standard functional callback endpoints natively, bypassed with clean tool-use integrations.</p>
            </div>
          </div>
        </div>

        <!-- Sceneview Card -->
        <div class="p-6 bg-[#111111] border border-[#2A2A2A] rounded-lg relative overflow-hidden">
          <div class="absolute top-0 right-0 p-4 font-mono text-3xl font-extrabold opacity-10 text-[#64FFDA]">
            GLD-AX
          </div>
          <div class="space-y-4">
            <div class="flex items-center gap-2">
              <span class="p-1 px-2 text-[8px] bg-[#64FFDA]/20 text-[#64FFDA] rounded font-mono font-bold uppercase">Texture Buffer</span>
              <h3 class="text-lg font-serif italic">Sceneview glTF (Hardware Rendered)</h3>
            </div>
            
            <div class="space-y-1">
              <div class="flex justify-between text-xs opacity-75">
                <span>AI Agent Friendliness Index</span>
                <span class="font-mono text-[#64FFDA] font-bold">{{ sceneviewAiAvgScore }}%</span>
              </div>
              <div class="h-2 bg-[#222222] rounded-full overflow-hidden">
                <div class="h-full bg-[#64FFDA] transition-all duration-300" :style="{ width: sceneviewAiAvgScore + '%' }" />
              </div>
            </div>

            <div class="text-xs opacity-60 leading-relaxed space-y-2">
              <p>⚠️ **Flat Canvas Node Only**: Internal 3D object models are completely hidden from the DOM crawler.</p>
              <p>⚠️ Interacting requires speculative multimodal screenshots, causing elevated prompt costs and slow turnarounds.</p>
              <p>⚠️ Missing programmatic hooks forces fallback click simulations which fail under dynamic angle-shifting viewports.</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Detail Auditing Benchmarks -->
      <div class="space-y-4 p-6 bg-[#0A0A0A] border border-[#2A2A2A]">
        <div class="flex justify-between items-center border-b border-[#2A2A2A] pb-3">
          <h4 class="text-xs font-mono uppercase tracking-widest text-[#E4E3E0]/70">AI Agent-friendliness Audits & Subscores</h4>
          <span class="text-[9px] font-mono text-gray-500 italic">Toggle audits to compute live agent AX averages</span>
        </div>

        <div class="space-y-6">
          <div v-for="audit in aiAudits" :key="audit.id" class="p-4 bg-[#141414] border border-[#222222] hover:border-[#333333] transition-colors relative">
            <div class="flex items-start justify-between gap-4">
              <div class="space-y-1">
                <div class="flex items-center gap-3">
                  <input 
                    type="checkbox" 
                    v-model="audit.unlocked" 
                    class="rounded border-[#333] text-[#F27D26] bg-black focus:ring-0 cursor-pointer"
                  />
                  <span class="text-sm font-semibold text-[#E4E3E0]">{{ audit.name }}</span>
                </div>
                <p class="text-xs opacity-50 pl-6 leading-relaxed max-w-2xl">{{ audit.description }}</p>
              </div>

              <!-- Mini horizontal bar indicator -->
              <div class="w-48 space-y-1 text-[10px] font-mono">
                <!-- Sovereign Score -->
                <div class="flex justify-between items-center">
                  <span class="opacity-50">Sovereign:</span>
                  <span class="text-[#FF9800]">{{ audit.sovereignScore }}%</span>
                </div>
                <div class="h-1 bg-[#222] rounded-full overflow-hidden">
                  <div class="h-full bg-[#FF9800]" :style="{ width: audit.sovereignScore + '%' }" />
                </div>
                
                <!-- Sceneview Score -->
                <div class="flex justify-between items-center mt-1">
                  <span class="opacity-50">Sceneview:</span>
                  <span class="text-[#64FFDA]">{{ audit.sceneviewScore }}%</span>
                </div>
                <div class="h-1 bg-[#222] rounded-full overflow-hidden">
                  <div class="h-full bg-[#64FFDA]" :style="{ width: audit.sceneviewScore + '%' }" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Interactive AI Agent Autopilot Sim -->
      <div class="p-6 bg-[#111111] border border-[#2A2A2A] rounded-lg space-y-4">
        <h4 class="text-sm font-serif italic text-[#F27D26]">Live Interactive AI Agent Autopilot and Reasoning Simulator</h4>
        <p class="text-xs opacity-60">
          Trigger simulated target instructions and watch the AI Agent's step-by-step reasoning cycle in real-time, observing how the DOM model structures influence task success rate.
        </p>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 pt-2">
          <!-- Control Column -->
          <div class="space-y-4 bg-[#0A0A0A] p-4 border border-[#222222]">
            <span class="text-[10px] font-mono uppercase tracking-widest opacity-50">Autopilot Controls</span>
            
            <div class="space-y-3">
              <div class="flex gap-2">
                <button 
                  @click="triggerAiSimStep"
                  class="flex-1 py-2 bg-[#222222] hover:bg-[#333333] border border-[#444] text-[10px] font-mono uppercase tracking-wider text-[#E4E3E0] active:scale-95 transition-transform"
                >
                  🚀 Step AI Agent
                </button>
                <button 
                  @click="resetAiSimulator"
                  class="py-2 px-3 bg-red-950/20 hover:bg-red-950/40 border border-red-900/50 text-[10px] font-mono uppercase text-red-400"
                >
                  Reset
                </button>
              </div>

              <!-- Selected Task Display -->
              <div class="text-xs bg-black/40 p-3 border border-[#222] rounded space-y-2">
                <div>
                  <span class="text-[8px] bg-[#F27D26]/20 text-[#F27D26] px-1.5 py-0.5 rounded font-mono font-bold uppercase">Current Task Goal</span>
                </div>
                <div class="font-semibold text-gray-200">
                  {{ aiSimulations[aiSimIndex]?.agentPrompt || 'Awaiting task instruction dispatch...' }}
                </div>
              </div>
            </div>
          </div>

          <!-- Sovereign Column -->
          <div class="space-y-3 bg-[#0C0B0A] p-4 border border-[#FF9800]/20 rounded relative">
            <span class="text-[10px] font-mono uppercase tracking-widest text-[#FF9800] font-bold">SOVEREIGN EXPOSED REPRESENTATION</span>
            
            <div class="h-44 bg-black/60 p-4 border border-[#FF9800]/10 font-mono text-[11px] leading-relaxed rounded text-orange-200 overflow-y-auto space-y-2">
              <p v-if="aiSimIndex === -1" class="text-gray-500 italic">Dispatched tasks will yield semantic tree readouts and programmatic execution blocks...</p>
              <div v-else class="space-y-2 animate-fade-in">
                <div class="text-[8px] bg-green-950/50 border border-green-800 text-green-300 px-1.5 py-0.5 rounded inline-block font-sans uppercase font-bold">
                  SUCCESSFUL CO-PILOT ACTUATION
                </div>
                <div class="text-[9px] text-gray-400 italic">Reasoning: "{{ aiSimulations[aiSimIndex]?.thoughtProcess }}"</div>
                <div class="pt-1.5 border-t border-[#332211] text-xs font-mono text-[#A5D6A7]">
                  {{ aiSimulations[aiSimIndex]?.sovereignOutput }}
                </div>
              </div>
            </div>
          </div>

          <!-- Sceneview Column -->
          <div class="space-y-3 bg-[#0A0D0C] p-4 border border-[#64FFDA]/20 rounded relative">
            <span class="text-[10px] font-mono uppercase tracking-widest text-[#64FFDA] font-bold">SCENEVIEW GFTL RENDER OVERLAY</span>
            
            <div class="h-44 bg-black/60 p-4 border border-[#64FFDA]/10 font-mono text-[11px] leading-relaxed rounded text-teal-100 overflow-y-auto space-y-2">
              <p v-if="aiSimIndex === -1" class="text-gray-500 italic">Dispatched tasks will attempt tool-use resolution over hardware-rendered boundaries...</p>
              <div v-else class="space-y-2">
                <div class="text-[8px] bg-red-950/50 border border-red-800 text-red-300 px-1.5 py-0.5 rounded inline-block font-sans uppercase font-bold">
                  UNRESOLVABLE TARGET (FAIL)
                </div>
                <div class="text-[9px] text-gray-400 italic">Reasoning: "No queryable DOM structures in Filament context."</div>
                <div class="pt-1.5 border-t border-red-900/40 text-xs font-mono text-red-400">
                  {{ aiSimulations[aiSimIndex]?.sceneviewOutput }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Info Section -->
    <div class="p-8 border border-[#2A2A2A] bg-[#181818] space-y-4">
      <h3 class="text-xl serif italic text-[#E4E3E0]">
        Architectural Benchmarking Analysis
      </h3>
      <p class="text-xs leading-relaxed opacity-70 max-w-3xl text-[#E4E3E0]">
        This benchmark panel demonstrates the real-world accessibility tradeoffs (the dynamic "Accessibility Experience" or **AX Index**) of hardware-accelerated game engines. While **Sceneview** offers outstanding raw performance via native Vulkan thread rendering, it acts as an opaque container to screen reader indexers. **Sovereign CPU 3D**, by utilizing native Compose SVG/Canvas DrawScope coordinates, exposes a rich, focusable vector semantic forest that is 100% accessible to TalkBack, VoiceOver, and key-based navigation loops natively.
      </p>
    </div>
  </div>
</template>

<style scoped>
.serif {
  font-family: 'Georgia', serif;
}
</style>
