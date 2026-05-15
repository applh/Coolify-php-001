<script setup lang="ts">
import { type Component } from 'vue';
import { X } from 'lucide-vue-next';

defineProps<{
  modelValue: string | number;
  placeholder?: string;
  type?: string;
  label?: string;
  icon?: Component;
  error?: string;
  clearable?: boolean;
}>();

const emit = defineEmits(['update:modelValue', 'clear']);

const onInput = (e: Event) => {
  emit('update:modelValue', (e.target as HTMLInputElement).value);
};

const clear = () => {
  emit('update:modelValue', '');
  emit('clear');
};
</script>

<template>
  <div class="flex flex-col gap-2 w-full">
    <label
      v-if="label"
      class="text-[10px] uppercase tracking-[0.2em] font-bold text-white/40 ml-1"
    >
      {{ label }}
    </label>
    <div class="relative group">
      <div 
        v-if="icon" 
        class="absolute left-4 top-1/2 -translate-y-1/2 text-white/20 group-focus-within:text-[#FF3B30] transition-colors"
      >
        <component
          :is="icon"
          :size="16"
        />
      </div>
      
      <input
        :type="type || 'text'"
        :value="modelValue"
        :placeholder="placeholder"
        class="w-full bg-white/5 border border-white/10 rounded-xl py-3 text-sm text-white placeholder:text-white/20 focus:outline-none focus:border-[#FF3B30] focus:bg-white/[0.08] transition-all"
        :class="[
          icon ? 'pl-11' : 'pl-5',
          clearable ? 'pr-11' : 'pr-5',
          error ? 'border-red-500/50 focus:border-red-500' : ''
        ]"
        @input="onInput"
      >

      <button
        v-if="clearable && modelValue"
        class="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 text-white/20 hover:text-white hover:bg-white/10 rounded-lg transition-all"
        @click="clear"
      >
        <X :size="14" />
      </button>
    </div>
    
    <p
      v-if="error"
      class="text-[10px] text-red-500 font-mono ml-1"
    >
      {{ error }}
    </p>
  </div>
</template>
