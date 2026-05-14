<script setup lang="ts">
import { ref, computed } from 'vue';
import { BookOpen, Search, Layers, Zap, Code, ShieldCheck, ChevronLeft, ChevronRight, X, ExternalLink, Filter } from 'lucide-vue-next';

import glossary1Url from '../../docs/training/slides/glossary/technical-terms-1.json?url';
import glossary2Url from '../../docs/training/slides/glossary/technical-terms-2.json?url';
import moduleIntroUrl from '../../docs/training/slides/phase1/module-intro.json?url';
import agileAiRolesUrl from '../../docs/training/slides/phase5/agile-ai-roles.json?url';
import deploymentCoolifyUrl from '../../docs/training/slides/phase5/deployment-coolify.json?url';

interface Slide {
  id: number;
  title: string;
  content: string;
  tags: string[];
  codeReferences: string[];
}

interface SlideBundle {
  moduleId: string;
  title: string;
  totalSlides: number;
  bundleId: number;
  slides: Slide[];
}

interface TrainingPhase {
  id: string;
  title: string;
  duration: string;
  slides: number;
  description: string;
  repos: string[];
}

interface ModuleMeta {
  id: string;
  title: string;
  phaseId: string;
  path: string;
  description: string;
}

const MODULES: ModuleMeta[] = [
  {
    id: 'glossary-1',
    title: 'Technical Glossary Vol 1',
    phaseId: '1',
    path: glossary1Url,
    description: 'Core concepts of PHP, MVC, and web infrastructure.'
  },
  {
    id: 'glossary-2',
    title: 'Technical Glossary Vol 2',
    phaseId: '1',
    path: glossary2Url,
    description: 'Advanced patterns and multi-tenant logic.'
  },
  {
    id: 'module-intro',
    title: 'Curriculum Introduction',
    phaseId: '1',
    path: moduleIntroUrl,
    description: 'Overview of the 1000-hour mastery path.'
  },
  {
    id: 'agile-ai',
    title: 'Agile AI Agent Roles',
    phaseId: '5',
    path: agileAiRolesUrl,
    description: 'Integrating humans and agents in the SDLC.'
  },
  {
    id: 'coolify',
    title: 'Deployment & Scaling',
    phaseId: '5',
    path: deploymentCoolifyUrl,
    description: 'Using Coolify for self-hosted cloud scaling.'
  }
];

const phases = ref<TrainingPhase[]>([
  { id: '1', title: 'Web Fundamentals', duration: '175h', slides: 10500, description: 'HTML5, CSS3, Vanilla JS, and PHP core patterns.', repos: ['repo-php'] },
  { id: '2', title: 'Modern Frontend', duration: '225h', slides: 13500, description: 'Reactive architecture with Vue 3 and React hooks.', repos: ['repo-react', 'repo-vue'] },
  { id: '3', title: 'Backend Systems', duration: '275h', slides: 16500, description: 'Compiled vs Dynamic languages: Go, Rust, Python.', repos: ['repo-go', 'repo-rust', 'repo-python'] },
  { id: '4', title: 'Multi-Platform', duration: '175h', slides: 10500, description: 'Mobile development with Flutter and Kotlin.', repos: ['repo-android', 'repo-flutter'] },
  { id: '5', title: 'DevOps & AI Agents', duration: '150h', slides: 9000, description: 'Containerization, Nginx, and agentic workflows.', repos: ['docs/ai-agents'] },
]);

const stats = {
  totalHours: 1000,
  totalSlides: 60000,
  totalData: "60MB",
  density: "1 slide / min"
};

// State for navigation
const selectedPhase = ref<TrainingPhase | null>(null);
const currentBundle = ref<SlideBundle | null>(null);
const currentSlideIndex = ref(0);
const isLoading = ref(false);
const error = ref<string | null>(null);
const viewMode = ref<'dashboard' | 'modules' | 'reader'>('dashboard');

const currentSlide = computed(() => {
  if (!currentBundle.value || !currentBundle.value.slides.length) return null;
  return currentBundle.value.slides[currentSlideIndex.value];
});

const progress = computed(() => {
  if (!currentBundle.value) return 0;
  return ((currentSlideIndex.value + 1) / currentBundle.value.slides.length) * 100;
});

const phaseModules = computed(() => {
  if (!selectedPhase.value) return [];
  return MODULES.filter(m => m.phaseId === selectedPhase.value?.id);
});

async function openPhase(phase: TrainingPhase) {
  selectedPhase.value = phase;
  viewMode.value = 'modules';
}

