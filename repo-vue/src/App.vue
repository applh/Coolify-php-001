<script setup lang="ts">
import { ref } from 'vue'

const count = ref(0)
</script>

<template>
  <div class="container">
    <h1>Vite + Vue</h1>
    <div class="card">
      <button type="button" @click="count++">count is {{ count }}</button>
      <p>
        Edit <code>components/HelloWorld.vue</code> to test HMR
      </p>
    </div>
  </div>
</template>

<style scoped>
.container {
  padding: 2rem;
  text-align: center;
  font-family: sans-serif;
}
.card {
  margin: 2rem;
}
button {
  padding: 0.5rem 1rem;
  font-size: 1.2rem;
  cursor: pointer;
}
p {
  color: #666;
}
</style>
