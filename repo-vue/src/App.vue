<script setup lang="ts">
import { ref, onMounted } from 'vue'
import sitesData from './sites.json'

interface SiteData {
  id: string;
  name: string;
  domain: string;
  description: string;
  theme: {
    primary: string;
    accent: string;
  };
  content: {
    hero: {
      title: string;
      subtitle: string;
      image: string;
    };
    features: Array<{ title: string; text: string }>;
  };
}

const activeSite = ref<SiteData | null>(null)
const loading = ref(true)

onMounted(() => {
  const host = window.location.hostname
  const urlParams = new URLSearchParams(window.location.search)
  const siteOverride = urlParams.get('__site')

  let site = sitesData.find(s => s.domain === host || s.id === siteOverride)
  
  if (!site) {
    site = sitesData[0]
  }
  
  activeSite.value = site as SiteData
  loading.value = false
})
</script>

<template>
  <div
    v-if="loading"
    class="flex items-center justify-center h-screen font-sans text-gray-500"
  >
    Loading CMS...
  </div>
  
  <div
    v-else-if="activeSite"
    id="site-root"
    class="min-h-screen font-sans bg-white"
  >
    <!-- Navigation -->
    <nav
      id="site-nav"
      class="border-b border-gray-100 py-4 px-6 flex justify-between items-center"
    >
      <div
        class="text-xl font-bold"
        :style="{ color: activeSite.theme.primary }"
      >
        {{ activeSite.name }}
      </div>
      <div class="space-x-6 hidden md:flex text-gray-600">
        <a
          href="#"
          class="hover:text-gray-900 transition-colors"
        >Accueil</a>
        <a
          href="#"
          class="hover:text-gray-900 transition-colors"
        >Services</a>
        <a
          href="#"
          class="hover:text-gray-900 transition-colors"
        >À Propos</a>
        <a
          href="#"
          class="hover:text-gray-900 transition-colors"
        >Contact</a>
      </div>
      <button 
        id="cta-nav"
        class="px-4 py-2 rounded-lg text-white font-medium transition-all hover:brightness-110"
        :style="{ backgroundColor: activeSite.theme.primary }"
      >
        S'inscrire
      </button>
    </nav>

    <!-- Hero Section -->
    <header
      id="hero-section"
      class="relative py-20 px-6 overflow-hidden"
    >
      <div class="max-w-6xl mx-auto grid md:grid-cols-2 gap-12 items-center relative z-10">
        <div class="space-y-6 text-left">
          <h1 class="text-5xl font-extrabold tracking-tight text-gray-900 leading-tight">
            {{ activeSite.content.hero.title }}
          </h1>
          <p class="text-xl text-gray-600">
            {{ activeSite.content.hero.subtitle }}
          </p>
          <div class="flex gap-4">
            <button 
              id="hero-cta-main"
              class="px-8 py-3 rounded-xl text-white font-bold text-lg shadow-lg"
              :style="{ backgroundColor: activeSite.theme.primary }"
            >
              Découvrir
            </button>
            <button
              id="hero-cta-secondary"
              class="px-8 py-3 rounded-xl border-2 border-gray-200 text-gray-700 font-bold text-lg hover:bg-gray-50 bg-white"
            >
              En savoir plus
            </button>
          </div>
        </div>
        <div
          id="hero-image-wrapper"
          class="relative group"
        >
          <div
            class="absolute -inset-1 rounded-2xl blur opacity-25 group-hover:opacity-40 transition-opacity"
            :style="{ background: activeSite.theme.primary }"
          />
          <img 
            id="hero-image" 
            :src="activeSite.content.hero.image" 
            :alt="activeSite.name"
            class="relative rounded-2xl shadow-2xl w-full h-[400px] object-cover ring-1 ring-black/5"
          >
        </div>
      </div>
      
      <!-- Background blobs -->
      <div
        class="absolute top-0 right-0 -translate-y-1/2 translate-x-1/4 w-96 h-96 blur-3xl opacity-10 rounded-full"
        :style="{ backgroundColor: activeSite.theme.primary }"
      />
      <div
        class="absolute bottom-0 left-0 translate-y-1/2 -translate-x-1/4 w-96 h-96 blur-3xl opacity-10 rounded-full"
        :style="{ backgroundColor: activeSite.theme.accent }"
      />
    </header>

    <!-- Features -->
    <section
      id="features-section"
      class="py-20 px-6 bg-gray-50/50"
    >
      <div class="max-w-6xl mx-auto">
        <div class="text-center mb-16">
          <h2 class="text-3xl font-bold text-gray-900">
            Nos Services
          </h2>
          <div
            class="h-1 w-20 mx-auto mt-4 rounded-full"
            :style="{ backgroundColor: activeSite.theme.primary }"
          />
        </div>
        <div class="grid md:grid-cols-3 gap-8">
          <div
            v-for="(feature, i) in activeSite.content.features"
            :id="'feature-' + i"
            :key="i"
            class="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow"
          >
            <div 
              class="w-12 h-12 rounded-xl mb-6 flex items-center justify-center text-white font-bold text-xl"
              :style="{ backgroundColor: activeSite.theme.primary }"
            >
              {{ i + 1 }}
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-3">
              {{ feature.title }}
            </h3>
            <p class="text-gray-600 leading-relaxed">
              {{ feature.text }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer
      id="site-footer"
      class="bg-gray-900 text-gray-400 py-12 px-6"
    >
      <div class="max-w-6xl mx-auto flex flex-col md:flex-row justify-between items-center gap-8 text-center md:text-left">
        <div class="space-y-4">
          <div class="text-2xl font-bold text-white">
            {{ activeSite.name }}
          </div>
          <p class="max-w-xs">
            {{ activeSite.description }}
          </p>
        </div>
        <div class="flex gap-8">
          <a
            href="#"
            class="hover:text-white transition-colors"
          >Mentions Légales</a>
          <a
            href="#"
            class="hover:text-white transition-colors"
          >Confidentialité</a>
          <a
            href="#"
            class="hover:text-white transition-colors"
          >Contact</a>
        </div>
        <div class="text-sm">
          © 2026 {{ activeSite.name }}. Multi-stack CMS.
        </div>
      </div>
    </footer>
  </div>
</template>
