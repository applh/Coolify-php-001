<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
import { 
  Unlock, 
  AlertCircle, 
  Loader2,
  LayoutDashboard as DashboardIcon,
  Search as ExplorerIcon,
  Clapperboard as MediaQueueIcon,
  Users as TeamsIcon,
  BarChart3 as ChartIcon,
  RefreshCcw as SyncIcon,
  Boxes as GlbIcon,
  Library as TrainingIcon,
  ChevronLeft,
  ChevronRight
} from 'lucide-vue-next';

const isAuthenticated = ref(false);
const passkey = ref('');
const error = ref('');
const isLoading = ref(false);
const isChecking = ref(true);
const isDrawerExpanded = ref(true);

const route = useRoute();

const toggleDrawer = () => {
  isDrawerExpanded.value = !isDrawerExpanded.value;
};

const navItems = [
  { name: 'Dashboard', path: '/', icon: DashboardIcon },
  { name: 'Explorer', path: '/explorer', icon: ExplorerIcon },
  { name: 'AI Media', path: '/ai-media', icon: MediaQueueIcon },
  { name: 'Agent Teams', path: '/agent-teams', icon: TeamsIcon },
  { name: 'Benchmarks', path: '/benchmark', icon: ChartIcon },
  { name: 'Sync', path: '/sync', icon: SyncIcon },
  { name: '3D GLB', path: '/glb-validator', icon: GlbIcon },
  { name: 'Training', path: '/training', icon: TrainingIcon, highlight: true },
];

const isActive = (path: string) => {
  if (path === '/') return route?.path === '/';
  return route?.path?.startsWith(path);
};

const verifyPasskey = async (p: string) => {
  isLoading.value = true;
  error.value = '';
  try {
    const res = await fetch('/api/auth/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ passkey: p })
    });
    const data = await res.json();
    if (data.success) {
      isAuthenticated.value = true;
      localStorage.setItem('admin_passkey', p);
    } else {
      error.value = data.error || 'Invalid passkey';
    }
  } catch (err) {
    error.value = 'Failed to verify passkey';
    console.error(err);
  } finally {
    isLoading.value = false;
  }
};

const logout = () => {
  isAuthenticated.value = false;
  localStorage.removeItem('admin_passkey');
  passkey.value = '';
};

onMounted(async () => {
  window.addEventListener('admin-auth-failed', () => {
    isAuthenticated.value = false;
    localStorage.removeItem('admin_passkey');
  });

  window.addEventListener('admin-logout', logout);

  const stored = localStorage.getItem('admin_passkey');
  if (stored) {
    await verifyPasskey(stored);
  } else {
    // Check if passkey is required at all
    try {
      const res = await fetch('/api/auth/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ passkey: '' })
      });
      const data = await res.json();
      if (data.noPasskey) {
        isAuthenticated.value = true;
      }
    } catch (err) {
      console.error('Initial auth check failed', err);
    }
  }
  isChecking.value = false;
});

const handleLogin = () => {
  if (passkey.value) {
    verifyPasskey(passkey.value);
  }
};
</script>

