<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { 
  FileBox as BoxIcon, 
  RefreshCw as RefreshIcon, 
  CheckCircle as CheckIcon, 
  AlertTriangle as ErrorIcon, 
  Download as DownloadIcon, 
  Search as SearchIcon, 
  Cpu as CpuIcon, 
  FileText as FileIcon, 
  Eye as ViewIcon,
  HelpCircle as HelpIcon,
  Compass as CompassIcon,
  ShieldCheck as ShieldCheckIcon
} from 'lucide-vue-next';

interface GlbFile {
  name: string;
  relPath: string;
  actualSize: number;
  modifiedAt: string;
  magicMatches: boolean;
  version: number;
  declaredLength: number;
  declaredSizeMatchesActual: boolean;
  isValidGLB: boolean;
  errorMessage: string;
  headerHex: string;
}

const glbFiles = ref<GlbFile[]>([]);
const selectedFile = ref<GlbFile | null>(null);
const loading = ref(false);
const searchQuery = ref('');
const statusFilter = ref<'all' | 'valid' | 'corrupted'>('all');
const scanError = ref('');

// Advanced controls for <model-viewer>
const autoRotate = ref(true);
const cameraControls = ref(true);
const shadowIntensity = ref('1');

const fetchGlbFiles = async () => {
  loading.value = true;
  scanError.value = '';
  try {
    const passkey = localStorage.getItem('admin_passkey') || '';
    const res = await fetch(`/api/glb/list?passkey=${passkey}`);
    const data = await res.json();
    if (data.success) {
      glbFiles.value = data.glbs;
      
      // Auto-select first or find robot if selection is empty/stale
      if (glbFiles.value.length > 0) {
        const foundRobot = glbFiles.value.find(f => f.name.includes('robot'));
        selectedFile.value = foundRobot || glbFiles.value[0];
      } else {
        selectedFile.value = null;
      }
    } else {
      scanError.value = data.error || 'Failed to scan files.';
    }
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    scanError.value = 'Failed to load GLB catalog: ' + errorMsg;
    console.error(err);
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await fetchGlbFiles();
});

const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const getModelSrc = (file: GlbFile) => {
  const passkey = localStorage.getItem('admin_passkey') || '';
  return `/api/glb/raw?path=${encodeURIComponent(file.relPath)}&passkey=${encodeURIComponent(passkey)}`;
};

const triggerDownload = (file: GlbFile) => {
  const url = getModelSrc(file);
  const a = document.createElement('a');
  a.href = url;
  a.download = file.name;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
};

// Filtering & Search logical flows
const filteredGlbs = computed(() => {
  return glbFiles.value.filter(file => {
    const matchesSearch = file.name.toLowerCase().includes(searchQuery.value.toLowerCase()) || 
                          file.relPath.toLowerCase().includes(searchQuery.value.toLowerCase());
    
    if (statusFilter.value === 'valid') {
      return matchesSearch && file.isValidGLB;
    } else if (statusFilter.value === 'corrupted') {
      return matchesSearch && !file.isValidGLB;
    }
    return matchesSearch;
  });
});

const selectFile = (file: GlbFile) => {
  selectedFile.value = file;
};

// Segment parsing for human display of byte positions
const getHeaderChunks = (hex: string) => {
  if (!hex || hex.length < 24) return null;
  return {
    magic: hex.slice(0, 8),      // Bytes 0-3 (8 characters)
    version: hex.slice(8, 16),   // Bytes 4-7 (8 characters)
    length: hex.slice(16, 24),   // Bytes 8-11 (8 characters)
  };
};
</script>

