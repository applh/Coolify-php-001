<script setup lang="ts">
import { ref, onMounted } from 'vue';
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
import { Unlock, AlertCircle, Loader2 } from 'lucide-vue-next';

const isAuthenticated = ref(false);
const passkey = ref('');
const error = ref('');
const isLoading = ref(false);
const isChecking = ref(true);

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
            <p class="text-white/30 text-[10px] uppercase tracking-[0.3em] font-bold">Secure Infrastructure Ops</p>
          </div>

          <form @submit.prevent="handleLogin" class="space-y-6 relative z-10">
            <div>
              <input
                v-model="passkey"
                type="password"
                placeholder="Passkey"
                class="w-full bg-[#1F1F1F] border border-white/10 rounded-xl px-4 py-3 outline-none focus:border-[#FF3B30]/50 focus:ring-1 focus:ring-[#FF3B30]/50 transition-all text-center tracking-widest"
                required
                autofocus
              />
            </div>
            
            <p v-if="error" class="text-[#FF3B30] text-sm flex items-center justify-center gap-2">
              <AlertCircle class="w-4 h-4" />
              {{ error }}
            </p>

            <button
              type="submit"
              :disabled="isLoading"
              class="w-full bg-[#FF3B30] hover:bg-[#E6352B] disabled:opacity-50 text-white font-semibold py-3 rounded-xl transition-all flex items-center justify-center gap-2"
            >
              <Loader2 v-if="isLoading" class="w-5 h-5 animate-spin" />
              <template v-else>
                <Unlock class="w-5 h-5" />
                Access Dashboard
              </template>
            </button>
          </form>

          <p class="mt-8 text-center text-[10px] uppercase tracking-widest text-white/20">
            Secure Infrastructure / Auth v1.2
          </p>
        </div>
      </div>
    </template>

    <template v-else>
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
    </template>
  </div>
</template>

<style>
/* Any global App.vue styles if needed */
</style>

