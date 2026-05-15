<script setup lang="ts">
import { useRoute } from 'vue-router';
import { 
  BarChart3 as ChartIcon, 
  RefreshCcw as SyncIcon, 
  Library as TrainingIcon,
  Search as ExplorerIcon,
  LayoutDashboard as DashboardIcon,
  Clapperboard as MediaQueueIcon,
  LogOut as LogoutIcon
} from 'lucide-vue-next';

const route = useRoute();

const handleLogout = () => {
  if (confirm('Are you sure you want to logout?')) {
    window.dispatchEvent(new CustomEvent('admin-logout'));
  }
};

const navItems = [
  { name: 'Dashboard', path: '/', icon: DashboardIcon },
  { name: 'Explorer', path: '/explorer', icon: ExplorerIcon },
  { name: 'AI Media', path: '/ai-media', icon: MediaQueueIcon },
  { name: 'Benchmarks', path: '/benchmark', icon: ChartIcon },
  { name: 'Sync', path: '/sync', icon: SyncIcon },
  { name: 'Training', path: '/training', icon: TrainingIcon, highlight: true },
];

const isActive = (path: string) => {
  if (path === '/') return route.path === '/';
  return route.path.startsWith(path);
};
</script>

<template>
  <header class="border-b border-white/5 px-6 py-4 flex justify-between items-center bg-[#0D0D0D] sticky top-0 z-40 backdrop-blur-md bg-opacity-80">
    <div class="flex items-center gap-4">
      <router-link
        to="/"
        class="flex items-center gap-3 group"
      >
        <div class="w-10 h-10 bg-[#FF3B30] rounded-xl flex items-center justify-center shadow-lg shadow-red-900/20 group-hover:rotate-12 transition-transform duration-300">
          <span class="text-xl">🍓</span>
        </div>
        <div>
          <h1 class="font-serif italic text-2xl tracking-tighter font-black text-white leading-none">
            FRAISE
          </h1>
          <span class="text-[8px] uppercase tracking-[0.3em] opacity-30 font-bold">Intelligence Ops</span>
        </div>
      </router-link>
    </div>
    
    <nav class="hidden md:flex items-center gap-1">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="px-4 py-2 rounded-lg text-[11px] uppercase tracking-wider font-bold transition-all flex items-center gap-2"
        :class="[
          isActive(item.path) 
            ? 'bg-white/5 text-[#FF3B30]' 
            : 'text-white/40 hover:text-white hover:bg-white/5',
          item.highlight && !isActive(item.path) ? 'text-[#F27D26]' : ''
        ]"
      >
        <component
          :is="item.icon"
          :size="14"
        />
        {{ item.name }}
      </router-link>
    </nav>

    <div class="flex items-center gap-3">
      <div class="flex flex-col items-end mr-2">
        <span class="text-[10px] font-mono opacity-40 uppercase tracking-tighter">System Status</span>
        <span class="flex items-center gap-1.5 text-[10px] font-bold text-green-500 uppercase tracking-wider">
          <span class="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse" />
          Operational
        </span>
      </div>
      <div class="w-8 h-8 rounded-full border border-white/10 bg-white/5 flex items-center justify-center overflow-hidden">
        <div class="w-full h-full bg-gradient-to-br from-red-500/20 to-orange-500/20" />
      </div>
      <button 
        @click="handleLogout"
        class="w-8 h-8 rounded-lg border border-white/10 bg-white/5 flex items-center justify-center text-white/40 hover:text-[#FF3B30] hover:bg-white/10 transition-all group"
        title="Logout"
      >
        <LogoutIcon :size="16" />
      </button>
    </div>
  </header>
</template>
