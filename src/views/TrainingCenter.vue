<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { 
  Search, Zap, Code, 
  ChevronLeft, ChevronRight, X, ExternalLink, Filter,
  Clock, Target, Grid3X3, List as ListIcon,
  LayoutDashboard, Database, Sparkles, TrendingUp
} from 'lucide-vue-next';

// Load taxonomy
import taxonomyData from '../../docs/training/taxonomy.json';

interface Slide {
  id: string | number;
  title: string;
  content: string;
  svgContent?: string;
  tags: string[];
  links?: string[];
  studentPrompts?: string[];
  codeReferences: string[];
}

interface SlideBundle {
  moduleId: string;
  title: string;
  totalSlides: number;
  bundleId: number;
  slides: Slide[];
}

interface Module {
  id: string;
  name: string;
  hours: [number, number];
  description?: string;
}

interface Phase {
  id: string;
  name: string;
  hours: [number, number];
  modules: Module[];
}

// Global data store for modules
const allSlidesData = ref<Record<string, SlideBundle>>({});

// Dynamic Module Discovery
const phases = ref<Phase[]>(taxonomyData.phases);
const searchQuery = ref('');
const activeView = ref<'atlas' | 'list'>('atlas');

// Navigation State
const selectedPhase = ref<Phase | null>(null);
const selectedModule = ref<Module | null>(null);
const currentBundle = ref<SlideBundle | null>(null);
const currentSlideIndex = ref(0);
const viewMode = ref<'dashboard' | 'phase' | 'reader'>('dashboard');
const isLoading = ref(false);
const isSyncing = ref(false);
const error = ref<string | null>(null);

// Initialize modules
onMounted(async () => {
  isLoading.value = true;
  try {
    // Vite's dynamic import for all slide JSONs
    const modulesImport = import.meta.glob<SlideBundle>('../../docs/training/slides/**/*.json', { import: 'default', eager: true });
    
    const data: Record<string, SlideBundle> = {};
    for (const path in modulesImport) {
      const bundle = modulesImport[path];
      if (bundle && bundle.moduleId) {
        // Some files might cover multiple modules (e.g. M093-M094)
        const ids = bundle.moduleId.split('-').map(id => id.trim());
        ids.forEach(id => {
          data[id] = bundle;
        });
      }
    }
    allSlidesData.value = data;
  } catch (err) {
    console.error('Failed to load dynamic modules:', err);
    error.value = 'Failed to index training curriculum.';
  } finally {
    isLoading.value = false;
  }
});

const syncWithProd = async () => {
  if (isSyncing.value) return;
  isSyncing.value = true;
  try {
    const res = await fetch('http://localhost:3000/api/admin/sync', {
      method: 'POST'
    });
    // Fallback if not mapped on node proxy:
    if (!res.ok) {
       await fetch('http://localhost:8000/admin/api/sync', {
          method: 'POST'
       });
    }
    alert('Sync with production completed (simulated).');
  } catch (e) {
    console.error(e);
    alert('Error syncing with Prod.');
  } finally {
    isSyncing.value = false;
  }
};

// Computed stats
const stats = computed(() => {
  const totalModules = phases.value.reduce((acc, p) => acc + p.modules.length, 0);
  const availableContentCount = Object.keys(allSlidesData.value).length;
  const coverage = Math.round((availableContentCount / totalModules) * 100);
  return {
    totalHours: 1000,
    totalModules,
    curriculumCoverage: `${coverage}%`,
    readyStatus: coverage > 50 ? 'OPTIMIZED' : 'DRAFT'
  };
});

const latestModules = computed(() => {
  // Sort modules by bundleId descending, or just pick the ones we have content for and take the last few phases
  const available = Object.keys(allSlidesData.value);
  
  // Flatten all modules and filter by availability
  const allFlattened = phases.value.flatMap(p => p.modules);
  const availableObjects = allFlattened.filter(m => available.includes(m.id));
  
  // Sort by ID descending (usually newer modules have higher numbers)
  return availableObjects.sort((a, b) => b.id.localeCompare(a.id)).slice(0, 4);
});

