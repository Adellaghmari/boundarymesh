<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  document.title = 'BoundaryMesh'
})
</script>

<template>
  <div class="missing">
    <main class="card stack">
      <h1>Page not available</h1>
      <p>This address is not part of the public portfolio.</p>
      <div class="cluster">
        <a class="action" href="/">Portfolio</a>
        <a class="action action--quiet" href="/explore">Explore BoundaryMesh</a>
        <a class="action action--quiet" href="/operator">Operator sign in</a>
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/tokens' as *;

.missing {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: $space-5;
  background: $color-canvas;
}

.action {
  display: inline-flex;
  width: fit-content;
  border-radius: $radius-md;
  padding: 0.7rem 1rem;
  background: $color-accent;
  color: white;
  text-decoration: none;
  font-weight: 700;
}

.action--quiet {
  background: transparent;
  color: $color-accent;
  border: 1px solid $color-accent;
}
</style>
