<template>
  <div class="min-h-screen flex flex-col bg-[#121212] text-[#E4E3E0]">
    <header class="border-b border-[#2A2A2A] p-4 flex justify-between items-center bg-[#181818]">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 bg-[#F27D26] rounded flex items-center justify-center font-bold text-black text-xs">
          CMS
        </div>
        <h1 class="font-serif italic text-lg tracking-tight">
          PHP CMS Manager
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
        <a
          href="https://ais-dev-b4sj5apyjy3qy5glsde24e-241950013583.europe-west1.run.app"
          target="_blank"
          class="hover:text-[#F27D26] transition-colors"
        >Live Preview</a>
      </nav>
    </header>
    <main class="flex-grow p-6">
      <router-view />
    </main>
    <footer class="border-t border-[#2A2A2A] p-4 text-[10px] text-center opacity-40 uppercase tracking-[0.2em]">
      Node.js CMS &copy; 2026 &mdash; Powered by SQLite
    </footer>
  </div>
</template>

<style>
.font-serif {
  font-family: 'Georgia', serif;
}
</style>
