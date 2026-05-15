<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline';
  size?: 'xs' | 'sm' | 'md' | 'lg';
  loading?: boolean;
  disabled?: boolean;
}>();

const variantClasses = computed(() => {
  switch (props.variant) {
    case 'primary': return 'bg-[#FF3B30] text-white hover:bg-[#E0342B] shadow-lg shadow-red-900/10';
    case 'secondary': return 'bg-[#F27D26] text-black hover:bg-[#D96D1D]';
    case 'danger': return 'bg-red-600/10 text-red-500 border border-red-500/20 hover:bg-red-600 hover:text-white';
    case 'ghost': return 'text-white/40 hover:text-white hover:bg-white/5';
    case 'outline': return 'border border-white/10 text-white/70 hover:border-white/30 hover:text-white';
    default: return 'bg-white/5 text-white hover:bg-white/10';
  }
});

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'xs': return 'px-2.5 py-1.5 text-[9px] uppercase tracking-widest font-bold';
    case 'sm': return 'px-3.5 py-2 text-[10px] uppercase tracking-widest font-bold';
    case 'lg': return 'px-6 py-4 text-sm font-bold';
    default: return 'px-5 py-2.5 text-xs uppercase tracking-wider font-bold';
  }
});
</script>

<template>
  <button
    v-bind="$attrs"
    :disabled="disabled || loading"
    class="inline-flex items-center justify-center gap-2 rounded-xl transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100"
    :class="[variantClasses, sizeClasses]"
  >
    <span
      v-if="loading"
      class="w-3 h-3 border-2 border-current border-t-transparent rounded-full animate-spin"
    />
    <slot
      v-else
      name="icon"
    />
    <slot />
  </button>
</template>