const filteredPhases = computed(() => {
  if (!searchQuery.value) return phases.value;
  const q = searchQuery.value.toLowerCase();
  return phases.value.filter(p => 
    p.name.toLowerCase().includes(q) || 
    p.modules.some(m => m.name.toLowerCase().includes(q))
  );
});

const currentSlide = computed(() => {
  if (!currentBundle.value || !currentBundle.value.slides.length) return null;
  return currentBundle.value.slides[currentSlideIndex.value];
});

const progress = computed(() => {
  if (!currentBundle.value) return 0;
  return ((currentSlideIndex.value + 1) / currentBundle.value.slides.length) * 100;
});

// Actions
async function openPhase(phase: Phase) {
  selectedPhase.value = phase;
  viewMode.value = 'phase';
}

async function startModule(module: Module) {
  const bundle = allSlidesData.value[module.id];
  if (!bundle) return;

  selectedModule.value = module;
  currentSlideIndex.value = 0;
  currentBundle.value = bundle;
  viewMode.value = 'reader';
}

function nextSlide() {
  if (currentBundle.value && currentSlideIndex.value < currentBundle.value.slides.length - 1) {
    currentSlideIndex.value++;
  }
}

function prevSlide() {
  if (currentSlideIndex.value > 0) {
    currentSlideIndex.value--;
  }
}

function closeReader() {
  viewMode.value = 'phase';
}

function backToDashboard() {
  viewMode.value = 'dashboard';
  selectedPhase.value = null;
}
</script>