async function loadModule(moduleMeta: ModuleMeta) {
  isLoading.value = true;
  error.value = null;
  currentSlideIndex.value = 0;
  
  try {
    const response = await fetch(moduleMeta.path);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const data = await response.json();
    currentBundle.value = data;
    viewMode.value = 'reader';
  } catch (err) {
    console.error('Failed to load slides:', err);
    error.value = 'Could not load training content. This might be because the file is not served by Vite directly. Attempting alternative access...';
    
    // In a real scenario, we might retry or show a specific error
    // For this simulation, we'll gracefully handle it if the path is wrong
  } finally {
    isLoading.value = false;
  }
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
  viewMode.value = 'modules';
}

function backToDashboard() {
  viewMode.value = 'dashboard';
  selectedPhase.value = null;
}
</script>

<template>
  <div class="space-y-8 animate-in fade-in duration-700">
    <!-- Header -->
    <div class="flex flex-col md:flex-row justify-between items-end gap-4 border-b border-white/10 pb-6">
      <div class="space-y-1">
        <div class="flex items-center gap-2 text-[#F27D26] text-xs font-bold tracking-widest uppercase">
          <BookOpen class="w-4 h-4" />
          Training Architecture
        </div>
        <h2 class="text-3xl font-serif italic italic text-white">Knowledge Transmission Center</h2>
        <p class="text-sm text-white/50 max-w-2xl">
          Managing 60MB of training logic across 60,000 architectural slides.
          This interface demonstrates how AI Studio optimizes polyglot stack transitions.
        </p>
      </div>

      <div class="flex gap-4">
        <div v-for="(val, label) in stats" :key="label" class="bg-white/5 px-4 py-2 rounded border border-white/10 flex flex-col items-center min-w-[100px]">
          <span class="text-[10px] text-white/40 uppercase tracking-tighter">{{ label }}</span>
          <span class="text-lg font-mono font-bold">{{ val }}</span>
        </div>
      </div>
    </div>

    <!-- Dashboard View -->
    <div v-if="viewMode === 'dashboard'" class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      
      <!-- Left side: The Phases -->
      <div class="lg:col-span-2 space-y-4">
        <div class="text-xs font-bold text-white/40 uppercase tracking-widest mb-2 flex justify-between">
          <span>Curriculum Modules</span>
          <span>Progressive Disclosure Pattern</span>
        </div>
        
        <div 
          v-for="phase in phases" 
          :key="phase.id"
          @click="openPhase(phase)"
          class="bg-[#181818] border border-white/5 p-5 rounded-lg hover:border-[#F27D26]/30 transition-all group relative overflow-hidden cursor-pointer"
        >
          <div class="absolute top-0 right-0 p-4 opacity-5 group-hover:opacity-10 transition-opacity text-[#F27D26]">
             <BookOpen class="w-24 h-24" />
          </div>
          
          <div class="flex justify-between items-start mb-3">
            <div>
              <div class="text-[10px] text-[#F27D26] font-bold mb-1">PHASE 0{{ phase.id }}</div>
              <h3 class="text-xl font-bold group-hover:text-white transition-colors">{{ phase.title }}</h3>
            </div>
            <div class="text-right">
              <div class="text-xs font-mono text-white/60">{{ phase.duration }}</div>
              <div class="text-[10px] text-white/30">{{ phase.slides.toLocaleString() }} slides</div>
            </div>
          </div>
          
          <p class="text-sm text-white/60 mb-6 max-w-md">{{ phase.description }}</p>
          
          <div class="flex justify-between items-center">
            <div class="flex flex-wrap gap-2">
              <span v-for="repo in phase.repos" :key="repo" class="text-[9px] px-2 py-1 bg-white/5 border border-white/10 rounded flex items-center gap-1">
                <Code class="w-3 h-3 text-white/40" />
                {{ repo }}
              </span>
            </div>
            <button class="text-[10px] font-bold text-[#F27D26] uppercase tracking-widest opacity-0 group-hover:opacity-100 transition-opacity">
              Explore Content
            </button>
          </div>
        </div>
      </div>

      <!-- Right side: AI Agent Optimization Strategy -->
      <div class="space-y-6">
        <div class="bg-[#1c1c1c] border border-white/10 rounded-xl p-6 relative">
          <div class="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-[#F27D26] flex items-center justify-center">
            <Zap class="w-4 h-4 text-black" />
          </div>
          <h4 class="text-lg font-bold mb-4">Agentic Optimization</h4>
          <ul class="space-y-4">
            <li class="flex gap-3">
              <div class="mt-1"><Layers class="w-4 h-4 text-[#F27D26]" /></div>
              <div>
                <p class="text-sm font-bold">Bundle Ingestion</p>
                <p class="text-xs text-white/50">Slides are bundled in 1MB JSON files to prevent context fragmentation.</p>
              </div>
            </li>
            <li class="flex gap-3">
              <div class="mt-1"><Search class="w-4 h-4 text-[#F27D26]" /></div>
              <div>
                <p class="text-sm font-bold">Vector Mapping</p>
                <p class="text-xs text-white/50">Metadata indexes allow agents to perform selective retrieval instead of full-scans.</p>
              </div>
            </li>
            <li class="flex gap-3">
              <div class="mt-1"><ShieldCheck class="w-4 h-4 text-[#F27D26]" /></div>
              <div>
                <p class="text-sm font-bold">Code Grounding</p>
                <p class="text-xs text-white/50">Every training slide is linked to at least one physical file in the multi-repo stack.</p>
              </div>
            </li>
          </ul>
          
          <button class="w-full mt-8 py-3 bg-[#F27D26] text-black font-bold text-xs uppercase tracking-widest rounded hover:bg-[#ff8f3e] transition-colors">
            Generate Training Index
          </button>
        </div>

        <div class="border border-white/5 p-6 rounded-xl bg-gradient-to-br from-white/5 to-transparent">
          <h4 class="text-xs font-bold uppercase tracking-widest text-[#F27D26] mb-3">AI Coder Agent Status</h4>
          <div class="space-y-2">
            <div class="flex justify-between text-xs">
              <span class="opacity-50">Stack Recognition</span>
              <span class="text-green-400">READY</span>
            </div>
            <div class="w-full h-1 bg-white/10 rounded-full overflow-hidden">
              <div class="w-full h-full bg-green-500"></div>
            </div>
          </div>
          <div class="space-y-2 mt-4">
            <div class="flex justify-between text-xs">
              <span class="opacity-50">Slide Ingestion Speed</span>
              <span class="text-[#F27D26]">OPTIMIZED (via JSON)</span>
            </div>
            <div class="w-full h-1 bg-white/10 rounded-full overflow-hidden">
              <div class="w-[85%] h-full bg-[#F27D26]"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Module Selection View -->
    <div v-else-if="viewMode === 'modules'" class="space-y-6">
      <button @click="backToDashboard" class="flex items-center gap-2 text-xs text-white/50 hover:text-white transition-colors">
        <ChevronLeft class="w-4 h-4" />
        Back to Dashboard
      </button>

      <div class="bg-white/5 border border-white/10 rounded-xl p-8">
        <div class="flex justify-between items-start mb-8">
          <div>
            <div class="text-[10px] text-[#F27D26] font-bold mb-1 uppercase tracking-widest">Phase 0{{ selectedPhase?.id }}</div>
            <h3 class="text-3xl font-bold">{{ selectedPhase?.title }}</h3>
            <p class="text-white/50 mt-2">{{ selectedPhase?.description }}</p>
          </div>
        </div>

        <div v-if="phaseModules.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div 
            v-for="module in phaseModules" 
            :key="module.id"
            @click="loadModule(module)"
            class="bg-black/40 border border-white/5 p-6 rounded-lg hover:border-[#F27D26]/50 transition-all cursor-pointer group"
          >
            <div class="flex justify-between items-start mb-4">
              <div class="p-3 bg-[#F27D26]/10 rounded shadow-inner">
                <Layers class="w-6 h-6 text-[#F27D26]" />
              </div>
              <div class="text-[10px] text-white/20 font-mono">MODULE ID: {{ module.id.toUpperCase() }}</div>
            </div>
            <h4 class="text-lg font-bold mb-2 group-hover:text-[#F27D26] transition-colors">{{ module.title }}</h4>
            <p class="text-sm text-white/40 mb-6">{{ module.description }}</p>
            <div class="flex items-center gap-2 text-xs font-bold text-[#F27D26] uppercase tracking-tighter">
              Start Learning
              <ChevronRight class="w-3 h-3 translate-x-0 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>
        </div>
        <div v-else class="text-center py-20 bg-black/20 rounded border border-dashed border-white/10">
          <BookOpen class="w-12 h-12 text-white/10 mx-auto mb-4" />
          <p class="text-white/40">No interactive modules available for this phase yet.</p>
          <p class="text-[10px] text-white/20 mt-1 uppercase">Indexing in progress...</p>
        </div>
      </div>
    </div>

    <!-- Slide Reader View -->
    <div v-else-if="viewMode === 'reader'" class="fixed inset-0 z-50 bg-black flex flex-col pt-[60px]">
      <!-- Reader Header -->
      <div class="absolute top-0 left-0 right-0 h-[60px] border-b border-white/10 flex items-center justify-between px-6 bg-black/90 backdrop-blur">
        <div class="flex items-center gap-4">
          <button @click="closeReader" class="p-2 hover:bg-white/5 rounded transition-colors text-white/60">
            <X class="w-5 h-5" />
          </button>
          <div class="h-6 w-[1px] bg-white/10"></div>
          <div>
            <div class="text-[10px] text-[#F27D26] font-bold uppercase tracking-widest leading-none mb-1">
              {{ currentBundle?.moduleId }} • Bundle {{ currentBundle?.bundleId }}
            </div>
            <h4 class="text-sm font-bold leading-none">{{ currentBundle?.title }}</h4>
          </div>
        </div>

        <div class="flex items-center gap-4">
           <div class="text-xs font-mono text-white/40">
             Slide {{ currentSlideIndex + 1 }} / {{ currentBundle?.slides.length }}
           </div>
           <div class="w-32 h-1 bg-white/10 rounded-full overflow-hidden">
             <div class="h-full bg-[#F27D26] transition-all duration-300" :style="{ width: progress + '%' }"></div>
           </div>
        </div>
      </div>

      <!-- Reader Body -->
      <div class="flex-1 overflow-auto bg-[#0a0a0a]">
        <div class="max-w-4xl mx-auto py-12 px-6">
          <div v-if="currentSlide" class="space-y-8 animate-in slide-in-from-bottom-4 duration-500">
            <h1 class="text-4xl md:text-5xl font-serif italic text-white">{{ currentSlide.title }}</h1>
            
            <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div class="md:col-span-2 space-y-6">
                <div class="prose prose-invert max-w-none text-lg text-white/80 leading-relaxed">
                  {{ currentSlide.content }}
                </div>

                <div v-if="currentSlide.codeReferences.length > 0" class="space-y-3 pt-6 border-t border-white/10">
                  <h5 class="text-xs font-bold text-white/40 uppercase tracking-widest flex items-center gap-2">
                    <Code class="w-4 h-4" />
                    Code Grounding
                  </h5>
                  <div class="flex flex-wrap gap-2">
                    <div 
                      v-for="refPath in currentSlide.codeReferences" 
                      :key="refPath"
                      class="px-3 py-2 bg-black border border-white/10 rounded text-[11px] font-mono flex items-center gap-2 hover:border-[#F27D26]/50 transition-colors cursor-help"
                    >
                      <span class="text-white/70">{{ refPath }}</span>
                      <ExternalLink class="w-3 h-3 text-white/20" />
                    </div>
                  </div>
                </div>
              </div>

              <div class="space-y-6">
                <div class="bg-white/5 border border-white/10 p-6 rounded-lg">
                  <h5 class="text-xs font-bold text-white/40 uppercase tracking-widest mb-4 flex items-center gap-2">
                    <Filter class="w-4 h-4" />
                    Taxonomy
                  </h5>
                  <div class="flex flex-wrap gap-2">
                    <span 
                      v-for="tag in currentSlide.tags" 
                      :key="tag"
                      class="px-2 py-1 bg-[#F27D26]/10 text-[#F27D26] border border-[#F27D26]/20 rounded text-[10px] font-bold uppercase"
                    >
                      {{ tag }}
                    </span>
                  </div>
                </div>

                <div class="p-6 border border-white/5 rounded-lg bg-gradient-to-br from-white/5 to-transparent">
                  <div class="text-[10px] text-white/30 uppercase mb-2">Architectural Level</div>
                  <div class="flex gap-1">
                    <div v-for="i in 5" :key="i" class="flex-1 h-1 rounded-full" :class="i <= (parseInt(selectedPhase?.id || '1')) ? 'bg-[#F27D26]' : 'bg-white/10'"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Reader Navigation -->
      <div class="h-[80px] bg-black border-t border-white/10 flex items-center justify-center gap-4 px-6">
        <button 
          @click="prevSlide" 
          :disabled="currentSlideIndex === 0"
          class="flex items-center gap-2 px-6 py-3 rounded border border-white/10 hover:bg-white/5 disabled:opacity-20 disabled:cursor-not-allowed transition-all"
        >
          <ChevronLeft class="w-5 h-5" />
          <span class="text-xs font-bold uppercase tracking-widest">Previous Slide</span>
        </button>

        <button 
          @click="nextSlide" 
          :disabled="currentSlideIndex === (currentBundle?.slides.length || 0) - 1"
          class="flex items-center gap-2 px-12 py-3 bg-[#F27D26] text-black rounded hover:bg-[#ff8f3e] disabled:opacity-20 disabled:cursor-not-allowed transition-all"
        >
          <span class="text-xs font-bold uppercase tracking-widest">Next Slide</span>
          <ChevronRight class="w-5 h-5" />
        </button>
      </div>
    </div>
  </div>
</template>
