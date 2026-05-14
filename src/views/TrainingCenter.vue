<script setup lang="ts">
import { ref } from 'vue';
import { BookOpen, Search, Layers, Zap, Code, ShieldCheck } from 'lucide-vue-next';

interface TrainingPhase {
  id: string;
  title: string;
  duration: string;
  slides: number;
  description: string;
  repos: string[];
}

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

    <!-- Hierarchy Proposal -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      
      <!-- Left side: The Phases -->
      <div class="lg:col-span-2 space-y-4">
        <div class="text-xs font-bold text-white/40 uppercase tracking-widest mb-2 flex justify-between">
          <span>Curriculum Modules</span>
          <span>Progressive Disclosure Pattern</span>
        </div>
        
        <div 
          v-for="phase in phases" 
          :key="phase.id"
          class="bg-[#181818] border border-white/5 p-5 rounded-lg hover:border-[#F27D26]/30 transition-all group relative overflow-hidden"
        >
          <div class="absolute top-0 right-0 p-4 opacity-5 group-hover:opacity-10 transition-opacity">
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
          
          <div class="flex flex-wrap gap-2">
            <span v-for="repo in phase.repos" :key="repo" class="text-[9px] px-2 py-1 bg-white/5 border border-white/10 rounded flex items-center gap-1">
              <Code class="w-3 h-3 text-white/40" />
              {{ repo }}
            </span>
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
  </div>
</template>