<template>
  <div class="space-y-8 animate-in fade-in duration-700 max-w-[1400px] mx-auto px-4 md:px-8 py-8 min-h-screen">
    <!-- Breadcrumbs / Back button -->
    <div
      v-if="viewMode !== 'dashboard' && viewMode !== 'reader'"
      class="flex items-center gap-2 mb-4 animate-in slide-in-from-left duration-300"
    >
      <button
        class="flex items-center gap-1 text-xs text-white/40 hover:text-[#F27D26] transition-colors uppercase tracking-widest font-bold"
        @click="backToDashboard"
      >
        <LayoutDashboard class="w-3 h-3" />
        Dashboard
      </button>
      <ChevronRight class="w-3 h-3 text-white/10" />
      <span class="text-xs text-white/80 uppercase tracking-widest font-bold">{{ selectedPhase?.name }}</span>
    </div>

    <!-- Header -->
    <div
      v-if="viewMode !== 'reader'"
      class="flex flex-col md:flex-row justify-between items-end gap-6 border-b border-white/5 pb-8"
    >
      <div class="space-y-2">
        <div class="flex items-center gap-2 text-[#F27D26] text-[10px] font-black tracking-[0.2em] uppercase">
          <Target class="w-4 h-4" />
          Engineering Mastery Path
        </div>
        <h2 class="text-4xl font-serif italic text-white leading-tight">
          Training Center
        </h2>
        <p class="text-sm text-white/40 max-w-xl">
          A 1000-hour architecture curriculum indexed into 10 industrial phases. 
          Use the Atlas to browse the entire stack from foundations to agentic orchestration.
        </p>
      </div>

      <div class="flex gap-3 flex-wrap">
        <button 
          @click="syncWithProd"
          :disabled="isSyncing"
          class="bg-[#F27D26]/20 border border-[#F27D26] px-5 py-3 rounded-lg flex flex-col items-center min-w-[120px] backdrop-blur-sm shadow-xl hover:bg-[#F27D26]/40 transition-colors text-[#F27D26] cursor-pointer"
        >
          <span class="text-[9px] uppercase tracking-[0.1em] mb-1 font-bold">Prod Sync</span>
          <span class="text-lg font-mono font-bold">{{ isSyncing ? 'SYNCING...' : 'SYNC' }}</span>
        </button>
        <div
          v-for="(val, label) in stats"
          :key="label"
          class="bg-white/[0.02] border border-white/5 px-5 py-3 rounded-lg flex flex-col items-center min-w-[120px] backdrop-blur-sm shadow-xl"
        >
          <span class="text-[9px] text-white/30 uppercase tracking-[0.1em] mb-1 font-bold">{{ label.replace(/([A-Z])/g, ' $1') }}</span>
          <span class="text-xl font-mono font-bold text-white/90">{{ val }}</span>
        </div>
      </div>
    </div>

    <!-- MAIN VIEWPORT -->
    <div class="relative min-h-[600px]">
      <!-- DASHBOARD -->
      <div
        v-if="viewMode === 'dashboard'"
        class="space-y-8 animate-in fade-in duration-500"
      >
        <!-- Controls -->
        <div class="flex flex-col md:flex-row gap-4 items-center justify-between">
          <div class="relative w-full md:w-96 group">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/20 group-focus-within:text-[#F27D26] transition-colors" />
            <input 
              v-model="searchQuery"
              type="text" 
              placeholder="Search 100 modules (e.g. 'Rust', 'Gemini', 'SQL')..."
              class="w-full bg-white/5 border border-white/10 rounded-full py-2.5 pl-10 pr-4 text-sm focus:outline-none focus:border-[#F27D26]/50 focus:ring-1 focus:ring-[#F27D26]/20 transition-all font-mono"
            >
          </div>

          <div class="flex gap-2">
            <button 
              v-for="view in (['atlas', 'list'] as const)" 
              :key="view"
              :class="[
                'px-4 py-2 rounded-lg text-[10px] font-bold uppercase tracking-widest flex items-center gap-2 transition-all border shadow-lg',
                activeView === view ? 'bg-[#F27D26] text-black border-[#F27D26]' : 'bg-white/5 text-white/40 border-white/5 hover:bg-white/10'
              ]"
              @click="activeView = view"
            >
              <Grid3X3
                v-if="view === 'atlas'"
                class="w-3.5 h-3.5"
              />
              <ListIcon
                v-else
                class="w-3.5 h-3.5"
              />
              {{ view }}
            </button>
          </div>
        </div>

        <!-- ATLAS View -->
        <div
          v-if="activeView === 'atlas'"
          class="space-y-12 pb-20"
        >
          <!-- Spotlight: Trending/Latest -->
          <div 
            v-if="latestModules.length > 0"
            class="space-y-4"
          >
            <div class="flex items-center justify-between">
              <h3 class="text-xs font-bold text-[#F27D26] uppercase tracking-[0.2em] flex items-center gap-2">
                <Sparkles class="w-3 h-3 fill-[#F27D26]" />
                New & Featured Modules
              </h3>
              <div class="flex items-center gap-2 text-[10px] text-white/20 font-mono">
                <TrendingUp class="w-3 h-3" />
                Latest Curriculum Drops
              </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 tracking-tight">
              <div 
                v-for="mod in latestModules" 
                :key="mod.id"
                class="group relative bg-[#111] border border-white/5 rounded-2xl p-5 hover:border-[#F27D26]/40 transition-all cursor-pointer overflow-hidden shadow-2xl"
                @click="startModule(mod)"
              >
                <!-- Glow Effect -->
                <div class="absolute -inset-1 bg-gradient-to-r from-[#F27D26] to-[#ff8f3e] rounded-2xl opacity-0 group-hover:opacity-10 blur-xl transition-opacity animate-pulse" />
                
                <div class="relative z-10 space-y-3">
                  <div class="flex justify-between items-start">
                    <div class="text-[10px] font-mono text-[#F27D26] font-bold bg-[#F27D26]/10 px-2 py-0.5 rounded border border-[#F27D26]/20">
                      {{ mod.id }}
                    </div>
                    <Zap class="w-3.5 h-3.5 text-[#F27D26] opacity-0 group-hover:opacity-100 transition-opacity" />
                  </div>
                  <h4 class="text-base font-bold text-white group-hover:text-[#F27D26] transition-colors line-clamp-2 leading-tight">
                    {{ mod.name }}
                  </h4>
                  <div class="flex items-center gap-1.5 text-[9px] font-black text-white/30 uppercase tracking-widest pt-2 border-t border-white/5 group-hover:text-[#F27D26]/60">
                    Execute Training <ChevronRight class="w-3 h-3" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Roadmap -->
          <div class="space-y-4">
            <h3 class="text-xs font-bold text-white/40 uppercase tracking-widest">
              Full Curriculum Roadmap
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              <div 
                v-for="phase in filteredPhases" 
                :key="phase.id"
                class="space-y-3 group"
              >
                <div 
                  class="flex flex-col p-4 bg-white/[0.03] border border-white/5 rounded-xl hover:border-[#F27D26]/40 transition-all cursor-pointer relative overflow-hidden shadow-2xl"
                  @click="openPhase(phase)"
                >
                  <div class="flex justify-between items-center mb-4">
                    <span class="text-[9px] font-mono text-[#F27D26] font-bold">{{ phase.id }}</span>
                    <span class="text-[9px] font-mono text-white/20">{{ phase.hours[0] }}-{{ phase.hours[1] }}H</span>
                  </div>
                  <h3 class="text-sm font-bold text-white/80 group-hover:text-white mb-2 leading-tight h-8 line-clamp-2 uppercase tracking-tight">
                    {{ phase.name }}
                  </h3>
                  
                  <div class="grid grid-cols-5 gap-1 pt-2">
                    <div 
                      v-for="mod in phase.modules" 
                      :key="mod.id"
                      class="aspect-square rounded-sm border border-white/5 transition-all duration-500"
                      :class="allSlidesData[mod.id] ? 'bg-[#F27D26] shadow-[0_0_12px_rgba(242,125,38,0.4)]' : 'bg-white/5'"
                      :title="mod.name"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- LIST View -->
        <div
          v-else
          class="space-y-4 pb-20"
        >
          <div
            v-for="phase in filteredPhases"
            :key="phase.id"
            class="border border-white/5 rounded-xl overflow-hidden bg-white/[0.01]"
          >
            <div
              class="p-6 bg-white/5 flex justify-between items-center cursor-pointer hover:bg-white/10 transition-colors"
              @click="openPhase(phase)"
            >
              <div class="flex items-center gap-4">
                <span class="text-xs font-mono text-[#F27D26] font-extrabold">{{ phase.id }}</span>
                <h3 class="font-bold text-base tracking-tight uppercase">
                  {{ phase.name }}
                </h3>
              </div>
              <span class="text-[10px] text-white/20 font-mono tracking-widest">{{ phase.modules.length }} MODULES</span>
            </div>
            <div class="p-3 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-2 bg-black/20">
              <button 
                v-for="mod in phase.modules" 
                :key="mod.id"
                class="p-3 text-[10px] text-left rounded-lg transition-all flex flex-col items-start gap-1 group border border-transparent shadow-inner"
                :disabled="!allSlidesData[mod.id]"
                :class="allSlidesData[mod.id] ? 'bg-white/5 hover:bg-[#F27D26]/10 hover:border-[#F27D26]/20 text-white/90' : 'opacity-20 cursor-not-allowed'"
                @click="allSlidesData[mod.id] ? startModule(mod) : null"
              >
                <div class="flex justify-between w-full opacity-40 group-hover:opacity-100 transition-opacity">
                  <span class="font-mono text-[8px]">{{ mod.id }}</span>
                  <Zap
                    v-if="allSlidesData[mod.id]"
                    class="w-2.5 h-2.5 text-[#F27D26] fill-[#F27D26]"
                  />
                </div>
                <span class="line-clamp-1 font-bold group-hover:text-[#F27D26] transition-colors uppercase tracking-tighter">{{ mod.name }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- PHASE VIEW -->
      <div
        v-else-if="viewMode === 'phase'"
        class="space-y-8 animate-in slide-in-from-bottom-8 duration-500 pb-20"
      >
        <div class="relative bg-gradient-to-br from-[#F27D26]/10 via-[#F27D26]/5 to-transparent border border-[#F27D26]/20 p-10 rounded-3xl overflow-hidden shadow-2xl">
          <div class="absolute top-0 right-0 p-12 opacity-5 scale-150 rotate-12 pointer-events-none">
            <Database class="w-64 h-64 text-[#F27D26]" />
          </div>
          <div class="relative z-10 space-y-6">
            <div class="inline-flex items-center gap-2 px-4 py-1.5 bg-[#F27D26] text-black rounded-full text-[10px] font-black uppercase tracking-[0.2em] shadow-lg">
              <Clock class="w-3.5 h-3.5" />
              {{ selectedPhase?.hours[1]! - selectedPhase?.hours[0]! + 1 }} Hours of Deep Flow
            </div>
            <h1 class="text-5xl font-serif italic text-white max-w-2xl drop-shadow-lg">
              {{ selectedPhase?.name }}
            </h1>
            <p class="text-white/50 max-w-xl text-sm leading-relaxed">
              Master the core architectural concepts of this phase through 10 focused modules. This section of the 🍓 FRAISE curriculum transitions you closer to agentic autonomy.
            </p>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div 
            v-for="(mod, idx) in selectedPhase?.modules" 
            :key="mod.id"
            class="relative group h-full"
            @click="allSlidesData[mod.id] ? startModule(mod) : null"
          >
            <div 
              class="h-full bg-white/[0.02] border border-white/5 p-8 rounded-2xl transition-all duration-500 flex flex-col shadow-xl"
              :class="allSlidesData[mod.id] ? 'hover:border-[#F27D26]/40 hover:bg-white/[0.05] cursor-pointer' : 'opacity-40 grayscale pointer-events-none'"
            >
              <div class="flex justify-between items-start mb-8">
                <div class="w-12 h-12 rounded-2xl bg-white/5 flex items-center justify-center text-sm font-mono font-black text-[#F27D26] border border-white/10 shadow-inner group-hover:scale-110 transition-transform">
                  {{ String(idx + 1).padStart(2, '0') }}
                </div>
                <div class="text-[9px] font-mono text-white/20 bg-white/5 px-3 py-1 rounded-full border border-white/5 uppercase tracking-widest">
                  Ref: {{ mod.id }}
                </div>
              </div>

              <h3 class="text-xl font-bold mb-4 leading-tight text-white group-hover:text-[#F27D26] transition-colors">
                {{ mod.name }}
              </h3>
              <p class="text-xs text-white/40 mb-10 leading-relaxed flex-1">
                Engineering deep-dive covering advanced patterns in this domain over a {{ mod.hours[1] - mod.hours[0] + 1 }} hour focused window.
              </p>

              <div class="flex items-center justify-between pt-6 border-t border-white/5">
                <span
                  class="text-[10px] font-black uppercase tracking-widest transition-colors duration-300"
                  :class="allSlidesData[mod.id] ? 'text-[#F27D26]' : 'text-white/20'"
                >
                  {{ allSlidesData[mod.id] ? 'Launch Training' : 'In Backlog' }}
                </span>
                <ChevronRight
                  v-if="allSlidesData[mod.id]"
                  class="w-5 h-5 text-[#F27D26] translate-x-0 group-hover:translate-x-2 transition-transform"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- READER OVERLAY -->
      <div
        v-if="viewMode === 'reader'"
        class="fixed inset-0 z-[100] bg-black flex flex-col pt-[70px] animate-in fade-in duration-500"
      >
        <div class="absolute top-0 left-0 right-0 h-[70px] border-b border-white/10 flex items-center justify-between px-8 bg-black/90 backdrop-blur-xl">
          <div class="flex items-center gap-6">
            <button
              class="p-3 hover:bg-white/5 rounded-full transition-all text-white/60 hover:text-white border border-transparent hover:border-white/10 shadow-lg group"
              @click="closeReader"
            >
              <X class="w-5 h-5 group-hover:rotate-90 transition-transform" />
            </button>
            <div class="h-8 w-[1px] bg-white/10" />
            <div class="flex flex-col">
              <div class="text-[10px] text-[#F27D26] font-black uppercase tracking-[0.3em] leading-none mb-2">
                {{ selectedModule?.id }} • {{ selectedPhase?.name }}
              </div>
              <h4 class="text-lg font-bold leading-none text-white/90 uppercase tracking-tight">
                {{ selectedModule?.name }}
              </h4>
            </div>
          </div>

          <div class="flex items-center gap-8">
            <div class="flex flex-col items-end gap-1">
              <div class="text-[9px] font-mono text-white/40 uppercase tracking-widest">
                Progress
              </div>
              <div class="text-xs font-mono text-[#F27D26] font-bold">
                Slide {{ currentSlideIndex + 1 }} of {{ currentBundle?.slides.length }}
              </div>
            </div>
            <div class="w-48 h-1.5 bg-white/5 rounded-full overflow-hidden border border-white/10 group shadow-inner">
              <div
                class="h-full bg-[#F27D26] transition-all duration-500 shadow-[0_0_15px_rgba(242,125,38,0.5)]"
                :style="{ width: progress + '%' }"
              />
            </div>
          </div>
        </div>

        <div class="flex-1 overflow-auto bg-[#050505] relative">
          <div class="max-w-5xl mx-auto py-20 px-8">
            <div
              v-if="currentSlide"
              :key="currentSlideIndex"
              class="space-y-12 animate-in slide-in-from-bottom-8 duration-500"
            >
              <h1 class="text-5xl md:text-7xl font-serif italic text-white drop-shadow-2xl leading-tight">
                {{ currentSlide.title }}
              </h1>
              
              <div class="grid grid-cols-1 lg:grid-cols-4 gap-16">
                <div class="lg:col-span-3 space-y-10">
                  <div 
                    class="prose prose-invert max-w-none text-xl text-white/70 leading-relaxed font-light first-letter:text-5xl first-letter:font-serif first-letter:text-[#F27D26] first-letter:mr-3 first-letter:float-left whitespace-pre-line"
                    v-html="currentSlide.content"
                  />

                  <div v-if="currentSlide.svgContent" v-html="currentSlide.svgContent" class="my-8"></div>

                  <div v-if="currentSlide.studentPrompts?.length" class="mt-8 space-y-4">
                    <h5 class="text-xs font-black text-white/40 uppercase tracking-[0.3em]">Student Prompts</h5>
                    <ul class="list-disc list-inside text-white/60 space-y-2">
                      <li v-for="(prompt, index) in currentSlide.studentPrompts" :key="index">{{ prompt }}</li>
                    </ul>
                  </div>

                  <div v-if="currentSlide.links?.length" class="mt-8 space-y-4">
                    <h5 class="text-xs font-black text-white/40 uppercase tracking-[0.3em]">Links</h5>
                    <ul class="space-y-2">
                      <li v-for="(link, index) in currentSlide.links" :key="index">
                        <a :href="link" target="_blank" class="text-[#F27D26] hover:underline text-sm">{{ link }}</a>
                      </li>
                    </ul>
                  </div>

                  <div
                    v-if="currentSlide.codeReferences.length > 0"
                    class="space-y-6 pt-12 border-t border-white/10"
                  >
                    <h5 class="text-xs font-black text-white/40 uppercase tracking-[0.3em] flex items-center gap-3">
                      <Code class="w-5 h-5 text-[#F27D26]" />
                      Code Grounding Matrix
                    </h5>
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div 
                        v-for="refPath in currentSlide.codeReferences" 
                        :key="refPath"
                        class="p-4 bg-white/[0.02] border border-white/5 rounded-xl text-[11px] font-mono flex items-center justify-between group hover:border-[#F27D26]/40 hover:bg-white/[0.05] transition-all cursor-help shadow-lg"
                      >
                        <span class="text-white/60 group-hover:text-white transition-colors truncate shrink">{{ refPath }}</span>
                        <ExternalLink class="w-3.5 h-3.5 text-[#F27D26] opacity-30 group-hover:opacity-100 transition-opacity" />
                      </div>
                    </div>
                  </div>
                </div>

                <div class="space-y-8">
                  <div class="bg-white/5 border border-white/10 p-8 rounded-3xl shadow-2xl">
                    <h5 class="text-[10px] font-black text-white/30 uppercase tracking-[0.2em] mb-6 flex items-center gap-2">
                      <Filter class="w-4 h-4 text-[#F27D26]" />
                      Taxonomy
                    </h5>
                    <div class="flex flex-wrap gap-2">
                      <span 
                        v-for="tag in currentSlide.tags" 
                        :key="tag"
                        class="px-3 py-1.5 bg-[#F27D26]/10 text-[#F27D26] border border-[#F27D26]/20 rounded-lg text-[9px] font-black uppercase tracking-tighter shadow-inner"
                      >
                        {{ tag }}
                      </span>
                    </div>
                  </div>

                  <div class="p-8 border border-white/5 rounded-3xl bg-gradient-to-br from-white/5 to-transparent shadow-xl">
                    <div class="text-[10px] font-black text-white/30 uppercase tracking-[0.2em] mb-4">
                      Architectural Tier
                    </div>
                    <div class="flex gap-1.5 h-1.5 mb-2">
                      <div 
                        v-for="i in 10" 
                        :key="i" 
                        class="flex-1 rounded-full transition-all duration-1000 shadow-[0_0_8px_rgba(242,125,38,0.2)]" 
                        :class="i <= (parseInt(selectedPhase?.id.replace('P', '') || '1')) ? 'bg-[#F27D26]' : 'bg-white/10'"
                      />
                    </div>
                    <div class="flex justify-between items-center text-[8px] font-black text-white/10 uppercase tracking-widest">
                      <span>Foundation</span>
                      <span>Enterprise</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="h-[100px] bg-black/90 backdrop-blur-xl border-t border-white/10 flex items-center justify-center gap-8 px-8">
          <button 
            :disabled="currentSlideIndex === 0" 
            class="group flex items-center gap-4 px-8 py-4 rounded-2xl border border-white/10 hover:bg-white/5 hover:border-white/20 disabled:opacity-10 disabled:cursor-not-allowed transition-all shadow-xl"
            @click="prevSlide"
          >
            <ChevronLeft class="w-6 h-6 group-hover:-translate-x-1 transition-transform" />
            <span class="text-xs font-black uppercase tracking-[0.2em]">Previous</span>
          </button>

          <button 
            :disabled="currentSlideIndex === (currentBundle?.slides.length || 0) - 1" 
            class="group flex items-center gap-6 px-16 py-4 bg-[#F27D26] text-black rounded-2xl hover:bg-[#ff8f3e] disabled:opacity-20 disabled:cursor-not-allowed transition-all shadow-[0_0_30px_rgba(242,125,38,0.3)] hover:shadow-[0_0_40px_rgba(242,125,38,0.5)]"
            @click="nextSlide"
          >
            <span class="text-xs font-black uppercase tracking-[0.2em]">Continue</span>
            <ChevronRight class="w-6 h-6 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:deep(svg) {
  max-width: 400px;
  height: auto;
  margin: 2rem 0;
  display: block;
}
</style>