<template>
  <div class="px-6 py-8 h-full max-w-7xl mx-auto space-y-8 select-none">
    
    <!-- Top Display Header -->
    <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-[#111111] p-6 border border-white/5 rounded-2xl relative overflow-hidden">
      <div class="absolute -top-24 -left-24 w-48 h-48 bg-red-500/5 blur-[80px] rounded-full pointing-events-none" />
      <div class="relative z-10">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center text-[#FF3B30]">
            <BoxIcon class="w-5 h-5" />
          </div>
          <div>
            <h2 class="text-2xl font-serif italic font-black text-white leading-tight">
              3D GLB Asset Validator
            </h2>
            <p class="text-[10px] uppercase font-bold tracking-widest text-white/40">
              Integrity testing, header decoding & live WebGL model inspections
            </p>
          </div>
        </div>
      </div>

      <div class="flex items-center gap-3 relative z-10">
        <button
          @click="fetchGlbFiles"
          :disabled="loading"
          class="bg-white/5 hover:bg-white/10 text-white font-mono text-xs px-4 py-2.5 rounded-xl border border-white/10 transition-all flex items-center gap-2 uppercase tracking-wider"
        >
          <RefreshIcon class="w-3.5 h-3.5" :class="{'animate-spin': loading}" />
          Rescan Workspace
        </button>
      </div>
    </div>

    <!-- Error state displays -->
    <div v-if="scanError" class="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-sm font-mono flex items-center gap-3">
      <ErrorIcon class="w-5 h-5 flex-shrink-0" />
      <span>{{ scanError }}</span>
    </div>

    <!-- No files or loading grid placeholder -->
    <div v-if="loading && glbFiles.length === 0" class="flex flex-col items-center justify-center py-20 border border-[dashed] border-white/10 rounded-3xl bg-black/20">
      <RefreshIcon class="w-8 h-8 animate-spin text-[#FF3B30] mb-4" />
      <p class="font-serif italic text-white/50 text-lg">Analyzing file workspace structure...</p>
    </div>

    <div v-else-if="glbFiles.length === 0" class="flex flex-col items-center justify-center py-24 border border-dashed border-white/10 rounded-3xl bg-black/20 text-center px-4">
      <BoxIcon class="w-12 h-12 text-white/20 mb-4" />
      <p class="font-serif italic text-white/70 text-lg mb-2">No 3D models detected</p>
      <p class="text-white/30 text-xs max-w-sm">No files with .glb extension were found recursively in the system routes. Ensure you have models configured inside your project assets folder.</p>
    </div>

    <!-- Active interactive board -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
      
      <!-- List Column (Left, 5 cols) -->
      <div class="lg:col-span-5 space-y-4">
        <div class="bg-[#111111] border border-white/5 rounded-2xl p-4 space-y-4 shadow-xl">
          <div class="relative">
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="Search assets by name or path..." 
              class="w-full bg-[#181818] border border-white/10 rounded-xl pl-10 pr-4 py-2.5 outline-none focus:border-[#FF3B30]/40 transition-all font-mono text-xs text-white"
            />
            <SearchIcon class="w-4 h-4 text-white/30 absolute left-3.5 top-3.5" />
          </div>

          <!-- Quick Filters -->
          <div class="grid grid-cols-3 gap-2">
            <button 
              @click="statusFilter = 'all'"
              class="py-1.5 px-3 rounded-lg text-[10px] font-black uppercase tracking-wider transition-all border"
              :class="statusFilter === 'all' ? 'bg-[#FF3B30] border-[#FF3B30] text-white shadow-md shadow-red-900/10' : 'bg-[#181818] border-white/5 text-white/40 hover:text-white'"
            >
              All ({{ glbFiles.length }})
            </button>
            <button 
              @click="statusFilter = 'valid'"
              class="py-1.5 px-3 rounded-lg text-[10px] font-black uppercase tracking-wider transition-all border"
              :class="statusFilter === 'valid' ? 'bg-green-500/10 border-green-500/20 text-green-400' : 'bg-[#181818] border-white/5 text-white/40 hover:text-white'"
            >
              Valid ({{ glbFiles.filter(f => f.isValidGLB).length }})
            </button>
            <button 
              @click="statusFilter = 'corrupted'"
              class="py-1.5 px-3 rounded-lg text-[10px] font-black uppercase tracking-wider transition-all border"
              :class="statusFilter === 'corrupted' ? 'bg-red-500/10 border-red-500/20 text-red-400' : 'bg-[#181818] border-white/5 text-white/40 hover:text-white'"
            >
              Corrupted ({{ glbFiles.filter(f => !f.isValidGLB).length }})
            </button>
          </div>
        </div>

        <div class="space-y-3 max-h-[600px] overflow-y-auto pr-2 custom-scrollbar">
          <div 
            v-for="file in filteredGlbs" 
            :key="file.relPath"
            @click="selectFile(file)"
            class="p-4 rounded-xl border transition-all cursor-pointer relative overflow-hidden group select-none"
            :class="[
              selectedFile?.relPath === file.relPath 
                ? 'bg-white/5 border-[#FF3B30]/50 shadow-lg shadow-red-950/5' 
                : 'bg-[#111111] border-white/5 hover:border-white/10 hover:bg-[#161616]'
            ]"
          >
            <!-- Highlight glow for selected -->
            <div 
              v-if="selectedFile?.relPath === file.relPath"
              class="absolute -right-12 -top-12 w-24 h-24 bg-[#FF3B30]/5 blur-2xl rounded-full pointer-events-none" 
            />

            <div class="flex items-start justify-between gap-3 relative z-10">
              <div class="space-y-1.5 min-w-0 flex-1">
                <div class="flex items-center gap-2">
                  <BoxIcon class="w-4 h-4 text-white/40 flex-shrink-0" :class="{'text-[#FF3B30]': selectedFile?.relPath === file.relPath}" />
                  <span class="text-xs font-mono font-bold text-white truncate text-wrap break-all pr-2 block">
                    {{ file.name }}
                  </span>
                </div>
                <p class="text-[9px] font-mono text-white/30 truncate" :title="file.relPath">
                  {{ file.relPath }}
                </p>
                <div class="flex items-center gap-3 pt-1">
                  <span class="text-[10px] font-mono font-bold text-neutral-400">
                    {{ formatBytes(file.actualSize) }}
                  </span>
                  <span class="text-[10px] text-white/20">•</span>
                  <span class="text-[10px] font-mono text-neutral-500">
                    {{ new Date(file.modifiedAt).toLocaleDateString() }}
                  </span>
                </div>
              </div>

              <div class="flex-shrink-0 flex flex-col items-end gap-1.5">
                <span 
                  class="text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded border flex items-center gap-1 font-mono"
                  :class="file.isValidGLB 
                    ? 'bg-green-500/10 text-green-400 border-green-500/20' 
                    : 'bg-red-500/10 text-red-500 border-red-500/20'"
                >
                  <CheckIcon v-if="file.isValidGLB" class="w-2.5 h-2.5" />
                  <ErrorIcon v-else class="w-2.5 h-2.5" />
                  {{ file.isValidGLB ? 'INTEGRITY OK' : 'CORRUPTED' }}
                </span>
                
                <span 
                  v-if="file.name.includes('robot')"
                  class="text-[8px] font-mono uppercase bg-blue-500/10 border border-blue-500/20 text-blue-400 px-1.5 py-0.5 rounded"
                >
                  Default Robot
                </span>
              </div>
            </div>
          </div>

          <div 
            v-if="filteredGlbs.length === 0"
            class="text-center py-12 border border-dashed border-white/5 rounded-xl bg-[#111111]/50 text-white/40 text-xs"
          >
            No assets match search or status filters.
          </div>
        </div>
      </div>

      <!-- Detail Column (Right, 7 cols) -->
      <div v-if="selectedFile" class="lg:col-span-7 space-y-6">
        
        <!-- Header Info Card -->
        <div class="bg-[#111111] border border-white/5 rounded-2xl p-6 relative overflow-hidden shadow-xl">
          <div class="flex flex-col md:flex-row justify-between items-start gap-4 mb-6">
            <div class="space-y-1">
              <h3 class="text-xl font-serif italic text-white flex items-center gap-2">
                <FileIcon class="w-5 h-5 text-[#FF3B30]" />
                {{ selectedFile.name }}
              </h3>
              <p class="text-xs font-mono text-white/40 break-all bg-black/30 p-2 rounded-lg border border-white/5 mt-2">
                {{ selectedFile.relPath }}
              </p>
            </div>
            
            <button 
              @click="triggerDownload(selectedFile)"
              class="w-full md:w-auto bg-[#181818] hover:bg-[#202020] border border-white/10 text-white font-mono text-xs px-4 py-2 rounded-xl transition-all flex items-center justify-center gap-2"
              title="Download Binary"
            >
              <DownloadIcon class="w-3.5 h-3.5" />
              Download GLB
            </button>
          </div>

          <!-- Integrity Auditing Panel -->
          <div class="border rounded-xl p-4 bg-black/40"
               :class="selectedFile.isValidGLB ? 'border-green-500/20 bg-green-500/5' : 'border-red-500/20 bg-red-500/5'"
          >
            <div class="flex items-start gap-3">
              <ShieldCheckIcon v-if="selectedFile.isValidGLB" class="w-5 h-5 text-green-400 mt-0.5 flex-shrink-0" />
              <ErrorIcon v-else class="w-5 h-5 text-red-500 mt-0.5 flex-shrink-0" />
              <div class="space-y-1">
                <h4 class="text-xs font-black uppercase tracking-widest"
                    :class="selectedFile.isValidGLB ? 'text-green-400' : 'text-red-400'"
                >
                  {{ selectedFile.isValidGLB ? 'Specification Clean' : 'Specification Failure' }}
                </h4>
                <p class="text-xs text-white/60">
                  {{ selectedFile.isValidGLB 
                    ? 'This asset has passing magic signatures and fits standard asset loading constraints. Size matching checks are aligned.' 
                    : selectedFile.errorMessage 
                  }}
                </p>
              </div>
            </div>
          </div>

          <!-- Header Binary Analysis (Decoder) -->
          <div class="mt-8 space-y-4">
            <div class="flex items-center gap-2 text-xs font-bold text-white uppercase tracking-wider">
              <CpuIcon class="w-4 h-4 text-orange-400" />
              Header Hex Segment Analysis (First 12 Bytes)
            </div>

            <div v-if="getHeaderChunks(selectedFile.headerHex)" class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <!-- Magic signature block -->
              <div class="bg-black/40 border border-white/5 rounded-xl p-3.5 flex flex-col justify-between">
                <div>
                  <span class="text-[9px] font-mono text-white/30 uppercase block mb-1">Bytes 0-3 (Magic)</span>
                  <span class="font-mono font-bold text-sm tracking-widest text-[#FF3B30]">
                    {{ getHeaderChunks(selectedFile.headerHex)?.magic }}
                  </span>
                </div>
                <div class="mt-3 text-[10px]">
                  <p class="font-bold text-[#F5F5F5] font-mono">glTF ASCII</p>
                  <p class="text-white/40 font-serif italic mt-0.5" 
                     :class="selectedFile.magicMatches ? 'text-green-400/80' : 'text-red-400/80'"
                  >
                    {{ selectedFile.magicMatches ? 'Match verified' : 'Mismatch' }}
                  </p>
                </div>
              </div>

              <!-- Version Block -->
              <div class="bg-black/40 border border-white/5 rounded-xl p-3.5 flex flex-col justify-between">
                <div>
                  <span class="text-[9px] font-mono text-white/30 uppercase block mb-1">Bytes 4-7 (Version)</span>
                  <span class="font-mono font-bold text-sm tracking-widest text-cyan-400">
                    {{ getHeaderChunks(selectedFile.headerHex)?.version }}
                  </span>
                </div>
                <div class="mt-3 text-[10px]">
                  <p class="font-bold text-[#F5F5F5] font-mono">Parsed: {{ selectedFile.version }}</p>
                  <p class="text-white/40 font-serif italic mt-0.5"
                     :class="selectedFile.version === 2 ? 'text-green-400/80' : 'text-red-400/80'"
                  >
                    {{ selectedFile.version === 2 ? 'JSON glTF 2.0 specs' : 'Unknown version' }}
                  </p>
                </div>
              </div>

              <!-- Declared Length Block -->
              <div class="bg-black/40 border border-white/5 rounded-xl p-3.5 flex flex-col justify-between">
                <div>
                  <span class="text-[9px] font-mono text-white/30 uppercase block mb-1">Bytes 8-11 (Length)</span>
                  <span class="font-mono font-bold text-sm tracking-widest text-yellow-500">
                    {{ getHeaderChunks(selectedFile.headerHex)?.length }}
                  </span>
                </div>
                <div class="mt-3 text-[10px]">
                  <p class="font-bold text-[#F5F5F5] font-mono">
                    Parsed: {{ selectedFile.declaredLength ? formatBytes(selectedFile.declaredLength) : 'Unknown' }}
                  </p>
                  <p class="text-white/40 font-serif italic mt-0.5"
                     :class="selectedFile.declaredSizeMatchesActual ? 'text-green-400/80' : 'text-red-400/80'"
                  >
                    {{ selectedFile.declaredSizeMatchesActual ? 'Perfect file match' : 'Length mismatch!' }}
                  </p>
                </div>
              </div>
            </div>

            <div v-else class="text-xs p-4 bg-black/20 text-neutral-400 font-mono border border-dashed border-white/5 rounded-xl text-center">
              Header bytes unreadable/file is empty.
            </div>

            <!-- Helpful notice for wrong files -->
            <div v-if="!selectedFile.isValidGLB" class="bg-amber-500/5 border border-amber-500/20 text-amber-300 rounded-xl p-4 flex gap-3 items-start">
              <HelpIcon class="w-5 h-5 text-amber-400 mt-0.5 flex-shrink-0" />
              <div class="text-xs space-y-2">
                <p class="font-bold">Why is this GLB file reported as corrupted?</p>
                <p class="leading-relaxed opacity-80">
                  Most glb file corruption comes from Git LFS mismatches or network truncation during workspace transfers. For instance, is actual file size under 1 KB, or does it start with plain text lines? It might actually be a Git LFS pointer text file instead of the actual binary asset!
                </p>
                <div class="bg-black/40 p-2 rounded font-mono text-[10px] text-zinc-300 border border-white/5 whitespace-pre">Raw Hex Header: {{ selectedFile.headerHex || 'Empty File' }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 3D Previewer Display Canvas -->
        <div class="bg-[#111111] border border-white/5 rounded-2xl overflow-hidden shadow-xl">
          <div class="px-6 py-4 border-b border-white/5 flex flex-col md:flex-row justify-between items-start md:items-center gap-2">
            <div class="flex items-center gap-2">
              <ViewIcon class="w-4 h-4 text-[#FF3B30]" />
              <h4 class="text-xs font-black uppercase tracking-wider text-white">Interactive WebGL Viewport</h4>
            </div>

            <div class="flex items-center gap-3">
              <label class="flex items-center gap-1.5 text-[10px] font-mono tracking-wider uppercase text-neutral-400 cursor-pointer">
                <input type="checkbox" v-model="autoRotate" class="rounded border-white/10 bg-[#161616] text-[#FF3B30] focus:ring-0 w-3 h-3 cursor-pointer" />
                Spin
              </label>
              
              <label class="flex items-center gap-1.5 text-[10px] font-mono tracking-wider uppercase text-neutral-400 cursor-pointer">
                <input type="checkbox" v-model="cameraControls" class="rounded border-white/10 bg-[#161616] text-[#FF3B30] focus:ring-0 w-3 h-3 cursor-pointer" />
                Orbit
              </label>
            </div>
          </div>
          
          <div class="relative bg-[#060606] h-[350px] flex items-center justify-center">
            
            <!-- Valid renderer -->
            <template v-if="selectedFile.isValidGLB">
              <model-viewer
                ref="viewer"
                :src="getModelSrc(selectedFile)"
                :autoplay="true"
                :auto-rotate="autoRotate ? 'true' : undefined"
                :camera-controls="cameraControls ? 'true' : undefined"
                :shadow-intensity="shadowIntensity"
                class="w-full h-full"
                environment-image="neutral"
                exposure="1"
                alt="3D model render"
                @error="console.error('model-viewer failed to render', $event)"
              >
                <!-- Loading indicator -->
                <div v-bind="{ slot: 'poster' }" class="absolute inset-0 flex items-center justify-center bg-black/40">
                  <div class="flex flex-col items-center gap-2">
                    <RefreshIcon class="w-6 h-6 animate-spin text-[#FF3B30]" />
                    <span class="text-[10px] font-mono tracking-wider uppercase text-white/50">Spinning up WebGL pipeline...</span>
                  </div>
                </div>
              </model-viewer>
            </template>
            
            <!-- Corrupted asset placeholder -->
            <template v-else>
              <div class="p-8 text-center flex flex-col items-center gap-4">
                <div class="w-16 h-16 bg-red-500/10 rounded-full flex items-center justify-center text-[#FF3B30]">
                  <ErrorIcon class="w-8 h-8" />
                </div>
                <div>
                  <h5 class="text-sm font-bold text-white uppercase tracking-wider">Cannot Instantiate 3D Render</h5>
                  <p class="text-[11px] text-white/40 mt-1 max-w-sm">The selected GLB failed standard binary validation. Passing corrupted models into WebGL will crash the browser rendering context.</p>
                </div>
              </div>
            </template>
          </div>

          <div class="px-6 py-4 bg-black/40 border-t border-white/5 flex justify-between items-center text-[10px] font-mono text-neutral-500">
            <span class="flex items-center gap-1">
              <CompassIcon class="w-3.5 h-3.5" />
              Use left mouse click or touch to drag & rotate model. scroll to zoom.
            </span>
            <span>WebGL Context V2</span>
          </div>
        </div>

      </div>

    </div>
  </div>
</template>

<style scoped>
/* Scoped overrides to style model-viewer component nicely */
model-viewer {
  --poster-color: transparent;
  display: block;
}

/* Scrollbar tweaks */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 3B, 30, 0.4);
}
</style>
