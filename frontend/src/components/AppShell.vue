<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from '@/components/shell/AppHeader.vue'
</script>

<template>
  <a class="skip-link" href="#main">Skip to content</a>
  <div class="shell">
    <AppHeader />
    <main id="main" class="shell__main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.shell {
  min-height: 100vh;
}

.shell__main {
  width: min(100%, $measure);
  margin: 0 auto;
  padding: $space-6 $space-4 $space-7;
}
</style>