<template>
  <div class="min-h-screen flex flex-col bg-[#0A0A0A] text-[#F5F5F5] selection:bg-[#FF3B30] selection:text-white">
    <template v-if="isChecking">
      <div class="fixed inset-0 flex items-center justify-center bg-[#0A0A0A] z-[9999]">
        <Loader2 class="w-8 h-8 animate-spin text-[#FF3B30]" />
      </div>
    </template>

    <template v-else-if="!isAuthenticated">
      <div class="fixed inset-0 flex items-center justify-center bg-[#0A0A0A] z-[9999] px-4">
        <div class="w-full max-w-md bg-[#161616] border border-white/10 rounded-3xl p-10 shadow-2xl relative overflow-hidden group">
          <!-- Background glow decoration -->
          <div class="absolute -top-24 -left-24 w-48 h-48 bg-[#FF3B30]/10 blur-[80px] rounded-full group-hover:bg-[#FF3B30]/20 transition-all duration-700" />
          <div class="absolute -bottom-24 -right-24 w-48 h-48 bg-[#FF3B30]/5 blur-[80px] rounded-full" />
          
          <div class="relative z-10 flex flex-col items-center mb-10">
            <div class="w-20 h-20 bg-[#1F1F1F] rounded-2xl flex items-center justify-center mb-6 border border-white/5 shadow-inner group-hover:scale-105 transition-transform duration-500">
              <span class="text-3xl">🍓</span>
            </div>
            <h1 class="text-3xl font-serif italic tracking-tighter font-black text-white leading-none mb-2">
              FRAISE
            </h1>
            <p class="text-white/30 text-[10px] uppercase tracking-[0.3em] font-bold">
              Secure Infrastructure Ops
            </p>
          </div>

          <form
            class="space-y-6 relative z-10"
            @submit.prevent="handleLogin"
          >
            <div>
              <input
                v-model="passkey"
                type="password"
                placeholder="Passkey"
                class="w-full bg-[#1F1F1F] border border-white/10 rounded-xl px-4 py-3 outline-none focus:border-[#FF3B30]/50 focus:ring-1 focus:ring-[#FF3B30]/50 transition-all text-center tracking-widest"
                required
                autofocus
              >
            </div>
            
            <p
              v-if="error"
              class="text-[#FF3B30] text-sm flex items-center justify-center gap-2"
            >
              <AlertCircle class="w-4 h-4" />
              {{ error }}
            </p>

            <button
              type="submit"
              :disabled="isLoading"
              class="w-full bg-[#FF3B30] hover:bg-[#E6352B] disabled:opacity-50 text-white font-semibold py-3 rounded-xl transition-all flex items-center justify-center gap-2"
            >
              <Loader2
                v-if="isLoading"
                class="w-5 h-5 animate-spin"
              />
              <template v-else>
                <Unlock class="w-5 h-5" />
                Access Dashboard
              </template>
            </button>
          </form>

          <p class="mt-8 text-center text-[10px] uppercase tracking-widest text-white/20">
            Secure Infrastructure / Auth v1.3
          </p>
        </div>
      </div>
    </template>

    <template v-else>
      <AppHeader />
      
      <div class="flex flex-1 relative min-h-0 w-full">
        <!-- Navigation Drawer (Left Side) - Responsive Sidebar -->
        <aside 
          class="bg-[#0D0D0D] border-r border-white/5 transition-all duration-300 flex flex-col z-30 shrink-0 select-none relative"
          :class="isDrawerExpanded ? 'w-64' : 'w-16'"
        >
          <!-- Accent glow -->
          <div class="absolute -bottom-12 -left-12 w-24 h-24 bg-red-500/5 blur-2xl rounded-full pointer-events-none" />

          <!-- Drawer Navigation Items -->
          <div class="flex-grow py-6 space-y-1 px-3 overflow-y-auto overflow-x-hidden custom-scrollbar">
            <router-link
              v-for="item in navItems"
              :key="item.path"
              :to="item.path"
              class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs uppercase tracking-wider font-bold transition-all relative group border"
              :class="[
                isActive(item.path) 
                  ? 'bg-red-500/10 text-[#FF3B30] border-red-500/20 shadow-md shadow-red-950/10' 
                  : 'text-white/40 hover:text-white hover:bg-white/5 border-transparent hover:border-white/5'
              ]"
              :title="!isDrawerExpanded ? item.name : undefined"
            >
              <component
                :is="item.icon"
                class="w-4 h-4 shrink-0 transition-transform duration-300 group-hover:scale-110"
                :class="isActive(item.path) ? 'text-[#FF3B30]' : 'text-neutral-400 group-hover:text-white'"
              />
              <span 
                v-if="isDrawerExpanded"
                class="truncate transition-opacity duration-300 whitespace-nowrap"
              >
                {{ item.name }}
              </span>
              
              <!-- Hover custom tooltip if collapsed -->
              <span 
                v-if="!isDrawerExpanded"
                class="absolute left-14 scale-0 group-hover:scale-100 bg-[#161616] border border-white/10 text-white text-[10px] uppercase tracking-wider font-bold py-1.5 px-3 rounded-lg opacity-0 group-hover:opacity-100 transition-all duration-200 shadow-xl pointer-events-none z-50 whitespace-nowrap"
              >
                {{ item.name }}
              </span>
            </router-link>
          </div>

          <!-- Drawer Collapse/Expand Toggle Footer -->
          <div class="p-3 border-t border-white/5 flex justify-center">
            <button
              @click="toggleDrawer"
              class="w-10 h-10 rounded-xl bg-[#161616] hover:bg-[#202020] border border-white/5 flex items-center justify-center text-white/50 hover:text-white transition-all shadow-inner cursor-pointer animate-none"
              :title="isDrawerExpanded ? 'Collapse Menu' : 'Expand Menu'"
            >
              <component :is="isDrawerExpanded ? ChevronLeft : ChevronRight" class="w-4 h-4" />
            </button>
          </div>
        </aside>

        <!-- Main Content Area -->
        <div class="flex-grow flex flex-col relative min-w-0">
          <main class="flex-grow min-w-0">
            <router-view v-slot="{ Component }">
              <transition
                name="page"
                mode="out-in"
              >
                <component :is="Component" />
              </transition>
            </router-view>
          </main>

          <AppFooter />
        </div>

        <!-- Vertical Floating Menu Bar (Right Side) -->
        <div class="fixed right-6 bottom-24 z-50 flex flex-col items-center select-none pointer-events-none">
          <div class="bg-[#111111]/80 backdrop-blur-md border border-white/10 p-2 rounded-2xl flex flex-col gap-2 shadow-2xl shadow-black pointer-events-auto">
            <router-link
              v-for="item in navItems"
              :key="'floating-' + item.path"
              :to="item.path"
              class="w-10 h-10 rounded-xl flex items-center justify-center transition-all relative group border"
              :class="[
                isActive(item.path)
                  ? 'bg-[#FF3B30] border-[#FF3B30] text-white shadow-lg shadow-red-900/30'
                  : 'bg-[#181818] border-white/5 text-white/40 hover:text-white hover:border-white/10 hover:bg-[#242424]'
              ]"
            >
              <component :is="item.icon" class="w-4 h-4" />
              
              <!-- Left tooltip on hover -->
              <span class="absolute right-14 scale-0 group-hover:scale-100 bg-[#161616] border border-white/10 text-white text-[9px] uppercase tracking-widest font-black py-1.5 px-3 rounded-lg opacity-0 group-hover:opacity-100 transition-all duration-200 shadow-xl pointer-events-none whitespace-nowrap z-50">
                {{ item.name }}
              </span>
            </router-link>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style>
/* Any global App.vue styles if needed */
</style>

