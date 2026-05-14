<template>
  <div class="min-h-screen flex flex-col bg-[#121212] text-[#E4E3E0]">
    <header class="border-b border-[#2A2A2A] p-4 flex justify-between items-center bg-[#181818]">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 bg-red-600 rounded flex items-center justify-center font-bold text-white text-xs">
          🍓
        </div>
        <h1 class="font-serif italic text-2xl tracking-tighter font-extrabold text-red-500">
          🍓 FRAISE
        </h1>
      </div>
      <nav class="flex gap-6 text-xs uppercase tracking-widest font-semibold opacity-70">
        <router-link
          to="/"
          class="hover:text-[#F27D26] transition-colors"
        >
          Dashboard
        </router-link>
        <router-link
          to="/explorer"
          class="hover:text-[#F27D26] transition-colors"
        >
          Explorer
        </router-link>
        <router-link
          to="/ai-media"
          class="hover:text-[#F27D26] transition-colors"
        >
          AI Media Queue
        </router-link>
        <router-link
          to="/benchmark"
          class="hover:text-[#F27D26] transition-colors"
        >
          Benchmarks
        </router-link>
        <router-link
          to="/sync"
          class="hover:text-[#F27D26] transition-colors"
        >
          Sync
        </router-link>
        <router-link
          to="/training"
          class="hover:text-[#F27D26] transition-colors text-[#F27D26] font-bold"
        >
          Training
        </router-link>
      </nav>
    </header>
    <main class="flex-grow p-6">
      <router-view />
    </main>
    <footer class="border-t border-[#2A2A2A] p-4 text-[10px] text-center opacity-40 uppercase tracking-[0.2em] flex justify-center items-center gap-4">
      <span>🍓 FRAISE &copy; 2026</span>
      <span class="px-1.5 py-0.5 border border-white/20 rounded-sm">v1.2.0</span>
    </footer>
  </div>
</template>

<style>
.font-serif {
  font-family: 'Georgia', serif;
}
</style>
