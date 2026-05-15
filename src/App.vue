<script setup lang="ts">
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
</script>

<template>
  <div class="min-h-screen flex flex-col bg-[#0A0A0A] text-[#F5F5F5] selection:bg-[#FF3B30] selection:text-white">
    <AppHeader />
    
    <main class="flex-grow">
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
</template>

<style>
/* Any global App.vue styles if needed */
</style>

