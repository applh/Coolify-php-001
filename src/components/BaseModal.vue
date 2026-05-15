<script setup lang="ts">
import { X } from 'lucide-vue-next';

defineProps<{
  modelValue: boolean;
  title: string;
}>();

const emit = defineEmits(['update:modelValue']);

const close = () => {
  emit('update:modelValue', false);
};
</script>

<template>
  <transition
    enter-active-class="transition duration-200 ease-out"
    enter-from-class="opacity-0 scale-95"
    enter-to-class="opacity-100 scale-100"
    leave-active-class="transition duration-150 ease-in"
    leave-from-class="opacity-100 scale-100"
    leave-to-class="opacity-0 scale-95"
  >
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      <!-- Backdrop -->
      <div 
        class="absolute inset-0 bg-black/80 backdrop-blur-sm"
        @click="close"
      ></div>
      
      <!-- Modal Content -->
      <div class="relative bg-[#0F0F0F] border border-white/10 w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-300">
        <div class="px-6 py-4 border-b border-white/5 flex justify-between items-center bg-white/[0.02]">
          <h3 class="text-xl font-serif italic text-white">{{ title }}</h3>
          <button 
            @click="close"
            class="p-2 text-white/40 hover:text-white hover:bg-white/5 rounded-lg transition-all"
          >
            <X :size="20" />
          </button>
        </div>
        
        <div class="p-8">
          <slot />
        </div>

        <div v-if="$slots.footer" class="px-6 py-4 border-t border-white/5 bg-white/[0.01] flex justify-end gap-3">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </transition>
</template>
