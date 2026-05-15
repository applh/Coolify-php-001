<script setup lang="ts">
defineProps<{
  title?: string;
  subtitle?: string;
  hoverable?: boolean;
}>();
</script>

<template>
  <div 
    class="relative overflow-hidden bg-[#0F0F0F] border border-white/5 p-6 rounded-2xl transition-all duration-300"
    :class="[
      hoverable ? 'hover:border-red-500/30 hover:bg-[#141414] cursor-pointer group' : ''
    ]"
  >
    <div v-if="title || subtitle" class="mb-6">
      <h3 v-if="title" class="font-serif italic text-xl text-white group-hover:text-red-500 transition-colors">
        {{ title }}
      </h3>
      <p v-if="subtitle" class="text-[10px] font-mono uppercase tracking-widest text-white/30 mt-1">
        {{ subtitle }}
      </p>
    </div>
    
    <slot />

    <!-- Technical background decoration -->
    <div class="absolute -right-4 -bottom-4 opacity-[0.02] pointer-events-none group-hover:opacity-[0.05] transition-opacity">
      <slot name="icon" />
    </div>
  </div>
</template>
